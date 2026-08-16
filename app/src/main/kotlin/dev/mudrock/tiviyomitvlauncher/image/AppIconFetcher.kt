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
        var drawable = loadAppIcon()

        // Downsample if a specific size is requested
        val size = options.size
        val width = size.width
        val height = size.height
        if (width is Dimension.Pixels && height is Dimension.Pixels) {
            val w = width.px
            val h = height.px
            if (w > 0 && h > 0) {
                val config = if (options.allowRgb565) android.graphics.Bitmap.Config.RGB_565 else android.graphics.Bitmap.Config.ARGB_8888
                val bitmap = createBitmap(w, h, config)
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, w, h)
                drawable.draw(canvas)
                drawable = bitmap.toDrawable(context.resources)
            }
        }

        DrawableResult(
            drawable = drawable,
			isSampled = true,
            // Report MEMORY for settings icon (already in resources)
            // Report DISK for app icons loaded from package manager (cached by system)
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
        if (intentUri != null) {
            try {
                val intent = Intent.parseUri(intentUri, 0)

                // First try to get the banner (preferred for TV)
                val banner = try {
                    packageManager.getActivityBanner(intent)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }

                if (banner != null) {
                    return banner
                }

                // Fall back to activity icon
                val icon = try {
                    packageManager.getActivityIcon(intent)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }

                if (icon != null) {
                    return icon
                }
            } catch (e: Exception) {
                // Ignore parse errors and fall through to default
            }
        }

        // Final fallback to default activity icon
        return packageManager.defaultActivityIcon
    }

    /**
     * Keyer implementation for proper Coil caching.
     * Uses the app ID as the cache key since that uniquely identifies the app.
     */
    class AppKeyer : Keyer<App> {
        override fun key(data: App, options: Options): String {
            // Use app ID as cache key - this is stable and unique per app
            // Include size to support different requested sizes (e.g. focused vs unfocused)
            return "app_icon:${data.id}:${options.size}"
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<App> {
        override fun create(data: App, options: Options, imageLoader: ImageLoader): Fetcher {
            return AppIconFetcher(data, context, options)
        }
    }

    companion object {
        private const val SETTINGS_PACKAGE_NAME = "dev.mudrock.tiviyomitvlauncher.settings"
    }
}
