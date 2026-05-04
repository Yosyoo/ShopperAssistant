# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ML Kit uses Firebase component discovery and a few internal factories to
# create the offline barcode scanner. Keep these pieces stable under R8 so the
# scanner component is still registered in release builds.
-keep class com.google.firebase.components.** { *; }
-keep class com.google.mlkit.common.internal.** { *; }
-keep class com.google.mlkit.common.sdkinternal.** { *; }
-keep class com.google.mlkit.vision.common.internal.** { *; }
-keep class com.google.mlkit.vision.barcode.internal.** { *; }
-keep class com.google.mlkit.vision.barcode.bundled.internal.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
