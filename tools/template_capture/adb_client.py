"""LDPlayer / 일반 안드로이드 에뮬레이터를 자동 탐지하는 ADB 래퍼.

LDPlayer 인스턴스는 보통 127.0.0.1:5555 부터 5557, 5559, ... 짝수+1 포트로
열린다. (인스턴스 N → 5555 + 2*N)
"""

from __future__ import annotations

import os
import shutil
import subprocess
from dataclasses import dataclass
from pathlib import Path

LDPLAYER_PATHS = [
    r"C:\LDPlayer\LDPlayer9\adb.exe",
    r"C:\LDPlayer\LDPlayer\adb.exe",
    r"C:\LDPlayer\LDPlayer4.0\adb.exe",
    r"C:\Program Files\LDPlayer\LDPlayer9\adb.exe",
    r"C:\Program Files (x86)\LDPlayer\LDPlayer9\adb.exe",
    r"D:\LDPlayer\LDPlayer9\adb.exe",
    r"D:\Program Files\LDPlayer\LDPlayer9\adb.exe",
]

PORT_SCAN = [5555 + 2 * i for i in range(16)]  # 5555..5585


@dataclass
class Device:
    serial: str
    state: str  # "device", "offline", ...

    @property
    def is_ready(self) -> bool:
        return self.state == "device"


class AdbError(RuntimeError):
    pass


class AdbClient:
    def __init__(self, adb_path: str | None = None):
        self.adb_path = adb_path or self._discover_adb()
        if not self.adb_path:
            raise AdbError(
                "adb.exe를 찾을 수 없습니다. LDPlayer 설치 폴더를 PATH에 추가하거나 "
                "Android Platform Tools를 설치하세요."
            )

    @staticmethod
    def _discover_adb() -> str | None:
        # 1) 시스템 PATH
        in_path = shutil.which("adb") or shutil.which("adb.exe")
        if in_path:
            return in_path
        # 2) LDPlayer 기본 설치 경로들
        for p in LDPLAYER_PATHS:
            if Path(p).exists():
                return p
        # 3) 환경변수에서 LDPlayer 위치 추측
        for env in ("LDPlayer", "LDPLAYER"):
            v = os.environ.get(env)
            if v and Path(v, "adb.exe").exists():
                return str(Path(v, "adb.exe"))
        return None

    def _run(self, *args: str, check: bool = True, binary: bool = False, timeout: float = 10.0):
        proc = subprocess.run(
            [self.adb_path, *args],
            capture_output=True,
            timeout=timeout,
            creationflags=0x08000000 if os.name == "nt" else 0,  # CREATE_NO_WINDOW
        )
        if check and proc.returncode != 0:
            raise AdbError(
                f"adb {' '.join(args)} 실패 (rc={proc.returncode}): "
                f"{proc.stderr.decode('utf-8', 'replace').strip()}"
            )
        return proc.stdout if binary else proc.stdout.decode("utf-8", "replace")

    def start_server(self) -> None:
        try:
            self._run("start-server", timeout=20)
        except subprocess.TimeoutExpired:
            pass

    def kill_server(self) -> None:
        self._run("kill-server", check=False)

    def connect_port(self, port: int) -> bool:
        try:
            out = self._run("connect", f"127.0.0.1:{port}", check=False, timeout=4)
        except subprocess.TimeoutExpired:
            return False
        return "connected" in out.lower() or "already" in out.lower()

    def list_devices(self) -> list[Device]:
        out = self._run("devices")
        devices: list[Device] = []
        for line in out.splitlines()[1:]:
            line = line.strip()
            if not line or "\t" not in line:
                continue
            serial, state = line.split("\t", 1)
            devices.append(Device(serial=serial.strip(), state=state.strip()))
        return devices

    def auto_discover(self) -> list[Device]:
        """LDPlayer 등에서 자주 쓰는 포트를 모두 connect 시도한 뒤 ready 디바이스만 반환."""
        self.start_server()
        for p in PORT_SCAN:
            self.connect_port(p)
        return [d for d in self.list_devices() if d.is_ready]

    def screencap(self, serial: str) -> bytes:
        """PNG bytes for the device's current screen."""
        # exec-out is faster and avoids \r\n mangling on Windows
        proc = subprocess.run(
            [self.adb_path, "-s", serial, "exec-out", "screencap", "-p"],
            capture_output=True,
            timeout=15,
            creationflags=0x08000000 if os.name == "nt" else 0,
        )
        if proc.returncode != 0:
            raise AdbError(
                "screencap 실패: " + proc.stderr.decode("utf-8", "replace").strip()
            )
        data = proc.stdout
        if not data.startswith(b"\x89PNG"):
            # 일부 구형 안드로이드는 \r\n 변환을 함 - 보정
            data = data.replace(b"\r\n", b"\n")
        if not data.startswith(b"\x89PNG"):
            raise AdbError("screencap 출력이 PNG 형식이 아닙니다.")
        return data

    def device_resolution(self, serial: str) -> tuple[int, int]:
        out = self._run("-s", serial, "shell", "wm", "size")
        # "Physical size: 1280x720" or "Override size: ..."
        size = None
        for line in out.splitlines():
            line = line.strip()
            if "size:" in line.lower() and "x" in line:
                size = line.split(":", 1)[1].strip()
        if not size or "x" not in size:
            return (0, 0)
        w, h = size.split("x", 1)
        return (int(w.strip()), int(h.strip()))
