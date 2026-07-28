package dev.mudrock.tiviyomitvlauncher.data.repository

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dev.mudrock.tiviyomitvlauncher.util.LauncherConstants
import timber.log.Timber

class SettingsRepository(
    context: Context
) {
    companion object {
        private const val PREFS_NAME = "launcher_settings"
        
        // Preference Keys
        private const val KEY_APP_CARD_SIZE = "app_card_size"
        private const val KEY_CHANNEL_CARD_SIZE = "channel_card_size"
        private const val KEY_HIDDEN_INPUTS = "hidden_inputs"
        private const val KEY_SHOW_MOBILE_APPS = "show_mobile_apps"
        private const val KEY_ENABLE_ANIMATIONS = "enable_animations"
        private const val KEY_ANIM_APP_ICON = "anim_app_icon"
        private const val KEY_ANIM_CHANNEL_ROW = "anim_channel_row"
        private const val KEY_ANIM_CHANNEL_MOVE = "anim_channel_move"
        private const val KEY_ANIM_APP_MOVE = "anim_app_move"
        private const val KEY_TOOLBAR_ITEMS_ORDER = "toolbar_items_order"
        private const val KEY_TOOLBAR_ITEMS_ENABLED = "toolbar_items_enabled"
        private const val KEY_CHANNEL_CARDS_PER_ROW = "channel_cards_per_row"
        private const val KEY_SUPPRESS_ORIGINAL_LAUNCHER = "suppress_original_launcher"
        private const val KEY_SUPPRESS_LAUNCHER_ONLY_EXTERNAL = "suppress_launcher_only_external"
        private const val KEY_DEVELOPER_MODE = "developer_mode"
        private const val KEY_BACKGROUND_COLOR = "background_color"
        private const val KEY_BACKGROUND_IMAGE_URI = "background_image_uri"

        // Legacy constants for backwards compatibility
        const val DEFAULT_APP_CARD_SIZE = LauncherConstants.CardSize.DEFAULT_APP_CARD_SIZE
        const val DEFAULT_CHANNEL_CARD_SIZE = LauncherConstants.CardSize.DEFAULT_CHANNEL_CARD_SIZE
        const val DEFAULT_CHANNEL_CARDS_PER_ROW = LauncherConstants.ChannelSettings.DEFAULT_CARDS_PER_ROW

        // Default toolbar items order (includes clock at the end/right side)
        val DEFAULT_TOOLBAR_ITEMS_ORDER = listOf(
            ToolbarItem.INPUTS.name,
            ToolbarItem.NOTIFICATIONS.name,
            ToolbarItem.WIFI.name,
            ToolbarItem.LAUNCHER_SETTINGS.name,
            ToolbarItem.SETTINGS.name,
            ToolbarItem.REFRESH.name,
            ToolbarItem.CLOCK.name
        )

        // Default enabled state for all items (all enabled by default, including launcher settings)
        val DEFAULT_TOOLBAR_ITEMS_ENABLED = ToolbarItem.entries.map { it.name }.toSet()

        enum class ToolbarItem {
            CLOCK, INPUTS, NOTIFICATIONS, WIFI, REFRESH, LAUNCHER_SETTINGS, SETTINGS
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // App Settings
    private val _appCardSize = MutableStateFlow(prefs.getInt(KEY_APP_CARD_SIZE, LauncherConstants.CardSize.DEFAULT_APP_CARD_SIZE))
    val appCardSize = _appCardSize.asStateFlow()

    // Channel Settings
    private val _channelCardSize = MutableStateFlow(prefs.getInt(KEY_CHANNEL_CARD_SIZE, LauncherConstants.CardSize.DEFAULT_CHANNEL_CARD_SIZE))
    val channelCardSize = _channelCardSize.asStateFlow()

    private val _channelCardsPerRow = MutableStateFlow(
        prefs.getInt(KEY_CHANNEL_CARDS_PER_ROW, LauncherConstants.ChannelSettings.DEFAULT_CARDS_PER_ROW)
    )
    val channelCardsPerRow = _channelCardsPerRow.asStateFlow()

    // Input Settings
    private val _hiddenInputs = MutableStateFlow(prefs.getStringSet(KEY_HIDDEN_INPUTS, emptySet()) ?: emptySet())
    val hiddenInputs = _hiddenInputs.asStateFlow()

    // App Display Settings
    private val _showMobileApps = MutableStateFlow(prefs.getBoolean(KEY_SHOW_MOBILE_APPS, false))
    val showMobileApps = _showMobileApps.asStateFlow()

    // Animation Settings
    private val _enableAnimations = MutableStateFlow(prefs.getBoolean(KEY_ENABLE_ANIMATIONS, LauncherConstants.Animations.DEFAULT_ENABLED))
    val enableAnimations = _enableAnimations.asStateFlow()

    private val _animAppIcon = MutableStateFlow(prefs.getBoolean(KEY_ANIM_APP_ICON, LauncherConstants.Animations.DEFAULT_ENABLED))
    val animAppIcon = _animAppIcon.asStateFlow()

    private val _animChannelRow = MutableStateFlow(prefs.getBoolean(KEY_ANIM_CHANNEL_ROW, LauncherConstants.Animations.DEFAULT_ENABLED))
    val animChannelRow = _animChannelRow.asStateFlow()

    private val _animChannelMove = MutableStateFlow(prefs.getBoolean(KEY_ANIM_CHANNEL_MOVE, LauncherConstants.Animations.DEFAULT_ENABLED))
    val animChannelMove = _animChannelMove.asStateFlow()

    private val _animAppMove = MutableStateFlow(prefs.getBoolean(KEY_ANIM_APP_MOVE, LauncherConstants.Animations.DEFAULT_ENABLED))
    val animAppMove = _animAppMove.asStateFlow()

    // Toolbar Settings
    private val _toolbarItemsOrder = MutableStateFlow(
        prefs.getStringSet(KEY_TOOLBAR_ITEMS_ORDER, null)?.toList() ?: DEFAULT_TOOLBAR_ITEMS_ORDER
    )
    val toolbarItemsOrder = _toolbarItemsOrder.asStateFlow()

    private val _toolbarItemsEnabled = MutableStateFlow(
        prefs.getStringSet(KEY_TOOLBAR_ITEMS_ENABLED, null)?.toSet() ?: DEFAULT_TOOLBAR_ITEMS_ENABLED
    )
    val toolbarItemsEnabled = _toolbarItemsEnabled.asStateFlow()

    // Launcher Suppression Settings
    private val _suppressOriginalLauncher = MutableStateFlow(
        prefs.getBoolean(KEY_SUPPRESS_ORIGINAL_LAUNCHER, false)
    )
    val suppressOriginalLauncher = _suppressOriginalLauncher.asStateFlow()

    private val _suppressLauncherOnlyExternal = MutableStateFlow(
        prefs.getBoolean(KEY_SUPPRESS_LAUNCHER_ONLY_EXTERNAL, false)
    )
    val suppressLauncherOnlyExternal = _suppressLauncherOnlyExternal.asStateFlow()

    // Developer Settings
    private val _developerMode = MutableStateFlow(
        prefs.getBoolean(KEY_DEVELOPER_MODE, false)
    )
    val developerMode = _developerMode.asStateFlow()

    // Background Settings
    private val _backgroundColor = MutableStateFlow(
        prefs.getLong(KEY_BACKGROUND_COLOR, LauncherConstants.Background.DEFAULT_BACKGROUND_COLOR)
    )
    val backgroundColor = _backgroundColor.asStateFlow()

    private val _backgroundImageUri = MutableStateFlow(
        prefs.getString(KEY_BACKGROUND_IMAGE_URI, null)
    )
    val backgroundImageUri = _backgroundImageUri.asStateFlow()

    // === App Settings ===

    fun setAppCardSize(size: Int) {
        try {
            val coercedSize = size.coerceIn(
                LauncherConstants.CardSize.MIN_APP_CARD_SIZE,
                LauncherConstants.CardSize.MAX_APP_CARD_SIZE
            )
            prefs.edit { putInt(KEY_APP_CARD_SIZE, coercedSize) }
            _appCardSize.value = coercedSize
        } catch (e: Exception) {
            Timber.e(e, "Failed to set app card size")
        }
    }

    fun setShowMobileApps(show: Boolean) {
        try {
            prefs.edit { putBoolean(KEY_SHOW_MOBILE_APPS, show) }
            _showMobileApps.value = show
        } catch (e: Exception) {
            Timber.e(e, "Failed to set show mobile apps")
        }
    }

    fun toggleShowMobileApps() {
        setShowMobileApps(!_showMobileApps.value)
    }

    // === Channel Settings ===

    fun setChannelCardSize(size: Int) {
        try {
            val coercedSize = size.coerceIn(
                LauncherConstants.CardSize.MIN_APP_CARD_SIZE,
                LauncherConstants.CardSize.MAX_APP_CARD_SIZE
            )
            prefs.edit { putInt(KEY_CHANNEL_CARD_SIZE, coercedSize) }
            _channelCardSize.value = coercedSize
        } catch (e: Exception) {
            Timber.e(e, "Failed to set channel card size")
        }
    }

    fun setChannelCardsPerRow(count: Int) {
        try {
            val coercedCount = count.coerceIn(
                LauncherConstants.ChannelSettings.MIN_CARDS_PER_ROW,
                LauncherConstants.ChannelSettings.MAX_CARDS_PER_ROW
            )
            prefs.edit { putInt(KEY_CHANNEL_CARDS_PER_ROW, coercedCount) }
            _channelCardsPerRow.value = coercedCount
        } catch (e: Exception) {
            Timber.e(e, "Failed to set channel cards per row")
        }
    }

    // === Input Settings ===

    fun toggleInputHidden(inputId: String) {
        try {
            val current = _hiddenInputs.value.toMutableSet()
            if (current.contains(inputId)) {
                current.remove(inputId)
            } else {
                current.add(inputId)
            }
            prefs.edit { putStringSet(KEY_HIDDEN_INPUTS, current) }
            _hiddenInputs.value = current
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle input hidden state")
        }
    }

    fun clearHiddenInputs() {
        try {
            prefs.edit { remove(KEY_HIDDEN_INPUTS) }
            _hiddenInputs.value = emptySet()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear hidden inputs")
        }
    }

    // === Animation Settings ===

    fun setEnableAnimations(enable: Boolean) {
        try {
            prefs.edit { putBoolean(KEY_ENABLE_ANIMATIONS, enable) }
            _enableAnimations.value = enable
        } catch (e: Exception) {
            Timber.e(e, "Failed to set enable animations")
        }
    }

    fun toggleEnableAnimations() {
        setEnableAnimations(!_enableAnimations.value)
    }

    fun setAnimAppIcon(enable: Boolean) {
        try {
            prefs.edit { putBoolean(KEY_ANIM_APP_ICON, enable) }
            _animAppIcon.value = enable
        } catch (e: Exception) {
            Timber.e(e, "Failed to set anim app icon")
        }
    }

    fun setAnimChannelRow(enable: Boolean) {
        try {
            prefs.edit { putBoolean(KEY_ANIM_CHANNEL_ROW, enable) }
            _animChannelRow.value = enable
        } catch (e: Exception) {
            Timber.e(e, "Failed to set anim channel row")
        }
    }

    fun setAnimChannelMove(enable: Boolean) {
        try {
            prefs.edit { putBoolean(KEY_ANIM_CHANNEL_MOVE, enable) }
            _animChannelMove.value = enable
        } catch (e: Exception) {
            Timber.e(e, "Failed to set anim channel move")
        }
    }

    fun setAnimAppMove(enable: Boolean) {
        try {
            prefs.edit { putBoolean(KEY_ANIM_APP_MOVE, enable) }
            _animAppMove.value = enable
        } catch (e: Exception) {
            Timber.e(e, "Failed to set anim app move")
        }
    }

    // === Toolbar Settings ===

    fun setToolbarItemsOrder(order: List<String>) {
        try {
            prefs.edit { putStringSet(KEY_TOOLBAR_ITEMS_ORDER, order.toSet()) }
            _toolbarItemsOrder.value = order
        } catch (e: Exception) {
            Timber.e(e, "Failed to set toolbar items order")
        }
    }

    fun toggleToolbarItem(itemName: String) {
        if (itemName == ToolbarItem.LAUNCHER_SETTINGS.name) {
            return
        }
        try {
            val current = _toolbarItemsEnabled.value.toMutableSet()
            if (current.contains(itemName)) {
                current.remove(itemName)
            } else {
                current.add(itemName)
            }
            prefs.edit { putStringSet(KEY_TOOLBAR_ITEMS_ENABLED, current) }
            _toolbarItemsEnabled.value = current
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle toolbar item")
        }
    }

    fun setToolbarItemEnabled(itemName: String, enabled: Boolean) {
        if (itemName == ToolbarItem.LAUNCHER_SETTINGS.name && !enabled) {
            return
        }
        try {
            val current = _toolbarItemsEnabled.value.toMutableSet()
            if (enabled) {
                current.add(itemName)
            } else {
                current.remove(itemName)
            }
            prefs.edit { putStringSet(KEY_TOOLBAR_ITEMS_ENABLED, current) }
            _toolbarItemsEnabled.value = current
        } catch (e: Exception) {
            Timber.e(e, "Failed to set toolbar item enabled")
        }
    }

    // === Launcher Suppression Settings ===

    fun setSuppressOriginalLauncher(suppress: Boolean) {
        try {
            prefs.edit { putBoolean(KEY_SUPPRESS_ORIGINAL_LAUNCHER, suppress) }
            _suppressOriginalLauncher.value = suppress
        } catch (e: Exception) {
            Timber.e(e, "Failed to set suppress original launcher")
        }
    }

    fun toggleSuppressOriginalLauncher() {
        setSuppressOriginalLauncher(!_suppressOriginalLauncher.value)
    }

    fun setSuppressLauncherOnlyExternal(onlyExternal: Boolean) {
        try {
            prefs.edit { putBoolean(KEY_SUPPRESS_LAUNCHER_ONLY_EXTERNAL, onlyExternal) }
            _suppressLauncherOnlyExternal.value = onlyExternal
        } catch (e: Exception) {
            Timber.e(e, "Failed to set suppress launcher only external")
        }
    }

    fun toggleSuppressLauncherOnlyExternal() {
        setSuppressLauncherOnlyExternal(!_suppressLauncherOnlyExternal.value)
    }

    // === Developer Settings ===

    fun setDeveloperMode(enabled: Boolean) {
        try {
            prefs.edit { putBoolean(KEY_DEVELOPER_MODE, enabled) }
            _developerMode.value = enabled
        } catch (e: Exception) {
            Timber.e(e, "Failed to set developer mode")
        }
    }

    fun toggleDeveloperMode() {
        setDeveloperMode(!_developerMode.value)
    }

    // === Background Settings ===

    fun setBackgroundColor(color: Long) {
        try {
            prefs.edit { putLong(KEY_BACKGROUND_COLOR, color) }
            _backgroundColor.value = color
        } catch (e: Exception) {
            Timber.e(e, "Failed to set background color")
        }
    }

    fun setBackgroundImageUri(uri: String?) {
        try {
            if (uri == null) {
                prefs.edit { remove(KEY_BACKGROUND_IMAGE_URI) }
            } else {
                prefs.edit { putString(KEY_BACKGROUND_IMAGE_URI, uri) }
            }
            _backgroundImageUri.value = uri
        } catch (e: Exception) {
            Timber.e(e, "Failed to set background image uri")
        }
    }

    fun resetBackground() {
        setBackgroundColor(LauncherConstants.Background.DEFAULT_BACKGROUND_COLOR)
        setBackgroundImageUri(null)
    }

    // === Reset Operations ===

    fun resetSettings() {
        try {
            prefs.edit { clear() }
            _appCardSize.value = LauncherConstants.CardSize.DEFAULT_APP_CARD_SIZE
            _channelCardSize.value = LauncherConstants.CardSize.DEFAULT_CHANNEL_CARD_SIZE
            _channelCardsPerRow.value = LauncherConstants.ChannelSettings.DEFAULT_CARDS_PER_ROW
            _hiddenInputs.value = emptySet()
            _showMobileApps.value = false

            // Reset animations
            _enableAnimations.value = LauncherConstants.Animations.DEFAULT_ENABLED
            _animAppIcon.value = LauncherConstants.Animations.DEFAULT_ENABLED
            _animChannelRow.value = LauncherConstants.Animations.DEFAULT_ENABLED
            _animChannelMove.value = LauncherConstants.Animations.DEFAULT_ENABLED
            _animAppMove.value = LauncherConstants.Animations.DEFAULT_ENABLED

            // Reset toolbar placement
            _toolbarItemsOrder.value = DEFAULT_TOOLBAR_ITEMS_ORDER
            _toolbarItemsEnabled.value = DEFAULT_TOOLBAR_ITEMS_ENABLED

            // Reset launcher suppression
            _suppressOriginalLauncher.value = false
            _suppressLauncherOnlyExternal.value = false

            // Reset developer mode
            _developerMode.value = false

            // Reset background
            _backgroundColor.value = LauncherConstants.Background.DEFAULT_BACKGROUND_COLOR
            _backgroundImageUri.value = null
        } catch (e: Exception) {
            Timber.e(e, "Failed to reset settings")
        }
    }
}