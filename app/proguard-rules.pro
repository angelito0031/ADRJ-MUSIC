# ── Youtubedl-android (JNI + reflection) ──
-keep class com.yausername.** { *; }
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }

# ── Coil (usa reflection) ──
-keep class coil.** { *; }
-dontwarn coil.**

# ── OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── NanoHTTPD ──
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**

# ── Media3 ──
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Coroutines ──
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Compose runtime ──
-keep class androidx.compose.runtime.** { *; }

# ── Room ──
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ── Data classes ──
-keepclassmembers class com.adrj.music.** {
    <init>(...);
}

# ── Updates (CRÍTICO: sin allowshrinking) ──
-keep class com.adrj.music.updates.** { *; }
-keepclassmembers class com.adrj.music.updates.** { *; }

# ── Reducir logs ──
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
