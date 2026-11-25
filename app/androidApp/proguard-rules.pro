# R8 / ProGuard rules for HealthTrack

# Preserve line numbers and source file names for crash reporting
-keepattributes SourceFile,LineNumberTable,EnclosingMethod,InnerClasses
-renamesourcefileattribute SourceFile

# Preserve annotations for serialization and framework reflection
-keepattributes *Annotation*,Signature,Exceptions

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlinx Serialization
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    *** INSTANCE;
}
-keepclassmembers class **$serializer {
    *;
}
-keep class com.yusufteker.healthtrack.core.model.** { *; }
-keep class com.yusufteker.healthtrack.core.network.** { *; }

# Ktor Client & OkHttp
-dontwarn io.ktor.**
-dontwarn okio.**
-dontwarn okhttp3.**

# Koin Dependency Injection
-keep class org.koin.** { *; }
-keep interface org.koin.** { *; }
-dontwarn org.koin.**

# Napier Logging
-keep class io.github.aakira.napier.** { *; }

# Multiplatform Settings
-dontwarn com.russhwolf.settings.**

# Compose & Compose Multiplatform
-dontwarn androidx.compose.**
-dontwarn org.jetbrains.compose.**

# ML Kit Text Recognition & Component Discovery
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
}
-keepclassmembers class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
}
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_bundled_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_common.** { *; }
-dontwarn com.google.mlkit.**

# Health Connect
-dontwarn androidx.health.connect.**

# Huawei HMS Core & AppGallery Connect
-ignorewarnings
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class com.huawei.hianalytics.** { *; }
-keep class com.huawei.updatesdk.** { *; }
-keep class com.huawei.hms.** { *; }
-keep class com.huawei.agconnect.** { *; }
-keep class com.hianalytics.android.** { *; }
-dontwarn com.huawei.hms.**
-dontwarn com.huawei.agconnect.**
