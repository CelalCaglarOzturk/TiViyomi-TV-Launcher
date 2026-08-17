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
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val totalRamMb = memoryInfo.totalMem / (1024 * 1024)

        // Dynamic memory cache sizing for TV profile:
        // Low-RAM devices (≤2GB): 12% - keeps rows cached without high GC pressure
        // Mid-range devices (≤3GB): 25% - fast smooth scrolling cache
        // Normal devices (>3GB): 20%
        val memoryCachePercent = when {
            totalRamMb <= 2048 -> 0.12
            totalRamMb <= 3072 -> 0.25
            else -> 0.20
        }
        val diskCacheMaxBytes = when {
            totalRamMb <= 2048 -> 50L * 1024 * 1024
            else -> 150L * 1024 * 1024
        }

        val loader = ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(memoryCachePercent)
                    .strongReferencesEnabled(true) // Keep strong references so bitmaps survive GC while scrolling
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
            .crossfade(false)
            .precision(coil.size.Precision.INEXACT)
            .allowHardware(true)
            .allowRgb565(true)
            .apply {
                if (totalRamMb <= 2048) {
                    bitmapConfig(Bitmap.Config.RGB_565)
                }
            }
            // App icons and preview channel artwork don't have HTTP cache headers
            .respectCacheHeaders(false)
            .build()

        imageLoaderRef = loader
        return loader
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Only release memory cache when launcher UI is actually hidden in background
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            Timber.d("LauncherApplication: Trimming image memory cache because UI is hidden (level $level)")
            imageLoaderRef?.memoryCache?.clear()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("LauncherApplication: System low memory warning - clearing image cache")
        imageLoaderRef?.memoryCache?.clear()
    }
}
