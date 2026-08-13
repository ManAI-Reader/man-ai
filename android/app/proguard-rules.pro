# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools proguard-defaults.txt.

# ONNX Runtime — classes are loaded via JNI from native code,
# so R8 cannot see the references and would strip them.
-keep class ai.onnxruntime.** { *; }

# ErrorProne annotations are compile-time only; Tink references them
# but they are not needed at runtime.
-dontwarn com.google.errorprone.annotations.**

# Kuromoji resolves its dictionary .bin resources at runtime relative to the
# Tokenizer class's package (Class.getResourceAsStream with a relative name).
# Renaming the package makes every lookup fail with "Classpath resource not
# found", which silently disables furigana and word selection.
-keep class com.atilika.kuromoji.** { *; }
