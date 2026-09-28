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
#
# This used to be `-keep class androidx.media3.** { *; }`, which is the single
# largest APK-size mistake in this project: media3 is the biggest dependency the
# app has, and a blanket keep pinned every class and every member of it —
# including the decoders, extractors, renderers and data sources for formats
# this app will never play (DASH, MP3, Ogg, MIDI, image sequences, every
# codec that is not H.264/AAC). R8 cannot remove what a `-keep` protects, so
# the whole of media3 went into the APK whatever else the build stripped.
#
# Media3 ships its own consumer ProGuard rules, which keep the parts it
# genuinely resolves reflectively (the DefaultRenderersFactory and
# DefaultDataSource.Factory component tables) and let R8 shrink the rest. So
# the correct rule here is none at all. The app constructs its players through
# the public `ExoPlayer.Builder` / `DefaultMediaSourceFactory` / `OkHttpDataSource`
# API, which R8 sees statically, and every reflective entry point media3 needs
# is covered by the rules inside the AAR.
-dontwarn androidx.media3.**

# --- OkHttp & Coil ---
# OkHttp references optional platform APIs that are absent on Android.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
# The platform integration shims are small and reached through a service
# loader; keeping only them is enough for the client to work, and leaves the
# rest of okhttp3 shrinkable.
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
