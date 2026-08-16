# Optimization and shrinkage rules for Android TV Launcher

# Keep SQLDelight database & queries
-keep class dev.mudrock.tiviyomitvlauncher.data.sqldelight.** { *; }
-keep class app.cash.sqldelight.** { *; }

# Keep Koin DI
-keep class org.koin.** { *; }
-keep class dev.mudrock.tiviyomitvlauncher.data.repository.** { *; }
-keep class dev.mudrock.tiviyomitvlauncher.data.resolver.** { *; }
-keep class dev.mudrock.tiviyomitvlauncher.ui.tab.**ViewModel { *; }

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Coil image loader and custom fetchers
-keep class coil.** { *; }
-keep class dev.mudrock.tiviyomitvlauncher.image.** { *; }

# Keep Compose Runtime and Foundation
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep TV Material and TV Provider
-keep class androidx.tv.material3.** { *; }
-keep class androidx.tvprovider.** { *; }

# Keep Timber logging
-dontwarn timber.log.**
