# ProGuard / R8 rules for the release build.
#
# The previous rules kept every class and member under com.example.**, which
# completely defeated R8: nothing could be stripped, so isMinifyEnabled and
# isShrinkResources had almost no effect on the APK. These rules keep only what
# is genuinely reached reflectively and let the rest shrink.

# --- Model / storage: plain Kotlin data classes read reflectively by org.json ---
# ChannelCacheStorage writes these fields by name, so the field names must survive.
-keepclassmembers class com.example.kurdishtv.model.Channel {
    <init>(...);
    <fields>;
}
-keepclassmembers class com.example.kurdishtv.model.AppSettings {
    <init>(...);
    <fields>;
}

# --- ViewModels are instantiated reflectively by the ViewModelProvider.Factory ---
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModel$Factory {
    <init>(...);
}

# --- Activities and Services referenced from the manifest ---
-keep class com.example.MainActivity { *; }

# --- Media3 / ExoPlayer ---
# Playback uses extension renderers and data sources resolved by name.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- OkHttp & Coil ---
# OkHttp references optional platform APIs that are absent on Android.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-keep class okhttp3.internal.platform.** { *; }

# Coil resolves image loaders and fetcher factories reflectively.
-dontwarn coil.**
-dontwarn coil.util.**

# --- Kotlin coroutines debug ---
-dontwarn kotlinx.coroutines.**

# Strip verbose/debug logging from the release build.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
