# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools proguard-defaults.txt.

# ONNX Runtime — classes are loaded via JNI from native code,
# so R8 cannot see the references and would strip them.
-keep class ai.onnxruntime.** { *; }

# ErrorProne annotations are compile-time only; Tink references them
# but they are not needed at runtime.
-dontwarn com.google.errorprone.annotations.**
