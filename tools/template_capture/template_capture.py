"""V26 템플릿 캡처 도구.

LDPlayer(또는 다른 안드로이드 에뮬레이터)에 ADB로 붙어 화면을 받아옵니다.

두 가지 저장 방식:
  - PNG로 저장: 드래그한 영역을 잘라서 app/src/main/assets/templates/<bucket>/<name>.png 에 저장
                → 매크로가 OpenCV 템플릿 매칭으로 그 화면/버튼을 찾는 데 사용
  - 좌표로 저장: 드래그한 영역의 중심점을 정규화 좌표로 app/src/main/assets/coords.json 에 저장
                → 매크로가 그 위치를 바로 탭. 팀 컬러로 배경이 바뀌는 메뉴 버튼처럼 매칭이
                  불안정한 곳에서 유용. 좌표가 정의되면 같은 이름의 PNG 보다 우선됨.

사용법:
    python template_capture.py
"""

from __future__ import annotations

import io
import json
import sys
import threading
import tkinter as tk
from pathlib import Path
from tkinter import messagebox, ttk

try:
    from PIL import Image, ImageTk
except ImportError:
    print("Pillow가 설치되어 있지 않습니다. start.bat를 사용하거나 'pip install Pillow'를 먼저 실행하세요.", file=sys.stderr)
    sys.exit(1)

from adb_client import AdbClient, AdbError, Device
from manifest import BUCKET_LABELS, BUCKET_ORDER, TEMPLATES

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS_DIR = REPO_ROOT / "app" / "src" / "main" / "assets" / "templates"
COORDS_FILE = REPO_ROOT / "app" / "src" / "main" / "assets" / "coords.json"

PREVIEW_MAX_W = 720
PREVIEW_MAX_H = 1080


def load_coords() -> dict[str, list[float]]:
    if not COORDS_FILE.exists():
        return {}
    try:
        text = COORDS_FILE.read_text(encoding="utf-8").strip()
        if not text:
            return {}
        data = json.loads(text)
        return {k: list(v) for k, v in data.items() if isinstance(v, (list, tuple)) and len(v) >= 2}
    except Exception:
        return {}


def save_coords(coords: dict[str, list[float]]) -> None:
    COORDS_FILE.parent.mkdir(parents=True, exist_ok=True)
    COORDS_FILE.write_text(
        json.dumps(coords, ensure_ascii=False, indent=2, sort_keys=True),
        encoding="utf-8",
    )


class CaptureApp(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("V26 템플릿 캡처 도구")
        self.geometry("1240x900")

        self.adb: AdbClient | None = None
        self.devices: list[Device] = []
        self.current_serial: str | None = None
        self.full_image: Image.Image | None = None
        self.preview_image: ImageTk.PhotoImage | None = None
        self.preview_scale: float = 1.0  # full -> preview

        # 선택 영역 (preview 좌표)
        self.sel_start: tuple[int, int] | None = None
        self.sel_end: tuple[int, int] | None = None
        self.sel_rect_id: int | None = None

        self._build_ui()
        self._refresh_status()

        # 비동기로 ADB 탐지
        self._set_status("ADB 탐지 중…")
        threading.Thread(target=self._init_adb_async, daemon=True).start()

    # ── UI ─────────────────────────────────────────────────────────
    def _build_ui(self) -> None:
        top = ttk.Frame(self, padding=8)
        top.pack(side=tk.TOP, fill=tk.X)

        ttk.Label(top, text="에뮬레이터:").pack(side=tk.LEFT)
        self.device_var = tk.StringVar()
        self.device_combo = ttk.Combobox(
            top, textvariable=self.device_var, width=28, state="readonly"
        )
        self.device_combo.pack(side=tk.LEFT, padx=(4, 8))
        self.device_combo.bind("<<ComboboxSelected>>", self._on_device_select)

        ttk.Button(top, text="다시 탐지", command=self._rescan).pack(side=tk.LEFT)
        ttk.Button(top, text="화면 새로고침 (F5)", command=self._capture_screen).pack(
            side=tk.LEFT, padx=(8, 0)
        )
        ttk.Label(top, text=" • 드래그하여 영역 선택", foreground="#666").pack(
            side=tk.LEFT, padx=(12, 0)
        )

        self.status_var = tk.StringVar(value="")
        ttk.Label(top, textvariable=self.status_var, foreground="#888").pack(
            side=tk.RIGHT
        )

        # 가운데: 좌측 = 체크리스트 / 우측 = 캡처 영역
        body = ttk.Frame(self, padding=(8, 4))
        body.pack(fill=tk.BOTH, expand=True)

        # ── 좌측 체크리스트 ──
        left = ttk.LabelFrame(body, text="필요한 템플릿", padding=6)
        left.pack(side=tk.LEFT, fill=tk.Y, padx=(0, 8))

        self.tree = ttk.Treeview(
            left, columns=("status",), show="tree headings", height=36
        )
        self.tree.heading("#0", text="버킷 / 이름")
        self.tree.heading("status", text="상태")
        self.tree.column("#0", width=300)
        self.tree.column("status", width=70, anchor="center")
        self.tree.pack(side=tk.LEFT, fill=tk.Y)
        self.tree.bind("<<TreeviewSelect>>", self._on_template_select)

        sb = ttk.Scrollbar(left, orient="vertical", command=self.tree.yview)
        sb.pack(side=tk.LEFT, fill=tk.Y)
        self.tree.configure(yscrollcommand=sb.set)

        # ── 우측 캡처 영역 ──
        right = ttk.Frame(body)
        right.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        canvas_frame = ttk.LabelFrame(right, text="화면 (드래그로 영역 선택)", padding=6)
        canvas_frame.pack(side=tk.TOP, fill=tk.BOTH, expand=True)

        self.canvas = tk.Canvas(canvas_frame, bg="#202020", highlightthickness=0)
        self.canvas.pack(fill=tk.BOTH, expand=True)
        self.canvas.bind("<ButtonPress-1>", self._on_press)
        self.canvas.bind("<B1-Motion>", self._on_drag)
        self.canvas.bind("<ButtonRelease-1>", self._on_release)

        # 하단: 저장 패널
        save_panel = ttk.LabelFrame(right, text="자르기 / 저장", padding=8)
        save_panel.pack(side=tk.TOP, fill=tk.X, pady=(8, 0))

        row1 = ttk.Frame(save_panel)
        row1.pack(fill=tk.X)
        ttk.Label(row1, text="버킷:").pack(side=tk.LEFT)
        self.bucket_var = tk.StringVar()
        self.bucket_combo = ttk.Combobox(
            row1, textvariable=self.bucket_var, width=24, state="readonly",
            values=[f"{b} — {BUCKET_LABELS.get(b, b)}" for b in BUCKET_ORDER],
        )
        self.bucket_combo.pack(side=tk.LEFT, padx=(4, 12))
        self.bucket_combo.bind("<<ComboboxSelected>>", self._on_bucket_change)

        ttk.Label(row1, text="이름:").pack(side=tk.LEFT)
        self.name_var = tk.StringVar()
        self.name_combo = ttk.Combobox(
            row1, textvariable=self.name_var, width=22, state="normal"
        )
        self.name_combo.pack(side=tk.LEFT, padx=(4, 12))
        self.name_combo.bind("<<ComboboxSelected>>", self._on_name_select)

        self.desc_var = tk.StringVar(value="")
        ttk.Label(row1, textvariable=self.desc_var, foreground="#666").pack(
            side=tk.LEFT, padx=(8, 0)
        )

        row2 = ttk.Frame(save_panel)
        row2.pack(fill=tk.X, pady=(8, 0))
        self.crop_info_var = tk.StringVar(value="선택된 영역 없음")
        ttk.Label(row2, textvariable=self.crop_info_var).pack(side=tk.LEFT)

        ttk.Button(row2, text="PNG로 저장", command=self._save_crop).pack(side=tk.RIGHT)
        ttk.Button(row2, text="좌표로 저장", command=self._save_coord).pack(
            side=tk.RIGHT, padx=(0, 6)
        )
        ttk.Button(row2, text="좌표 삭제", command=self._delete_coord).pack(
            side=tk.RIGHT, padx=(0, 6)
        )
        ttk.Button(row2, text="선택 초기화", command=self._clear_selection).pack(
            side=tk.RIGHT, padx=(0, 8)
        )

        row3 = ttk.Frame(save_panel)
        row3.pack(fill=tk.X, pady=(4, 0))
        ttk.Label(
            row3,
            text="PNG = 템플릿 매칭용 / 좌표 = 항상 같은 위치 탭 (배경색 바뀌는 메뉴 버튼에 추천). 좌표가 정의되면 PNG보다 우선 사용됩니다.",
            foreground="#666",
            wraplength=820,
        ).pack(side=tk.LEFT)

        # 단축키
        self.bind("<F5>", lambda _e: self._capture_screen())
        self.bind("<Control-s>", lambda _e: self._save_crop())
        self.bind("<Control-d>", lambda _e: self._save_coord())

        self._populate_tree()

    def _populate_tree(self) -> None:
        for child in self.tree.get_children():
            self.tree.delete(child)
        coords = load_coords()
        for bucket in BUCKET_ORDER:
            label = f"{BUCKET_LABELS.get(bucket, bucket)}  ({bucket})"
            parent = self.tree.insert("", "end", text=label, open=True, values=(""))
            for (b, name, required, desc) in TEMPLATES:
                if b != bucket:
                    continue
                key = f"{b}/{name}"
                has_png = (ASSETS_DIR / b / f"{name}.png").exists()
                has_coord = key in coords
                req_mark = "★" if required else "·"
                if has_coord:
                    status = "📍"  # 좌표 정의됨 (PNG보다 우선)
                elif has_png:
                    status = "✓"
                elif required:
                    status = "필수"
                else:
                    status = ""
                self.tree.insert(
                    parent, "end",
                    text=f" {req_mark} {name}  — {desc}",
                    values=(status,),
                    tags=(key,),
                )

    def _on_template_select(self, _evt) -> None:
        sel = self.tree.selection()
        if not sel:
            return
        tags = self.tree.item(sel[0], "tags")
        if not tags:
            return
        try:
            bucket, name = tags[0].split("/", 1)
        except ValueError:
            return
        self.bucket_var.set(f"{bucket} — {BUCKET_LABELS.get(bucket, bucket)}")
        self._refresh_name_options(bucket)
        self.name_var.set(name)
        self._on_name_select()

    def _on_bucket_change(self, _evt=None) -> None:
        bucket = self._current_bucket()
        if bucket:
            self._refresh_name_options(bucket)

    def _refresh_name_options(self, bucket: str) -> None:
        names = [name for (b, name, _r, _d) in TEMPLATES if b == bucket]
        self.name_combo["values"] = names

    def _on_name_select(self, _evt=None) -> None:
        bucket = self._current_bucket()
        name = self.name_var.get().strip()
        for (b, n, _r, desc) in TEMPLATES:
            if b == bucket and n == name:
                self.desc_var.set(desc)
                return
        self.desc_var.set("")

    def _current_bucket(self) -> str | None:
        v = self.bucket_var.get()
        if not v:
            return None
        return v.split(" — ", 1)[0].strip()

    # ── ADB ────────────────────────────────────────────────────────
    def _init_adb_async(self) -> None:
        try:
            self.adb = AdbClient()
        except AdbError as e:
            self.after(0, lambda: messagebox.showerror("ADB 오류", str(e)))
            self.after(0, lambda: self._set_status("ADB 미설치"))
            return
        self._rescan_async()

    def _rescan(self) -> None:
        threading.Thread(target=self._rescan_async, daemon=True).start()

    def _rescan_async(self) -> None:
        if not self.adb:
            return
        self.after(0, lambda: self._set_status("LDPlayer 탐지 중…"))
        try:
            self.devices = self.adb.auto_discover()
        except AdbError as e:
            self.after(0, lambda: messagebox.showerror("ADB 오류", str(e)))
            return
        labels = []
        for d in self.devices:
            try:
                w, h = self.adb.device_resolution(d.serial)
                labels.append(f"{d.serial}  ({w}x{h})")
            except AdbError:
                labels.append(d.serial)
        self.after(0, lambda: self._apply_devices(labels))

    def _apply_devices(self, labels: list[str]) -> None:
        self.device_combo["values"] = labels
        if labels:
            self.device_combo.current(0)
            self._on_device_select()
        else:
            self._set_status(
                "에뮬레이터를 찾지 못했습니다. LDPlayer가 켜져 있는지 확인하세요."
            )

    def _on_device_select(self, _evt=None) -> None:
        idx = self.device_combo.current()
        if idx < 0 or idx >= len(self.devices):
            return
        self.current_serial = self.devices[idx].serial
        self._set_status(f"연결: {self.current_serial}")
        self._capture_screen()

    def _capture_screen(self) -> None:
        if not self.adb or not self.current_serial:
            return
        threading.Thread(target=self._capture_screen_async, daemon=True).start()

    def _capture_screen_async(self) -> None:
        try:
            png = self.adb.screencap(self.current_serial)
        except AdbError as e:
            self.after(0, lambda: messagebox.showerror("캡처 실패", str(e)))
            return
        try:
            img = Image.open(io.BytesIO(png)).convert("RGB")
        except Exception as e:
            self.after(0, lambda: messagebox.showerror("이미지 오류", str(e)))
            return
        self.after(0, lambda: self._show_image(img))

    def _show_image(self, img: Image.Image) -> None:
        self.full_image = img
        # 프리뷰 다운스케일
        scale = min(PREVIEW_MAX_W / img.width, PREVIEW_MAX_H / img.height, 1.0)
        new_w = max(1, int(img.width * scale))
        new_h = max(1, int(img.height * scale))
        self.preview_scale = scale
        preview = img.resize((new_w, new_h), Image.LANCZOS)
        self.preview_image = ImageTk.PhotoImage(preview)
        self.canvas.delete("all")
        self.canvas.config(width=new_w, height=new_h)
        self.canvas.create_image(0, 0, image=self.preview_image, anchor="nw", tags=("img",))
        self.sel_rect_id = None
        self._clear_selection()
        self._set_status(f"화면 {img.width}x{img.height}, 미리보기 ×{scale:.2f}")

    # ── 선택 영역 ──────────────────────────────────────────────────
    def _on_press(self, evt) -> None:
        self.sel_start = (evt.x, evt.y)
        self.sel_end = (evt.x, evt.y)
        if self.sel_rect_id is not None:
            self.canvas.delete(self.sel_rect_id)
        self.sel_rect_id = self.canvas.create_rectangle(
            evt.x, evt.y, evt.x, evt.y,
            outline="#F97316", width=2, dash=(4, 2),
        )

    def _on_drag(self, evt) -> None:
        if self.sel_start is None or self.sel_rect_id is None:
            return
        self.sel_end = (evt.x, evt.y)
        x0, y0 = self.sel_start
        self.canvas.coords(self.sel_rect_id, x0, y0, evt.x, evt.y)

    def _on_release(self, _evt) -> None:
        if not self.sel_start or not self.sel_end:
            return
        x0, y0 = self.sel_start
        x1, y1 = self.sel_end
        if abs(x1 - x0) < 4 or abs(y1 - y0) < 4:
            self._clear_selection()
            return
        rx, ry, rw, rh = self._rect_in_full()
        self.crop_info_var.set(f"선택: ({rx},{ry}) {rw}x{rh}")

    def _rect_in_full(self) -> tuple[int, int, int, int]:
        x0, y0 = self.sel_start
        x1, y1 = self.sel_end
        x0, x1 = sorted((x0, x1))
        y0, y1 = sorted((y0, y1))
        s = self.preview_scale or 1.0
        rx = int(x0 / s); ry = int(y0 / s)
        rw = int((x1 - x0) / s); rh = int((y1 - y0) / s)
        return rx, ry, rw, rh

    def _clear_selection(self) -> None:
        if self.sel_rect_id is not None:
            self.canvas.delete(self.sel_rect_id)
        self.sel_rect_id = None
        self.sel_start = None
        self.sel_end = None
        self.crop_info_var.set("선택된 영역 없음")

    # ── 저장 ───────────────────────────────────────────────────────
    def _save_crop(self) -> None:
        if not self.full_image:
            messagebox.showwarning("경고", "화면이 없습니다. 먼저 새로고침하세요.")
            return
        if not (self.sel_start and self.sel_end):
            messagebox.showwarning("경고", "저장할 영역을 드래그로 선택하세요.")
            return
        bucket = self._current_bucket()
        name = self.name_var.get().strip()
        if not bucket or not name:
            messagebox.showwarning("경고", "버킷과 이름을 선택하세요.")
            return
        if not name.replace("_", "").isalnum():
            messagebox.showwarning("경고", "이름은 영문/숫자/_ 만 사용 가능합니다.")
            return

        rx, ry, rw, rh = self._rect_in_full()
        if rw < 4 or rh < 4:
            messagebox.showwarning("경고", "선택 영역이 너무 작습니다.")
            return
        # 클램프
        rx = max(0, min(rx, self.full_image.width - 1))
        ry = max(0, min(ry, self.full_image.height - 1))
        rw = min(rw, self.full_image.width - rx)
        rh = min(rh, self.full_image.height - ry)
        crop = self.full_image.crop((rx, ry, rx + rw, ry + rh))

        out_dir = ASSETS_DIR / bucket
        out_dir.mkdir(parents=True, exist_ok=True)
        out_path = out_dir / f"{name}.png"
        if out_path.exists():
            if not messagebox.askyesno("덮어쓰기", f"{out_path.relative_to(REPO_ROOT)} 이(가) 이미 있습니다. 덮어쓸까요?"):
                return
        crop.save(out_path, format="PNG")
        self._set_status(f"PNG 저장됨: {out_path.relative_to(REPO_ROOT)} ({rw}x{rh})")
        self._populate_tree()
        self._clear_selection()

    def _save_coord(self) -> None:
        """선택 영역의 중심을 정규화 좌표로 coords.json 에 저장."""
        if not self.full_image:
            messagebox.showwarning("경고", "화면이 없습니다. 먼저 새로고침하세요.")
            return
        if not (self.sel_start and self.sel_end):
            messagebox.showwarning(
                "경고",
                "좌표로 저장할 위치를 드래그로 표시해주세요. "
                "드래그한 사각형의 중심점이 좌표로 저장됩니다.",
            )
            return
        bucket = self._current_bucket()
        name = self.name_var.get().strip()
        if not bucket or not name:
            messagebox.showwarning("경고", "버킷과 이름을 선택하세요.")
            return
        if not name.replace("_", "").isalnum():
            messagebox.showwarning("경고", "이름은 영문/숫자/_ 만 사용 가능합니다.")
            return

        rx, ry, rw, rh = self._rect_in_full()
        cx = rx + rw // 2
        cy = ry + rh // 2
        w = self.full_image.width
        h = self.full_image.height
        x_frac = round(cx / w, 4)
        y_frac = round(cy / h, 4)

        coords = load_coords()
        key = f"{bucket}/{name}"
        coords[key] = [x_frac, y_frac]
        save_coords(coords)
        self._set_status(
            f"좌표 저장됨: {key} → ({x_frac}, {y_frac})  "
            f"[중심 픽셀 ({cx},{cy}) / 화면 {w}x{h}]"
        )
        self._populate_tree()
        self._clear_selection()

    def _delete_coord(self) -> None:
        bucket = self._current_bucket()
        name = self.name_var.get().strip()
        if not bucket or not name:
            messagebox.showwarning("경고", "버킷과 이름을 선택하세요.")
            return
        coords = load_coords()
        key = f"{bucket}/{name}"
        if key not in coords:
            self._set_status(f"좌표 없음: {key}")
            return
        if not messagebox.askyesno("좌표 삭제", f"{key} 의 좌표 정의를 삭제할까요?"):
            return
        coords.pop(key, None)
        save_coords(coords)
        self._set_status(f"좌표 삭제됨: {key}")
        self._populate_tree()

    # ── 상태바 ─────────────────────────────────────────────────────
    def _set_status(self, msg: str) -> None:
        self.status_var.set(msg)

    def _refresh_status(self) -> None:
        # 1초마다 트리만 자동 갱신
        self._populate_tree()
        self.after(3000, self._refresh_status)


def main() -> None:
    app = CaptureApp()
    app.mainloop()


if __name__ == "__main__":
    main()
