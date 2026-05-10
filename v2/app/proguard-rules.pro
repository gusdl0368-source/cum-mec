# 기본 옵션 — 디버그 빌드는 미사용. 추후 release 빌드 활성화 시 채울 것.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations

-keep class org.snakeyaml.engine.** { *; }
-keep class com.google.mlkit.** { *; }
