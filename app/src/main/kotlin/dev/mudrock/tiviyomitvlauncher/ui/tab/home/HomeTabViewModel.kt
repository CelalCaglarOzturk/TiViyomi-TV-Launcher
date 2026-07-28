package dev.mudrock.tiviyomitvlauncher.ui.tab.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dev.mudrock.tiviyomitvlauncher.data.repository.AppRepository
import dev.mudrock.tiviyomitvlauncher.data.repository.ChannelRepository
import dev.mudrock.tiviyomitvlauncher.data.repository.SettingsRepository
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.App
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.Channel
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.ChannelProgram

class HomeTabViewModel(
    private val appRepository: AppRepository,
    private val channelRepository: ChannelRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    companion object {
        // Use WhileSubscribed with a 20 second timeout to keep data cached during tab switches
        // This prevents reloading when quickly switching tabs while still allowing cleanup
        // when the screen is completely left
        private val SHARING_STARTED = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = 20000L,
            replayExpirationMillis = Long.MAX_VALUE // Never expire the replay cache
        )
    }

    val apps = appRepository.getFavoriteApps()
        .map { apps ->
            // Filter out Launcher Settings and the Launcher itself from Home tab
            apps.filterNot {
                it.packageName == "dev.mudrock.tiviyomitvlauncher.settings" ||
                        it.packageName == "dev.mudrock.tiviyomitvlauncher"
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SHARING_STARTED, emptyList())

    // Optimized map for fast O(1) lookup by package name for favorite apps
    val appsMap = apps
        .map { it.associateBy { app -> app.packageName } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SHARING_STARTED, emptyMap())

    // Map of all installed non-hidden apps for matching channel package names regardless of favorite status
    val allAppsMap = appRepository.getApps()
        .map { apps -> apps.associateBy { app -> app.packageName } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SHARING_STARTED, emptyMap())

    val isRefreshing = channelRepository.isRefreshing
        .stateIn(viewModelScope, SHARING_STARTED, false)

    val channels = channelRepository.getEnabledChannels()
        .stateIn(viewModelScope, SHARING_STARTED, emptyList())

    val allAppChannels = channelRepository.getChannels()
        .stateIn(viewModelScope, SHARING_STARTED, emptyList())

    val watchNextPrograms = channelRepository.getWatchNextPrograms()
        .stateIn(viewModelScope, SHARING_STARTED, emptyList())

    val watchNextBlacklist = channelRepository.getWatchNextBlacklist()
        .stateIn(viewModelScope, SHARING_STARTED, emptyList())

    val appCardSize = settingsRepository.appCardSize
        .stateIn(viewModelScope, SHARING_STARTED, SettingsRepository.DEFAULT_APP_CARD_SIZE)

    val channelCardSize = settingsRepository.channelCardSize
        .stateIn(viewModelScope, SHARING_STARTED, SettingsRepository.DEFAULT_CHANNEL_CARD_SIZE)

    private val _channelProgramsCache = mutableMapOf<String, StateFlow<List<ChannelProgram>>>()

    fun channelPrograms(channelId: String): StateFlow<List<ChannelProgram>> {
        return _channelProgramsCache.getOrPut(channelId) {
            channelRepository.getProgramsByChannel(channelId)
                .flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SHARING_STARTED, emptyList())
        }
    }

    fun favoriteApp(app: App, favorite: Boolean) = viewModelScope.launch {
        // Return if state is already satisfied
        if ((app.favoriteOrder != null) == favorite) return@launch

        if (favorite) appRepository.favorite(app.id)
        else appRepository.unfavorite(app.id)
    }

    fun setFavoriteOrder(app: App, targetIndex: Int) = viewModelScope.launch {
        // Make sure app is favorite first
        if (app.favoriteOrder == null) appRepository.favorite(app.id)

        val currentList = apps.value
        val currentIndex = currentList.indexOfFirst { it.id == app.id }

        if (currentIndex == -1) return@launch

        if (targetIndex > currentIndex) {
            appRepository.moveFavoriteAppDown(app.id)
        } else if (targetIndex < currentIndex) {
            appRepository.moveFavoriteAppUp(app.id)
        }
    }

    fun moveAppUp(app: App) = viewModelScope.launch {
        if (app.favoriteOrder != null) {
            appRepository.moveFavoriteAppUp(app.id)
        }
    }

    fun moveAppDown(app: App) = viewModelScope.launch {
        if (app.favoriteOrder != null) {
            appRepository.moveFavoriteAppDown(app.id)
        }
    }

    fun hideApp(app: App) = viewModelScope.launch {
        appRepository.hideApp(app.id)
    }

    fun toggleWatchNextBlacklist(packageName: String, blacklisted: Boolean) = viewModelScope.launch {
        if (blacklisted) {
            channelRepository.addToWatchNextBlacklist(packageName)
        } else {
            channelRepository.removeFromWatchNextBlacklist(packageName)
        }
    }

    fun removeWatchNextProgram(programId: String) = viewModelScope.launch {
        channelRepository.removeWatchNextProgram(programId)
    }

    // Channel management functions
    fun enableChannel(channel: Channel) = viewModelScope.launch {
        channelRepository.enableChannel(channel.id)
    }

    fun setChannelEnabled(channel: Channel, enabled: Boolean) = viewModelScope.launch {
        channelRepository.setChannelEnabled(channel.id, enabled)
    }

    fun moveChannelUp(channel: Channel) = viewModelScope.launch {
        channelRepository.moveChannelUp(channel.id)
    }

    fun moveChannelDown(channel: Channel) = viewModelScope.launch {
        channelRepository.moveChannelDown(channel.id)
    }
}
