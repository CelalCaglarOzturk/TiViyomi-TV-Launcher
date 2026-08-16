package dev.mudrock.tiviyomitvlauncher.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.mudrock.tiviyomitvlauncher.data.DatabaseContainer
import dev.mudrock.tiviyomitvlauncher.data.executeAsListFlow
import dev.mudrock.tiviyomitvlauncher.data.resolver.AppResolver
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.App
import timber.log.Timber

class AppRepository(
    private val context: Context,
    private val appResolver: AppResolver,
    private val database: DatabaseContainer,
) {
    private suspend fun commitApps(apps: Collection<App>) = withContext(Dispatchers.IO) {
        Timber.d("AppRepository: commitApps called with ${apps.size} apps")

        try {
            database.database.transaction {
                // Get existing apps from database
                val existingApps = database.apps.getAllIncludingHidden().executeAsList()
                val existingIds = existingApps.map { it.id }.toSet()
                val existingMap = existingApps.associateBy { it.id }
                Timber.d("AppRepository: Existing IDs in DB: ${existingIds.size}")

                val newIds = apps.map { it.id }.toSet()
                val idsToRemove = existingIds.subtract(newIds)

                Timber.d("AppRepository: IDs to remove: ${idsToRemove.size}")

                idsToRemove.forEach { id ->
                    database.apps.removeById(id)
                }

                // Upsert all found apps
                apps.forEach { app ->
                    val existing = existingMap[app.id]
                    val isNewApp = existing == null
                    // Only update if it's new or content changed
                    if (isNewApp || existing!!.displayName != app.displayName ||
                        existing.launchIntentUriDefault != app.launchIntentUriDefault ||
                        existing.launchIntentUriLeanback != app.launchIntentUriLeanback) {
                        commitApp(app, isNewApp)
                    }
                }
            }

            Timber.d("AppRepository: commitApps completed")
        } catch (e: Exception) {
            Timber.e(e, "AppRepository: Error in commitApps")
            throw e
        }
    }

    private fun commitApp(app: App, addToFavorites: Boolean = false) {
        try {
            database.apps.upsert(
                displayName = app.displayName,
                packageName = app.packageName,
                launchIntentUriDefault = app.launchIntentUriDefault,
                launchIntentUriLeanback = app.launchIntentUriLeanback,
                id = app.id
            )

            // If this is a new app, automatically add it to favorites (Home)
            // Don't add Launcher Settings or the Launcher itself to favorites
            // Only add TV apps (apps with leanback intent) to favorites automatically
            if (addToFavorites &&
                app.packageName != "dev.mudrock.tiviyomitvlauncher.settings" &&
                app.packageName != context.packageName &&
                app.launchIntentUriLeanback != null
            ) {
                Timber.d("AppRepository: Auto-adding ${app.displayName} to favorites")
                database.apps.updateFavoriteAdd(app.id)
            }

            // Ensure all apps have an order in the all apps list
            val currentApp = database.apps.getById(app.id).executeAsOne()
            if (currentApp.allAppsOrder == null) {
                database.apps.updateAllAppsOrderAdd(app.id)
            }
        } catch (e: Exception) {
            Timber.e(e, "AppRepository: Error upserting app ${app.packageName}")
            throw e
        }
    }

    suspend fun refreshAllApplications() = withContext(Dispatchers.IO) {
        Timber.d("AppRepository: refreshAllApplications called")
        try {
            val apps = appResolver.getApplications(context).toMutableList()

            Timber.d("AppRepository: Resolver returned ${apps.size} apps")

            if (apps.isNotEmpty()) {
                commitApps(apps)
                Timber.d("AppRepository: Apps committed to database")

                // Verify what's in the database
                val dbApps = database.apps.getAll().executeAsList()
                Timber.d("AppRepository: Database now contains ${dbApps.size} apps")
                dbApps.forEach { app ->
                    Timber.d("AppRepository: DB App - ${app.displayName} (${app.packageName})")
                }
            } else {
                Timber.w("AppRepository: No apps returned from resolver")
            }
        } catch (e: Exception) {
            Timber.e(e, "AppRepository: Error in refreshAllApplications")
            throw e
        }
    }

    suspend fun refreshApplication(packageName: String) = withContext(Dispatchers.IO) {
        val app = appResolver.getApplication(context, packageName)

        if (app == null) {
            database.apps.removeByPackageName(packageName)
        } else {
            // Check if this is a new app
            val existingApp = database.apps.getByPackageName(packageName).executeAsOneOrNull()
            val isNewApp = existingApp == null

            commitApp(app, addToFavorites = isNewApp)
        }
    }

    // Get visible apps (not hidden)
    fun getApps() = database.apps.getAll().executeAsListFlow()

    // Get hidden apps only
    fun getHiddenApps() = database.apps.getHidden().executeAsListFlow()

    // Get favorite apps (not hidden)
    fun getFavoriteApps() = database.apps.getAllFavorites(::App).executeAsListFlow()

    suspend fun getByPackageName(packageName: String) =
        withContext(Dispatchers.IO) { database.apps.getByPackageName(packageName).executeAsOneOrNull() }

    suspend fun favorite(id: String) = withContext(Dispatchers.IO) {
        database.apps.updateFavoriteAdd(id)
    }

    suspend fun unfavorite(id: String) = withContext(Dispatchers.IO) {
        database.apps.updateFavoriteRemove(id)
    }

    suspend fun updateFavoriteOrder(id: String, order: Int) = withContext(Dispatchers.IO) {
        val app = database.apps.getById(id).executeAsOneOrNull() ?: return@withContext
        val currentOrder = app.favoriteOrder ?: return@withContext

        if (order > currentOrder) {
            moveFavoriteAppDown(id)
        } else if (order < currentOrder) {
            moveFavoriteAppUp(id)
        }
    }

    suspend fun updateAllAppsOrder(id: String, order: Int) =
        withContext(Dispatchers.IO) { database.apps.updateAllAppsOrder(id, order.toLong()) }

    suspend fun moveFavoriteAppUp(id: String) = withContext(Dispatchers.IO) {
        database.database.transaction {
            val app = database.apps.getById(id).executeAsOneOrNull() ?: return@transaction
            val currentOrder = app.favoriteOrder ?: return@transaction
            val previousApp =
                database.apps.findPreviousFavorite(currentOrder).executeAsOneOrNull()
                    ?: return@transaction
            val previousOrder = previousApp.favoriteOrder

            database.apps.updateFavoriteOrderSimple(previousOrder, app.id)
            database.apps.updateFavoriteOrderSimple(currentOrder, previousApp.id)
        }
    }

    suspend fun moveFavoriteAppDown(id: String) = withContext(Dispatchers.IO) {
        database.database.transaction {
            val app = database.apps.getById(id).executeAsOneOrNull() ?: return@transaction
            val currentOrder = app.favoriteOrder ?: return@transaction
            val nextApp =
                database.apps.findNextFavorite(currentOrder).executeAsOneOrNull()
                    ?: return@transaction
            val nextOrder = nextApp.favoriteOrder

            database.apps.updateFavoriteOrderSimple(nextOrder, app.id)
            database.apps.updateFavoriteOrderSimple(currentOrder, nextApp.id)
        }
    }

    suspend fun hideApp(id: String) = withContext(Dispatchers.IO) {
        database.apps.hideApp(id)
    }

    suspend fun unhideApp(id: String) = withContext(Dispatchers.IO) {
        database.apps.unhideApp(id)
    }

    suspend fun resetApps() = withContext(Dispatchers.IO) {
        Timber.d("AppRepository: Clearing all apps to reset customizations")
        // Delete all apps - they will be re-added as "new" on next refresh
        // This ensures favorites are auto-populated correctly
        database.apps.deleteAllApps()
        Timber.d("AppRepository: All apps deleted, will be re-added on refresh")
    }
}
