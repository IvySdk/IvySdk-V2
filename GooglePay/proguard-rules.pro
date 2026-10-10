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
-keep class * implements com.ivy.base.IUnProguard

#unity 2021及之后版本混淆需要添加
-keep class com.google.androidgamesdk.**{*;}

#AIHelp
-keep class net.aihelp.** {*;}
#yandex
-keep class com.yandex.div.** { *; }
-keep class com.yandex.div2.** { *; }
-keep class com.yandex.** { *; }
#G+
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}
#brotli
-keep class com.google.brotli.** { *; }

-keep class com.google.firebase.example.fireeats.model.** { *; }
-keep class com.google.android.gms.ads.identifier.** { *; }