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

#----------------------------------
# 基础设置
#----------------------------------

# 保留类名和成员（避免反射/序列化出错）
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

-keep class * implements com.ivy.base.IUnProguard

# 避免混淆泛型
-keepattributes InnerClasses,Exceptions

#----------------------------------
# Android SDK 相关
#----------------------------------

# 保留四大组件及其子类（Activity, Service, BroadcastReceiver, ContentProvider）
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# 保留自定义 Application
-keep class * extends android.app.Application

# 保留 R 文件（避免资源 id 丢失）
-keep class **.R$* { *; }

#----------------------------------
# Gson / fastjson / Jackson 等 JSON 序列化
#----------------------------------

# Gson 需要保留实体类字段
-keep class com.google.gson.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class org.json.** { *; }

# 保留带有 @SerializedName 注解的字段
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

#----------------------------------
# 常见库
#----------------------------------

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# Glide
-dontwarn com.bumptech.glide.**
-keep public class com.bumptech.glide.** { *; }
-keep class * extends com.bumptech.glide.module.AppGlideModule

# RxJava
-dontwarn io.reactivex.**
-keep class io.reactivex.** { *; }
-keep interface io.reactivex.** { *; }

#----------------------------------
# Kotlin 协程 & 反射
#----------------------------------
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

#----------------------------------
# 防止反射失败（常用保留策略）
#----------------------------------
-keepclassmembers class * {
    public <init>(...);
}
-keepclassmembers class * {
    public void *(android.view.View);
}

#----------------------------------
# 其它
#----------------------------------
# 忽略警告
-dontwarn javax.annotation.**
-dontwarn sun.misc.**

# 保留 native 方法声明
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留 SystemProperties 类及其方法
-keep class android.os.SystemProperties {
    *;
}

# 保留引用 SystemProperties 的代码
-keepclassmembers class * {
    *** referenceToSystemProperties(...);
}

# 如果您知道具体哪个类在引用 SystemProperties，可以更精确地保留
# 根据错误信息，可能是 com.bytedance.bdtracker.e5 类
-keep class com.bytedance.bdtracker.e5 {
    *;
}

# 或者保留整个包（如果安全）
-keep class com.bytedance.bdtracker.** {
    *;
}

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


# 保留 SystemProperties 类及其所有成员
-keep class android.os.SystemProperties {
    *;
}

# 保留引用 SystemProperties 的特定类和方法
-keep class com.bytedance.bdtracker.e5 {
    *;
}

# 或者保留整个字节跳动跟踪器包
-keep class com.bytedance.bdtracker.** {
    *;
}

# 防止 R8 优化掉对 SystemProperties 的访问
-dontoptimize

-dontwarn android.os.SystemProperties
-dontwarn com.applovin.communicator.AppLovinCommunicator
-dontwarn com.applovin.communicator.AppLovinCommunicatorSubscriber
-dontwarn com.applovin.sdk.AppLovinSdk