package dev.mudrock.tiviyomitvlauncher.image

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import coil.size.Dimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.mudrock.tiviyomitvlauncher.R
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.App
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

/**
 * Custom Coil fetcher for loading app icons efficiently.
 * Implements proper cache keying based on app ID to prevent redundant loads.
 */
class AppIconFetcher(
    private val app: App,
    private val context: Context,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val size = options.size
        val width = size.width
        val height = size.height
        val w = if (width is Dimension.Pixels) width.px else 0
        val h = if (height is Dimension.Pixels) height.px else 0

        val cacheKey = if (w > 0 && h > 0) "${app.id}_${w}x${h}" else app.id
        
        iconCache.get(cacheKey)?.let { cached ->
            return@withContext DrawableResult(
                drawable = cached,
                isSampled = true,
                dataSource = DataSource.MEMORY
            )
        }

        val originalDrawable = loadAppIcon()

        val finalDrawable = if (w > 0 && h > 0) {
            val config = if (options.allowRgb565) android.graphics.Bitmap.Config.RGB_565 else android.graphics.Bitmap.Config.ARGB_8888
            val bitmap = createBitmap(w, h, config)
            val canvas = android.graphics.Canvas(bitmap)
            synchronized(originalDrawable) {
                originalDrawable.setBounds(0, 0, w, h)
                originalDrawable.draw(canvas)
            }
            val drawable = bitmap.toDrawable(context.resources)
            iconCache.put(cacheKey, drawable)
            drawable
        } else {
            iconCache.put(cacheKey, originalDrawable)
            originalDrawable
        }

        DrawableResult(
            drawable = finalDrawable,
            isSampled = true,
            dataSource = if (app.packageName == SETTINGS_PACKAGE_NAME) {
                DataSource.MEMORY
            } else {
                DataSource.DISK
            }
        )
    }

    private fun loadAppIcon(): Drawable {
        // Special case for settings - use launcher icon
        if (app.packageName == SETTINGS_PACKAGE_NAME) {
            return ContextCompat.getDrawable(context, R.drawable.ic_launcher)!!
        }

        val packageManager = context.packageManager

        // Try to get the app icon/banner from the launch intent
        val intentUri = app.launchIntentUriLeanback ?: app.launchIntentUriDefault
        val loadedDrawable = if (intentUri != null) {
            try {
                val intent = Intent.parseUri(intentUri, 0)

                // First try to get the banner (preferred for TV)
                val banner = try {
                    packageManager.getActivityBanner(intent)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }

                if (banner != null) {
                    banner
                } else {
                    // Fall back to activity icon
                    try {
                        packageManager.getActivityIcon(intent)
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    }
                }
            } catch (e: Exception) {
                null
            }
        } else null

        return loadedDrawable ?: packageManager.defaultActivityIcon
    }

    /**
     * Keyer implementation for proper Coil caching.
     * Uses the app ID and requested size as the cache key.
     */
    class AppKeyer : Keyer<App> {
        override fun key(data: App, options: Options): String {
            val size = options.size
            val width = size.width
            val height = size.height
            val w = if (width is Dimension.Pixels) width.px else 0
            val h = if (height is Dimension.Pixels) height.px else 0
            return if (w > 0 && h > 0) "app_icon:${data.id}:${w}x${h}" else "app_icon:${data.id}"
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<App> {
        override fun create(data: App, options: Options, imageLoader: ImageLoader): Fetcher {
            return AppIconFetcher(data, context, options)
        }
    }

    companion object {
        private const val SETTINGS_PACKAGE_NAME = "dev.mudrock.tiviyomitvlauncher.settings"
        private val iconCache = android.util.LruCache<String, Drawable>(120)

        fun clearIconCache() {
            iconCache.evictAll()
        }
    }
}
