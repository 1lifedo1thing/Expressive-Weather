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

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Google Maps / Play Services
-keep class com.google.android.gms.maps.** { *; }

# Keep Android 16 notification reflection targets if used via fallback
-keepclassmembers class android.app.Notification$Builder {
    public android.app.Notification$Builder setShortCriticalText(java.lang.CharSequence);
    public android.app.Notification$Builder setShortCriticalText(java.lang.String);
    public android.app.Notification$Builder setRequestPromotedOngoing(boolean);
}
-keep class android.app.Notification$ProgressStyle { *; }
-keep class android.app.Notification$ProgressStyle$Point { *; }
-keep class android.app.Notification$ProgressStyle$Segment { *; }

# Strip verbose/debug/info logs in release builds to avoid leaking runtime diagnostics or location metadata
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
