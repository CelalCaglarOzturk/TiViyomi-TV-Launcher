package dev.mudrock.tiviyomitvlauncher

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.DebugLogger
import dev.mudrock.tiviyomitvlauncher.crash.CrashHandler
import dev.mudrock.tiviyomitvlauncher.data.DatabaseContainer
import dev.mudrock.tiviyomitvlauncher.data.repository.AppRepository
import dev.mudrock.tiviyomitvlauncher.data.repository.BackupRepository
import dev.mudrock.tiviyomitvlauncher.data.repository.ChannelRepository
import dev.mudrock.tiviyomitvlauncher.data.repository.InputRepository
import dev.mudrock.tiviyomitvlauncher.data.repository.SettingsRepository
import dev.mudrock.tiviyomitvlauncher.data.resolver.AppResolver
import dev.mudrock.tiviyomitvlauncher.data.resolver.ChannelResolver
import dev.mudrock.tiviyomitvlauncher.data.resolver.InputResolver
import dev.mudrock.tiviyomitvlauncher.image.AppIconFetcher
import dev.mudrock.tiviyomitvlauncher.ui.tab.apps.AppsTabViewModel
import dev.mudrock.tiviyomitvlauncher.ui.tab.home.HomeTabViewModel
import dev.mudrock.tiviyomitvlauncher.util.DefaultLauncherHelper
import dev.mudrock.tiviyomitvlauncher.util.FocusController
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import timber.log.Timber

private val launcherModule = module {
    single { DefaultLauncherHelper(get()) }
    single { FocusController() }

    single { AppRepository(get(), get(), get()) }
    single { AppResolver() }

    single { ChannelRepository(get(), get(), get()) }
    single { ChannelResolver() }

    single { InputRepository(get(), get(), get()) }
    single { InputResolver() }

    single { SettingsRepository(get()) }
    single { BackupRepository(get(), get(), get(), get()) }

    viewModel { HomeTabViewModel(get(), get(), get()) }
    viewModel { AppsTabViewModel(get(), get()) }
}

private val databaseModule = module {
    // Create database(s)
    single { DatabaseContainer(get()) }
}

class LauncherApplication : Application(), ImageLoaderFactory, ComponentCallbacks2 {

    private var imageLoaderRef: ImageLoader? = null

    override fun onCreate() {
        super.onCreate()

        CrashHandler.init(this)

        Timber.plant(Timber.DebugTree())

        startKoin {
            androidLogger(level = if (BuildConfig.DEBUG) Level.DEBUG else Level.INFO)
            androidContext(this@LauncherApplication)

            modules(launcherModule, databaseModule)
        }
    }

    private fun isLowRamDevice(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return activityManager?.isLowRamDevice == true
    }

    override fun newImageLoader(): ImageLoader {
        val isLowRam = isLowRamDevice()
        // Dynamic memory cache sizing: 10% for low-RAM TV devices, 20% for standard devices
        val memoryCachePercent = if (isLowRam) 0.10 else 0.20
        // Dynamic disk cache limit (no fixed 100MB static allocation): 20MB for low-RAM, 50MB for standard
        val diskCacheMaxBytes = if (isLowRam) 20L * 1024 * 1024 else 50L * 1024 * 1024

        val loader = ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(memoryCachePercent)
                    .strongReferencesEnabled(false) // Allow GC under memory pressure
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(diskCacheMaxBytes)
                    .build()
            }
            .components {
                // Register keyer for proper cache key generation
                add(AppIconFetcher.AppKeyer())
                // Register fetcher for loading app icons
                add(AppIconFetcher.Factory(this@LauncherApplication))
            }
            .apply {
                if (isLowRam) {
                    bitmapConfig(Bitmap.Config.RGB_565)
                }
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            // App icons don't have HTTP cache headers
            .respectCacheHeaders(false)
            .build()

        imageLoaderRef = loader
        return loader
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Immediately release image memory cache when system indicates memory pressure (level >= 10 or UI hidden)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND || level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN || level >= 10) {
            Timber.d("LauncherApplication: Trimming image memory cache (level $level)")
            imageLoaderRef?.memoryCache?.clear()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("LauncherApplication: System low memory warning - clearing image cache")
        imageLoaderRef?.memoryCache?.clear()
    }
}
