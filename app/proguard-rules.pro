# Add project specific ProGuard rules here.

# Keep App Models, ViewModels, Repositories, Data & UI
-keep class com.example.** { *; }
-keepclassmembers class com.example.** { *; }
-keep class com.example.kurdishtv.** { *; }
-keepclassmembers class com.example.kurdishtv.** { *; }

# Keep Media3 / ExoPlayer classes for video playback
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep OkHttp & Coil
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class coil.** { *; }
-dontwarn coil.**
