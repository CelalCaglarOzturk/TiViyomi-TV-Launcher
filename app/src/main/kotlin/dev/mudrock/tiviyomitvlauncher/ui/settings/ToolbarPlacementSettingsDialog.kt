package dev.mudrock.tiviyomitvlauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.mudrock.tiviyomitvlauncher.R
import dev.mudrock.tiviyomitvlauncher.data.repository.SettingsRepository
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ToolbarPlacementSettingsDialog(
    onDismissRequest: () -> Unit
) {
    val settingsRepository = koinInject<SettingsRepository>()
    val toolbarItemsOrder by settingsRepository.toolbarItemsOrder.collectAsStateWithLifecycle()
    val toolbarItemsEnabled by settingsRepository.toolbarItemsEnabled.collectAsStateWithLifecycle()

    // Track which item is currently being moved (by its index)
    var movingItemIndex by remember { mutableIntStateOf(-1) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.width(500.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_toolbar_placement),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Single list of all toolbar items
                toolbarItemsOrder.forEachIndexed { index, itemName ->
                    val item = ToolbarItem.valueOf(itemName)
                    val isEnabled = toolbarItemsEnabled.contains(itemName)
                    val isLauncherSettings = item == ToolbarItem.LAUNCHER_SETTINGS
                    val isMoving = movingItemIndex == index

                    ToolbarItemRow(
                        item = item,
                        isEnabled = isEnabled,
                        isMoving = isMoving,
                        isLauncherSettings = isLauncherSettings,
                        onToggleEnabled = {
                            if (!isLauncherSettings) {
                                settingsRepository.toggleToolbarItem(itemName)
                            }
                        },
                        onStartMoving = {
                            movingItemIndex = index
                        },
                        onStopMoving = {
                            movingItemIndex = -1
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val newOrder = toolbarItemsOrder.toMutableList()
                                val temp = newOrder[index]
                                newOrder[index] = newOrder[index - 1]
                                newOrder[index - 1] = temp
                                settingsRepository.setToolbarItemsOrder(newOrder)
                                movingItemIndex = index - 1
                            }
                        },
                        onMoveDown = {
                            if (index < toolbarItemsOrder.size - 1) {
                                val newOrder = toolbarItemsOrder.toMutableList()
                                val temp = newOrder[index]
                                newOrder[index] = newOrder[index + 1]
                                newOrder[index + 1] = temp
                                settingsRepository.setToolbarItemsOrder(newOrder)
                                movingItemIndex = index + 1
                            }
                        }
                    )
                }
            }
        }
    }
}

private enum class ToolbarItem {
    CLOCK, INPUTS, NOTIFICATIONS, WIFI, REFRESH, LAUNCHER_SETTINGS, SETTINGS
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ToolbarItemRow(
    item: ToolbarItem,
    isEnabled: Boolean,
    isMoving: Boolean,
    isLauncherSettings: Boolean,
    onToggleEnabled: () -> Unit,
    onStartMoving: () -> Unit,
    onStopMoving: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    // Restore focus when this item becomes the moving item
    LaunchedEffect(isMoving) {
        if (isMoving) {
            focusRequester.requestFocus()
        }
    }

    Card(
        onClick = {
            if (isMoving) {
                onStopMoving()
            } else if (!isLauncherSettings) {
                // Short press toggles the item (when not launcher settings)
                onToggleEnabled()
            }
        },
        onLongClick = {
            if (!isMoving) {
                onStartMoving()
            }
        },
        shape = CardDefaults.shape(shape = MaterialTheme.shapes.small),
        colors = CardDefaults.colors(
            containerColor = if (isMoving) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            pressedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
        ),
        scale = CardDefaults.scale(focusedScale = 1.0f),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                if (isMoving && keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionUp -> {
                            onMoveUp()
                            true
                        }
                        Key.DirectionDown -> {
                            onMoveDown()
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            onStopMoving()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Item name (with moving indicator)
            Text(
                text = if (isMoving) {
                    "${getItemDisplayName(item)} (moving)"
                } else {
                    getItemDisplayName(item)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMoving) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Enable/disable indicator - Card handles the actual toggling
            if (isLauncherSettings) {
                // Launcher settings always enabled
                Text(
                    text = "ON",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                // Show ON/OFF status (toggle is handled by Card onClick)
                Text(
                    text = if (isEnabled) "ON" else "OFF",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
            }
        }
    }
}

@Composable
private fun getItemDisplayName(item: ToolbarItem): String {
    return when (item) {
        ToolbarItem.CLOCK -> stringResource(R.string.toolbar_clock)
        ToolbarItem.INPUTS -> stringResource(R.string.toolbar_inputs)
        ToolbarItem.NOTIFICATIONS -> stringResource(R.string.toolbar_notifications)
        ToolbarItem.WIFI -> stringResource(R.string.toolbar_wifi)
        ToolbarItem.REFRESH -> stringResource(R.string.toolbar_refresh)
        ToolbarItem.LAUNCHER_SETTINGS -> stringResource(R.string.toolbar_launcher_settings)
        ToolbarItem.SETTINGS -> stringResource(R.string.toolbar_settings)
    }
}
