# ── Youtubedl-android (usa JNI + reflection) ──
-keep class com.yausername.** { *; }
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }

# ── Coil (usa reflection en ImageLoader) ──
-keep class coil.** { *; }
-dontwarn coil.**

# ── OkHttp (usa reflection + Platform) ──
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── NanoHTTPD ──
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**

# ── Media3 (ExoPlayer) ──
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Kotlin Coroutines ──
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Compose (no romper el runtime) ──
-keep class androidx.compose.runtime.** { *; }

# ── Room ──
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ── Mantener nombres de clases de datos (para JSON / reflexión) ──
-keepclassmembers class com.adrj.music.** {
    <init>(...);
}

# ── No advertir por Jakarta / javax (usados por libs internas) ──
-dontwarn javax.annotation.**
-dontwarn jakarta.annotation.**

# ── Reducir tamaño eliminando logs de debug ──
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
