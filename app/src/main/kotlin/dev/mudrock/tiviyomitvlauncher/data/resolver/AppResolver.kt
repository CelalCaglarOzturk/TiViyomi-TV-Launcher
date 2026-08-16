package dev.mudrock.tiviyomitvlauncher.data.resolver

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.App
import timber.log.Timber

class AppResolver {
    companion object {
        private val launcherCategories = arrayOf(
            Intent.CATEGORY_LEANBACK_LAUNCHER,
            Intent.CATEGORY_LAUNCHER,
        )

        const val APP_ID_PREFIX = "app:"
    }

    fun getApplication(context: Context, packageId: String): App? {
        val packageManager = context.packageManager

        val leanbackIntent = try {
            packageManager.getLeanbackLaunchIntentForPackage(packageId)
        } catch (e: Exception) {
            null
        }

        val launchIntent = try {
            packageManager.getLaunchIntentForPackage(packageId)
        } catch (e: Exception) {
            null
        }

        if (leanbackIntent == null && launchIntent == null) return null

        val displayName = try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageId, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageId, 0)
            }
            appInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            packageId
        }

        return App(
            id = "$APP_ID_PREFIX$packageId",
            displayName = displayName,
            packageName = packageId,
            launchIntentUriDefault = launchIntent?.toUri(0),
            launchIntentUriLeanback = leanbackIntent?.toUri(0),
            favoriteOrder = null,
            hidden = 0,
            allAppsOrder = null,
        )
    }

    fun getApplications(context: Context): List<App> {
        val packageManager = context.packageManager

        Timber.d("AppResolver: Starting to query applications")

        // Fast path: Query leanback and standard launcher categories
        val categoryApps = getCategoryApps(packageManager)
        if (categoryApps.isNotEmpty()) {
            Timber.d("AppResolver: Total unique apps found via categories: ${categoryApps.size}")
            return categoryApps
        }

        // Fallback only if no category apps were found
        val fallbackApps = getInstalledAppsWithLaunchIntent(packageManager)
        Timber.d("AppResolver: Total unique apps found via fallback: ${fallbackApps.size}")
        return fallbackApps
    }

    private fun getCategoryApps(packageManager: PackageManager): List<App> {
        val leanbackIntents = mutableMapOf<String, String>()
        val defaultIntents = mutableMapOf<String, String>()
        val displayNames = mutableMapOf<String, String>()

        fun processActivities(activities: List<ResolveInfo>, isLeanback: Boolean) {
            activities.forEach { info ->
                val packageName = info.activityInfo.packageName
                val className = info.activityInfo.name

                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(if (isLeanback) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
                    .setClassName(packageName, className)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                val uri = intent.toUri(0)

                if (isLeanback) {
                    // Prefer activities that are already set if they are different? 
                    // Usually there's only one launcher activity per category.
                    leanbackIntents[packageName] = uri
                } else {
                    defaultIntents[packageName] = uri
                }

                if (!displayNames.containsKey(packageName)) {
                    try {
                        displayNames[packageName] = info.loadLabel(packageManager).toString()
                    } catch (e: Exception) {
                        displayNames[packageName] = packageName
                    }
                }
            }
        }

        try {
            val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            val activities = queryIntentActivitiesSafe(packageManager, intent)
            Timber.d("AppResolver: Found ${activities.size} leanback activities")
            processActivities(activities, true)
        } catch (e: Exception) {
            Timber.e(e, "AppResolver: Error querying leanback category")
        }

        try {
            val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
            val activities = queryIntentActivitiesSafe(packageManager, intent)
            Timber.d("AppResolver: Found ${activities.size} launcher activities")
            processActivities(activities, false)
        } catch (e: Exception) {
            Timber.e(e, "AppResolver: Error querying launcher category")
        }

        val allPackages = leanbackIntents.keys + defaultIntents.keys

        return allPackages.map { packageName ->
            App(
                id = "$APP_ID_PREFIX$packageName",
                displayName = displayNames[packageName] ?: packageName,
                packageName = packageName,
                launchIntentUriDefault = defaultIntents[packageName],
                launchIntentUriLeanback = leanbackIntents[packageName],
                favoriteOrder = null,
                hidden = 0,
                allAppsOrder = null,
            )
        }
    }

    /**
     * Fallback method: Get all installed applications that have a launch intent
     */
    private fun getInstalledAppsWithLaunchIntent(packageManager: PackageManager): List<App> {
        Timber.d("AppResolver: Getting installed apps with launch intent")

        val installedApps = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }
        } catch (e: Exception) {
            Timber.e(e, "AppResolver: Error getting installed applications")
            emptyList()
        }

        Timber.d("AppResolver: Found ${installedApps.size} total installed applications")

        val apps = installedApps
            .mapNotNull { appInfo ->
                try {
                    applicationInfoToApp(packageManager, appInfo)
                } catch (e: Exception) {
                    null
                }
            }

        Timber.d("AppResolver: Found ${apps.size} launchable apps via fallback")
        return apps
    }

    private fun hasLaunchIntent(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            packageManager.getLaunchIntentForPackage(packageName) != null ||
                    packageManager.getLeanbackLaunchIntentForPackage(packageName) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun applicationInfoToApp(packageManager: PackageManager, appInfo: ApplicationInfo): App? {
        val packageName = appInfo.packageName

        val launchIntent = try {
            packageManager.getLaunchIntentForPackage(packageName)
        } catch (e: Exception) {
            null
        }

        val leanbackIntent = try {
            packageManager.getLeanbackLaunchIntentForPackage(packageName)
        } catch (e: Exception) {
            null
        }

        // Skip if no launch intent at all
        if (launchIntent == null && leanbackIntent == null) {
            return null
        }

        val displayName = try {
            appInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            packageName
        }

        return App(
            id = "$APP_ID_PREFIX$packageName",
            displayName = displayName,
            packageName = packageName,
            launchIntentUriDefault = launchIntent?.toUri(0),
            launchIntentUriLeanback = leanbackIntent?.toUri(0),
            favoriteOrder = null,
            hidden = 0,
            allAppsOrder = null,
        )
    }

    private fun queryIntentActivitiesSafe(
        packageManager: PackageManager,
        intent: Intent
    ): List<ResolveInfo> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }
        } catch (e: Exception) {
            Timber.e(e, "AppResolver: Error in queryIntentActivities")
            emptyList()
        }
    }


}
