package dev.mudrock.tiviyomitvlauncher.ui.toolbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mudrock.tiviyomitvlauncher.data.repository.SettingsRepository
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    onOpenLauncherSettings: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val settingsRepository = koinInject<SettingsRepository>()

    val toolbarItemsOrder by settingsRepository.toolbarItemsOrder.collectAsStateWithLifecycle()
    val toolbarItemsEnabled by settingsRepository.toolbarItemsEnabled.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRestorer(focusRequester)
            .then(modifier),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarTabs(
            modifier = Modifier
                .focusRequester(focusRequester)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Render all toolbar items (including clock) based on order and enabled state
        toolbarItemsOrder.forEach { itemName ->
            if (toolbarItemsEnabled.contains(itemName)) {
                when (SettingsRepository.Companion.ToolbarItem.valueOf(itemName)) {
                    SettingsRepository.Companion.ToolbarItem.CLOCK -> ToolbarClock()
                    SettingsRepository.Companion.ToolbarItem.INPUTS -> ToolbarInputsButton()
                    SettingsRepository.Companion.ToolbarItem.NOTIFICATIONS -> ToolbarNotificationsButton()
                    SettingsRepository.Companion.ToolbarItem.WIFI -> ToolbarWifiButton()
                    SettingsRepository.Companion.ToolbarItem.REFRESH -> ToolbarRefreshButton()
                    SettingsRepository.Companion.ToolbarItem.LAUNCHER_SETTINGS -> ToolbarLauncherSettingsButton(onClick = onOpenLauncherSettings)
                    SettingsRepository.Companion.ToolbarItem.SETTINGS -> ToolbarSettingsButton()
                }
            }
        }
    }
}
