package dev.mudrock.tiviyomitvlauncher.ui.tab.home.row

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.mudrock.tiviyomitvlauncher.R
import dev.mudrock.tiviyomitvlauncher.data.repository.SettingsRepository
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.App
import dev.mudrock.tiviyomitvlauncher.ui.component.card.MoveableAppCard
import dev.mudrock.tiviyomitvlauncher.ui.tab.home.HomeTabViewModel
import dev.mudrock.tiviyomitvlauncher.util.MoveDirection
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun AppCardRow(
    apps: List<App>,
    modifier: Modifier = Modifier,
    baseHeight: Dp = 90.dp,
    firstItemFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    val viewModel = koinViewModel<HomeTabViewModel>()
    val settingsRepository = koinInject<SettingsRepository>()

    val enableAnimations by settingsRepository.enableAnimations.collectAsState(initial = true)
    val animAppMove by settingsRepository.animAppMove.collectAsState(initial = true)
    val animAppIcon by settingsRepository.animAppIcon.collectAsState(initial = true)
    val areAppMoveAnimationsEnabled = enableAnimations && animAppMove
    val areAnimationsEnabled = enableAnimations && animAppIcon

    // Track which app is in move mode (only one at a time)
    var moveAppId by remember { mutableStateOf<String?>(null) }

    // Track which app should receive focus after recomposition
    var focusedAppId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    rememberCoroutineScope()

    // Create a map of focus requesters for each app
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    // Restore focus to the app that was being moved after list recomposes
    LaunchedEffect(apps, focusedAppId) {
        if (focusedAppId != null) {
            val focusRequester = focusRequesters[focusedAppId]

            // Ensure the app is visible before requesting focus
            val index = apps.indexOfFirst { it.id == focusedAppId }
            if (index != -1) {
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val itemInfo = visibleItems.find { it.index == index }

                // Scroll if item is not visible or partially hidden on the left
                if (itemInfo == null || itemInfo.offset < layoutInfo.viewportStartOffset) {
                    listState.scrollToItem(index)
                }
            }

            if (focusRequester != null) {
                try {
                    focusRequester.requestFocus()
                    focusedAppId = null
                } catch (e: Exception) {
                    // Ignore focus request failures
                }
            }
        }
    }

    if (apps.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.empty_no_apps_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_no_apps_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        return
    }

    CardRow(
        modifier = modifier.focusProperties {
            onEnter = {
                if (requestedFocusDirection == FocusDirection.Down) {
                    firstItemFocusRequester
                } else {
                    FocusRequester.Default
                }
            }
        },
        state = listState,
        baseHeight = baseHeight,
    ) { childFocusRequester ->
        itemsIndexed(
            items = apps,
            key = { _, app -> app.id },
        ) { index, app ->
            val isInMoveMode = moveAppId == app.id

            // Get or create a focus requester for this app
            val appFocusRequester = remember(app.id) {
                focusRequesters.getOrPut(app.id) { FocusRequester() }
            }

            Box(modifier = if (areAppMoveAnimationsEnabled && isInMoveMode) Modifier.animateItem() else Modifier) {
                MoveableAppCard(
                    app = app,
                    baseHeight = baseHeight,
                    areAnimationsEnabled = areAnimationsEnabled,
                    modifier = Modifier
                        .focusRequester(appFocusRequester)
                        .then(
                            if (index == 0) {
                                Modifier
                                    .focusRequester(childFocusRequester)
                                    .focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            }
                        ),
                    isInMoveMode = isInMoveMode,
                    isFavorite = app.favoriteOrder != null,
                    onMoveModeChanged = { inMoveMode ->
                        moveAppId = if (inMoveMode) app.id else null
                        // Track focused app when entering move mode
                        if (inMoveMode) {
                            focusedAppId = app.id
                        }
                    },
                    onMove = { direction ->
                        when (direction) {
                            MoveDirection.LEFT -> {
                                if (index > 0) {
                                    // Track the app to restore focus
                                    focusedAppId = app.id
                                    viewModel.setFavoriteOrder(app, index - 1)
                                }
                            }

                            MoveDirection.RIGHT -> {
                                if (index < apps.size - 1) {
                                    // Track the app to restore focus
                                    focusedAppId = app.id
                                    viewModel.setFavoriteOrder(app, index + 1)
                                }
                            }

                            MoveDirection.UP, MoveDirection.DOWN -> {
                                // UP and DOWN don't apply to horizontal row
                            }
                        }
                    },
                    onToggleFavorite = { favorite ->
                        if (!favorite) {
                            // When removing an app, focus the next one (or previous if last)
                            val nextApp = if (index < apps.size - 1) {
                                apps[index + 1]
                            } else if (index > 0) {
                                apps[index - 1]
                            } else {
                                null
                            }

                            if (nextApp != null) {
                                focusedAppId = nextApp.id
                            }
                        }
                        viewModel.favoriteApp(app, favorite)
                    }
                )
            }
        }
    }

    // Clean up focus requesters for apps that are no longer in the list
    LaunchedEffect(apps) {
        val currentAppIds = apps.map { it.id }.toSet()
        focusRequesters.keys.removeAll { it !in currentAppIds }
    }
}
