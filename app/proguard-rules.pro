# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.github.nemynew.expressiveweather.**$$serializer { *; }
-keepclassmembers class io.github.nemynew.expressiveweather.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.nemynew.expressiveweather.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room (R8 fullMode strips no-arg constructors unless explicitly kept)
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase$Callback { *; }
-dontwarn androidx.room.paging.**

# WorkManager & App Startup (instantiated via reflection)
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.InputMerger {
    public <init>();
}
-keep class * implements androidx.startup.Initializer {
    <init>();
}

# ViewModel reflection instantiation
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Google Maps / Play Services
-keep class com.google.android.gms.maps.** { *; }

# Keep enum names used with valueOf() / .name for SharedPreferences persistence and Intent extras
-keep enum io.github.nemynew.expressiveweather.ui.components.BentoCardType { *; }
-keep enum io.github.nemynew.expressiveweather.data.TemperatureUnit { *; }
-keep enum io.github.nemynew.expressiveweather.data.WindSpeedUnit { *; }
-keep enum io.github.nemynew.expressiveweather.data.AlertSource { *; }
-keep enum io.github.nemynew.expressiveweather.data.RainStatus { *; }

# Keep AndroidX NotificationCompat promoted notification reflection targets
-keepclassmembers class androidx.core.app.NotificationCompat$Builder {
    public androidx.core.app.NotificationCompat$Builder setShortCriticalText(java.lang.CharSequence);
    public androidx.core.app.NotificationCompat$Builder setShortCriticalText(java.lang.String);
    public androidx.core.app.NotificationCompat$Builder setRequestPromotedOngoing(boolean);
}

# Strip verbose/debug/info logs in release builds to avoid leaking runtime diagnostics or location metadata
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
