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

-keep class io.cloudx.** { *; }
-dontwarn io.cloudx.**

-keep class io.cloudx.sdk.** { *; }
-keep class io.cloudx.adapter.** { *; }
-keep class io.cloudx.renderer.** { *; }
-keep class io.cloudx.programmatic.** { *; }
-keep class io.cloudx.vast.** { *; }
-keep class io.cloudx.internal.** { *; }

-keep class * implements io.cloudx.sdk.adapter.CloudXAdapterInitializer { *; }
-keep class * implements io.cloudx.sdk.adapter.CloudXBidRequestExtraProvider { *; }
-keep class * implements io.cloudx.sdk.adapter.CloudXInterstitialAdapterFactory { *; }
-keep class * implements io.cloudx.sdk.adapter.CloudXRewardedAdapterFactory { *; }
-keep class * implements io.cloudx.sdk.adapter.CloudXAdViewAdapterFactory { *; }
-keep class * implements io.cloudx.sdk.adapter.CloudXAdapterMetadataProvider { *; }

-keepattributes RuntimeVisibleAnnotations
-keep class io.cloudx.sdk.internal.AdNetwork { *; }
-keep class io.cloudx.sdk.internal.AdNetwork$* { *; }


-keep class * extends io.cloudx.adapter.** { *; }
-keep class * implements io.cloudx.** { *; }
-keep class * extends java.lang.Enum { *; }


-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.**

-keep class androidx.** { *; }
-dontwarn androidx.**

-dontwarn okhttp3.**
-dontwarn retrofit2.**