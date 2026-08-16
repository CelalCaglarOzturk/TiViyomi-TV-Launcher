package dev.mudrock.tiviyomitvlauncher.ui.tab.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.StandardCardContainer
import androidx.tv.material3.Text
import dev.mudrock.tiviyomitvlauncher.R
import dev.mudrock.tiviyomitvlauncher.data.model.ChannelType
import dev.mudrock.tiviyomitvlauncher.data.repository.SettingsRepository
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.Channel
import dev.mudrock.tiviyomitvlauncher.ui.tab.home.row.AppCardRow
import dev.mudrock.tiviyomitvlauncher.ui.tab.home.row.ChannelProgramCardRow
import dev.mudrock.tiviyomitvlauncher.util.FocusController
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeTab(
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    val viewModel = koinViewModel<HomeTabViewModel>()
    val focusController = koinInject<FocusController>()
    val settingsRepository = koinInject<SettingsRepository>()

    val enableAnimations by settingsRepository.enableAnimations.collectAsStateWithLifecycle()
    val animChannelMove by settingsRepository.animChannelMove.collectAsStateWithLifecycle()
    val areMoveAnimationsEnabled = enableAnimations && animChannelMove

    // Use lifecycle-aware collection for better performance
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val appsMap by viewModel.appsMap.collectAsStateWithLifecycle()
    val allAppsMap by viewModel.allAppsMap.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val channelRows by viewModel.channelRows.collectAsStateWithLifecycle()
    val allAppChannels by viewModel.allAppChannels.collectAsStateWithLifecycle()
    val appCardSize by viewModel.appCardSize.collectAsStateWithLifecycle()
    val channelCardSize by viewModel.channelCardSize.collectAsStateWithLifecycle()

    // Use derivedStateOf for filtered lists to avoid unnecessary recompositions
    // These are keyed on the source data to prevent recalculation on every recomposition
    val enabledChannels by remember {
        derivedStateOf {
            channels.filter { it.enabled }
        }
    }

    val disabledChannels by remember {
        derivedStateOf {
            allAppChannels.filter { !it.enabled }
        }
    }

    val hasDisabledChannels by remember {
        derivedStateOf { disabledChannels.isNotEmpty() }
    }

    val hasAppChannels by remember {
        derivedStateOf {
            enabledChannels.any { it.type != ChannelType.WATCH_NEXT }
        }
    }

    // Use rememberSaveable to preserve scroll position across tab switches
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Stable focus requester that persists across recompositions
    val firstItemFocusRequester = remember { FocusRequester() }

    // Track which channel should receive focus after recomposition
    var focusedChannelId by remember { mutableStateOf<String?>(null) }

    // Map for focus requesters
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    // Restore focus to the channel that was being toggled
    LaunchedEffect(focusedChannelId, enabledChannels, disabledChannels) {
        focusedChannelId?.let { id ->
            // Scroll to the item to ensure it's composed and can receive focus
            val enabledIndex = enabledChannels.indexOfFirst { it.id == id }
            if (enabledIndex != -1) {
                // +1 for Apps row
                listState.scrollToItem(enabledIndex + 1)
            } else if (disabledChannels.any { it.id == id }) {
                // +1 for Apps row, + enabledChannels.size
                listState.scrollToItem(enabledChannels.size + 1)
            }
        }
    }

    // Clean up focus requesters
    LaunchedEffect(channels, allAppChannels) {
        val currentIds = allAppChannels.map { it.id }.toSet()
        focusRequesters.keys.retainAll(currentIds)
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            focusController.focusReset.collect {
                listState.scrollToItem(0)
                try {
                    firstItemFocusRequester.requestFocus()
                } catch (e: Exception) {
                    // Ignore focus request failures
                }
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides dev.mudrock.tiviyomitvlauncher.ui.util.NuvioScrollDefaults.smoothScrollSpec
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = modifier
                .fillMaxSize()
                .focusProperties {
                    onEnter = {
                        if (requestedFocusDirection == FocusDirection.Down) {
                            firstItemFocusRequester
                        } else {
                            FocusRequester.Default
                        }
                    }
                }
                .focusRestorer(firstItemFocusRequester)
        ) {
            item(
                key = "apps",
                contentType = "apps_row"
            ) {
                AppCardRow(
                    apps = apps,
                    baseHeight = appCardSize.dp,
                    firstItemFocusRequester = firstItemFocusRequester
                )
            }

        itemsIndexed(
            items = channelRows,
            key = { _, rowData -> rowData.channel.id },
            contentType = { _, _ -> "channel_row" }
        ) { index, rowData ->
            val channel = rowData.channel
            val isWatchNext = rowData.isWatchNext
            val displayTitle = if (isWatchNext) {
                stringResource(R.string.channel_watch_next)
            } else {
                stringResource(R.string.channel_preview, rowData.titlePair!!.first, rowData.titlePair.second)
            }

            val focusRequester = remember(channel.id) {
                focusRequesters.getOrPut(channel.id) { FocusRequester() }
            }

            LaunchedEffect(focusedChannelId) {
                if (focusedChannelId == channel.id) {
                    try {
                        focusRequester.requestFocus()
                        focusedChannelId = null
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }

            val onToggleEnabled = remember(channel, viewModel) {
                { enabled: Boolean ->
                    focusedChannelId = channel.id
                    viewModel.setChannelEnabled(channel, enabled)
                    Unit
                }
            }

            val onMoveUp = remember(channel, viewModel) {
                {
                    viewModel.moveChannelUp(channel)
                    Unit
                }
            }

            val onMoveDown = remember(channel, viewModel) {
                {
                    viewModel.moveChannelDown(channel)
                    Unit
                }
            }

            val onRemoveProgram: ((dev.mudrock.tiviyomitvlauncher.data.sqldelight.ChannelProgram) -> Unit)? = if (isWatchNext) {
                remember(viewModel) {
                    { program ->
                        viewModel.removeWatchNextProgram(program.id)
                        Unit
                    }
                }
            } else null

            ChannelProgramCardRow(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .let {
                        if (index == 0 && apps.isEmpty()) it.focusRequester(
                            firstItemFocusRequester
                        ) else it
                    }
                    .focusGroup()
                    .then(if (areMoveAnimationsEnabled) Modifier.animateItem() else Modifier),
                title = displayTitle,
                programs = rowData.programs,
                channel = channel,
                baseHeight = channelCardSize.dp,
                onToggleEnabled = onToggleEnabled,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onRemoveProgram = onRemoveProgram
            )
        }

        // Show placeholder when there are no app channels (Watch Next won't exist either)
        if (!hasAppChannels) {
            item(
                key = "no_channels_placeholder",
                contentType = "no_channels_placeholder"
            ) {
                Text(
                    text = stringResource(R.string.channel_no_channels),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 48.dp)
                )
            }
        }

        // Show disabled channels section if there are any
        if (hasDisabledChannels) {
            item(
                key = "disabled_channels_header",
                contentType = "disabled_channels_section"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.disabled_channels),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            vertical = 4.dp,
                            horizontal = 48.dp,
                        )
                    )

                    LazyRow(
                        contentPadding = PaddingValues(
                            vertical = 16.dp,
                            horizontal = 48.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(
                            items = disabledChannels,
                            key = { _, channel -> "disabled_${channel.id}" }
                        ) { index, channel ->
                            val app = remember(channel.packageName, allAppsMap) {
                                allAppsMap[channel.packageName]
                            }

                            val focusRequester = remember(channel.id) {
                                focusRequesters.getOrPut(channel.id) { FocusRequester() }
                            }

                            LaunchedEffect(focusedChannelId) {
                                if (focusedChannelId == channel.id) {
                                    try {
                                        focusRequester.requestFocus()
                                        focusedChannelId = null
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            }

                            DisabledChannelCard(
                                modifier = Modifier.focusRequester(focusRequester),
                                channel = channel,
                                appName = app?.displayName ?: channel.packageName,
                                onEnable = {
                                    focusedChannelId = channel.id
                                    viewModel.enableChannel(channel)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun DisabledChannelCard(
    channel: Channel,
    appName: String,
    onEnable: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onEnable,
        modifier = modifier.width(200.dp),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
            )
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = channel.displayName,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = appName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.channel_enable),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.channel_enable),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


