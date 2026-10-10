-keep class * implements com.ivy.base.IUnProguard

# TikTok checks com.tiktok class names in stack traces before returning its event logger.
-keep class com.tiktok.** { *; }
