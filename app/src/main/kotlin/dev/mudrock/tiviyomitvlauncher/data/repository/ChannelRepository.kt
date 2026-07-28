package dev.mudrock.tiviyomitvlauncher.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import dev.mudrock.tiviyomitvlauncher.data.DatabaseContainer
import dev.mudrock.tiviyomitvlauncher.data.executeAsListFlow
import dev.mudrock.tiviyomitvlauncher.data.model.ChannelType
import dev.mudrock.tiviyomitvlauncher.data.resolver.ChannelResolver
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.Channel
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.ChannelProgram
import timber.log.Timber

class ChannelRepository(
    private val context: Context,
    private val channelResolver: ChannelResolver,
    private val database: DatabaseContainer,
) {
    private suspend fun commitChannels(type: ChannelType, channels: Collection<Channel>) = withContext(Dispatchers.IO) {
        database.database.transaction {
            // Remove channels found in database but not in committed list
            val existingIds = database.channels.getByType(type).executeAsList().map { it.id }
            val newIds = channels.map { it.id }.toSet()
            val idsToRemove = existingIds.subtract(newIds)

            idsToRemove.forEach { id ->
                database.channels.removeById(id)
            }

            // Upsert channels (preserving enabled and displayOrder)
            channels.forEach { channel ->
                commitChannel(channel)
            }
        }
    }

    private fun commitChannel(channel: Channel) {
        // Check if channel already exists to preserve enabled/displayOrder
        val existing = database.channels.getById(channel.id).executeAsOneOrNull()

        database.channels.upsert(
            id = channel.id,
            type = channel.type,
            channelId = channel.channelId,
            displayName = channel.displayName,
            description = channel.description,
            packageName = channel.packageName,
            appLinkIntentUri = channel.appLinkIntentUri,
        )

        // If this is a new channel, set initial display order
        if (existing == null) {
            database.channels.updateDisplayOrderAdd(channel.id)
        }
    }

    private suspend fun commitChannelPrograms(
        channelId: String,
        programs: Collection<ChannelProgram>,
    ) = withContext(Dispatchers.IO) {
        database.database.transaction {
            // Remove channels found in database but not in committed list
            val existingIds = database.channelPrograms.getByChannel(channelId).executeAsList().map { it.id }
            val newIds = programs.map { it.id }.toSet()
            val idsToRemove = existingIds.subtract(newIds)

            idsToRemove.forEach { id ->
                database.channelPrograms.removeById(id)
            }

            // Upsert channels
            programs.forEach { program ->
                commitChannelProgram(program)
            }
        }
    }

    private fun commitChannelProgram(program: ChannelProgram) {
        database.channelPrograms.upsert(
            id = program.id,
            channelId = program.channelId,
            packageName = program.packageName,
            weight = program.weight,
            type = program.type,
            posterArtUri = program.posterArtUri,
            posterArtAspectRatio = program.posterArtAspectRatio,
            lastPlaybackPositionMillis = program.lastPlaybackPositionMillis,
            durationMillis = program.durationMillis,
            releaseDate = program.releaseDate,
            itemCount = program.itemCount,
            interactionType = program.interactionType,
            interactionCount = program.interactionCount,
            author = program.author,
            genre = program.genre,
            live = program.live,
            startTimeUtcMillis = program.startTimeUtcMillis,
            endTimeUtcMillis = program.endTimeUtcMillis,
            title = program.title,
            episodeTitle = program.episodeTitle,
            seasonNumber = program.seasonNumber,
            episodeNumber = program.episodeNumber,
            description = program.description,
            intentUri = program.intentUri,
        )
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    suspend fun refreshAllChannels() = withContext(Dispatchers.IO) {
        if (_isRefreshing.value) return@withContext
        _isRefreshing.value = true
        try {
            Timber.d("Refreshing all channels")
            withContext(Dispatchers.Main) {
                try {
                    coil.Coil.imageLoader(context).memoryCache?.clear()
                } catch (e: Exception) {
                    Timber.w(e, "Failed to clear Coil memory cache on refresh")
                }
            }
            refreshWatchNextChannels()
            refreshPreviewChannels()

            // Preload images for all program cards into Coil memory/disk cache before stopping refresh spinner
            preloadAllProgramImages()
            Timber.d("All channels refreshed")
        } finally {
            _isRefreshing.value = false
        }
    }

    private suspend fun preloadAllProgramImages() = withContext(Dispatchers.IO) {
        try {
            val allPrograms = database.channelPrograms.getAll().executeAsList()
            val imageLoader = coil.Coil.imageLoader(context)

            coroutineScope {
                allPrograms.filter { !it.posterArtUri.isNullOrEmpty() }.chunked(6).forEach { batch ->
                    batch.map { program ->
                        async {
                            try {
                                val uri = program.posterArtUri!!
                                val request = coil.request.ImageRequest.Builder(context)
                                    .data(uri)
                                    .memoryCacheKey("program:${program.id}:$uri")
                                    .diskCacheKey("program:${program.id}:$uri")
                                    .build()
                                imageLoader.execute(request)
                            } catch (e: Exception) {
                                // Ignore individual image load failures during preload
                            }
                        }
                    }.awaitAll()
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to preload program images")
        }
    }

    suspend fun refreshPreviewChannels() {
        Timber.d("Refreshing preview channels")
        val channels = channelResolver.getPreviewChannels(context)
        commitChannels(ChannelType.PREVIEW, channels)

        for (channel in channels) refreshChannelPrograms(channel)
        Timber.d("Preview channels refreshed: ${channels.size} channels")
    }

    suspend fun refreshWatchNextChannels() {
        Timber.d("Refreshing Watch Next channels")
        val channel = Channel(
            id = ChannelResolver.CHANNEL_ID_WATCH_NEXT,
            type = ChannelType.WATCH_NEXT,
            channelId = -1,
            displayName = "",
            description = null,
            packageName = "",
            appLinkIntentUri = null,
            enabled = true,
            displayOrder = null,
        )
        val programs = channelResolver.getWatchNextPrograms(context)
        val blacklistedPackages = database.watchNextBlacklist.getAll().executeAsList()
        val filteredPrograms = programs.filter { it.packageName !in blacklistedPackages }

        commitChannel(channel)
        commitChannelPrograms(channel.id, filteredPrograms)
        Timber.d("Watch Next channels refreshed: ${filteredPrograms.size} programs")
    }

    suspend fun refreshChannelPrograms(channel: Channel) {
        val programs = channelResolver.getChannelPrograms(context, channel.channelId)
        commitChannelPrograms(channel.id, programs)
    }

    // Channel enable/disable
    suspend fun enableChannel(channelId: String) = withContext(Dispatchers.IO) {
        database.channels.enableChannel(channelId)
        // Assign display order when enabling
        database.channels.updateDisplayOrderAdd(channelId)
    }

    suspend fun disableChannel(channelId: String) = withContext(Dispatchers.IO) {
        database.channels.disableChannel(channelId)
    }

    suspend fun setChannelEnabled(channelId: String, enabled: Boolean) {
        if (enabled) enableChannel(channelId) else disableChannel(channelId)
    }

    // Channel reordering
    suspend fun updateChannelOrder(channelId: String, newOrder: Long) = withContext(Dispatchers.IO) {
        database.channels.updateDisplayOrderShift(order = newOrder, id = channelId)
    }

    suspend fun moveChannelUp(channelId: String) = withContext(Dispatchers.IO) {
        database.database.transaction {
            val channel = database.channels.getById(channelId).executeAsOneOrNull() ?: return@transaction
            val currentOrder = channel.displayOrder ?: return@transaction
            val previousChannel =
                database.channels.findPrevious(currentOrder).executeAsOneOrNull() ?: return@transaction
            val previousOrder = previousChannel.displayOrder ?: return@transaction

            database.channels.updateDisplayOrder(previousOrder, channel.id)
            database.channels.updateDisplayOrder(currentOrder, previousChannel.id)
        }
    }

    suspend fun moveChannelDown(channelId: String) = withContext(Dispatchers.IO) {
        database.database.transaction {
            val channel = database.channels.getById(channelId).executeAsOneOrNull() ?: return@transaction
            val currentOrder = channel.displayOrder ?: return@transaction
            val nextChannel = database.channels.findNext(currentOrder).executeAsOneOrNull() ?: return@transaction
            val nextOrder = nextChannel.displayOrder ?: return@transaction

            database.channels.updateDisplayOrder(nextOrder, channel.id)
            database.channels.updateDisplayOrder(currentOrder, nextChannel.id)
        }
    }

    // Watch Next Blacklist
    suspend fun addToWatchNextBlacklist(packageName: String) = withContext(Dispatchers.IO) {
        database.watchNextBlacklist.insert(packageName)
        refreshWatchNextChannels()
    }

    suspend fun removeFromWatchNextBlacklist(packageName: String) = withContext(Dispatchers.IO) {
        database.watchNextBlacklist.delete(packageName)
        refreshWatchNextChannels()
    }

    suspend fun clearWatchNextBlacklist() = withContext(Dispatchers.IO) {
        val all = database.watchNextBlacklist.getAll().executeAsList()
        all.forEach { database.watchNextBlacklist.delete(it) }
        refreshWatchNextChannels()
    }

    suspend fun resetChannels() = withContext(Dispatchers.IO) {
        // Delete all channels - they will be re-added on next refresh
        // This ensures all channels are enabled and ordered correctly
        database.channels.deleteAllChannels()
    }

    suspend fun removeWatchNextProgram(programId: String) = withContext(Dispatchers.IO) {
        val internalId = programId.removePrefix(ChannelResolver.CHANNEL_PROGRAM_ID_PREFIX).toLongOrNull()
        if (internalId != null) {
            channelResolver.removeWatchNextProgram(context, internalId)
            // Optimize: remove from local DB directly to avoid full refresh
            database.channelPrograms.removeById(programId)
        }
    }

    // Getters
    fun getChannels() = database.channels.getAll().executeAsListFlow()
    fun getEnabledChannels() = database.channels.getAllEnabled().executeAsListFlow()
    fun getProgramsByChannel(channelId: String) = database.channelPrograms.getByChannel(channelId).executeAsListFlow()
    fun getWatchNextPrograms() =
        database.channelPrograms.getByChannel(ChannelResolver.CHANNEL_ID_WATCH_NEXT).executeAsListFlow()

    fun getWatchNextBlacklist() = database.watchNextBlacklist.getAll().executeAsListFlow()
}
