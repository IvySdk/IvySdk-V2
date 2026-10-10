-keep class * implements com.ivy.base.IUnProguard
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}