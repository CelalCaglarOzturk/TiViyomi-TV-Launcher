package dev.mudrock.tiviyomitvlauncher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import dev.mudrock.tiviyomitvlauncher.R
import dev.mudrock.tiviyomitvlauncher.data.repository.AppRepository
import dev.mudrock.tiviyomitvlauncher.data.repository.ChannelRepository
import org.koin.compose.koinInject

@Composable
fun WatchNextSettingsDialog(
    onDismissRequest: () -> Unit
) {
    val appRepository = koinInject<AppRepository>()
    val channelRepository = koinInject<ChannelRepository>()
    val scope = rememberCoroutineScope()

    val apps by combine(
        appRepository.getApps(),
        channelRepository.getChannels()
    ) { allApps, allChannels ->
        val channelProviders = allChannels.map { it.packageName }.toSet()

        allApps.filter { app ->
            channelProviders.contains(app.packageName)
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val blacklist by channelRepository.getWatchNextBlacklist().collectAsStateWithLifecycle(initialValue = emptyList())

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .fillMaxHeight(0.8f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.channel_watch_next),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (apps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.watch_next_no_apps),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn {
                        items(apps) { app ->
                            val isBlacklisted = blacklist.contains(app.packageName)
                            val isEnabled = !isBlacklisted

                            ListItem(
                                selected = false,
                                onClick = {
                                    scope.launch {
                                        if (isEnabled) {
                                            channelRepository.addToWatchNextBlacklist(app.packageName)
                                        } else {
                                            channelRepository.removeFromWatchNextBlacklist(app.packageName)
                                        }
                                    }
                                },
                                headlineContent = { Text(app.displayName) },
                                trailingContent = {
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
