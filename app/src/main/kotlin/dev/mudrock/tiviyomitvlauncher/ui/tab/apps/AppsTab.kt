package dev.mudrock.tiviyomitvlauncher.ui.tab.apps

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.mudrock.tiviyomitvlauncher.R
import dev.mudrock.tiviyomitvlauncher.data.repository.SettingsRepository
import dev.mudrock.tiviyomitvlauncher.ui.component.card.AppCard
import dev.mudrock.tiviyomitvlauncher.ui.component.card.MoveableAppCard
import dev.mudrock.tiviyomitvlauncher.util.FocusController
import dev.mudrock.tiviyomitvlauncher.util.MoveDirection
import dev.mudrock.tiviyomitvlauncher.ui.util.dpadRepeatThrottle
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun AppsTab(
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    val viewModel = koinViewModel<AppsTabViewModel>()
    val focusController = koinInject<FocusController>()
    val settingsRepository = koinInject<SettingsRepository>()

    val enableAnimations by settingsRepository.enableAnimations.collectAsStateWithLifecycle()
    val animAppMove by settingsRepository.animAppMove.collectAsStateWithLifecycle()
    val animAppIcon by settingsRepository.animAppIcon.collectAsStateWithLifecycle()
    val areAppMoveAnimationsEnabled = enableAnimations && animAppMove
    val areAnimationsEnabled = enableAnimations && animAppIcon

    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val hiddenApps by viewModel.hiddenApps.collectAsStateWithLifecycle()
    val appCardSize by viewModel.appCardSize.collectAsStateWithLifecycle()

    val hasApps by remember { derivedStateOf { apps.isNotEmpty() } }
    val hasHiddenApps by remember { derivedStateOf { hiddenApps.isNotEmpty() } }

    var moveAppId by remember { mutableStateOf<String?>(null) }
    var focusedAppId by remember { mutableStateOf<String?>(null) }

    val firstItemFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyGridState()

    // Stable map of focus requesters
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    // Restore focus logic
    LaunchedEffect(focusedAppId, apps, hiddenApps) {
        focusedAppId?.let { id ->
            val index = apps.indexOfFirst { it.id == id }
            if (index != -1) {
                listState.scrollToItem(index)
            } else {
                val hiddenIndex = hiddenApps.indexOfFirst { it.id == id }
                if (hiddenIndex != -1) {
                    listState.scrollToItem(apps.size + 1 + hiddenIndex)
                }
            }
        }
    }

    // Reset focus on tab activation
    LaunchedEffect(isActive) {
        if (isActive) {
            focusController.focusReset.collect {
                listState.scrollToItem(0)
                try {
                    firstItemFocusRequester.requestFocus()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } else {
            focusedAppId = null
        }
    }

    // Cleanup focus requesters
    LaunchedEffect(apps, hiddenApps) {
        val currentAppIds = (apps + hiddenApps).map { it.id }.toSet()
        focusRequesters.keys.retainAll(currentAppIds)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current

        val itemMinWidth = remember(appCardSize) { appCardSize.dp * (16f / 9f) }
        val spacing = remember { 14.dp }
        val horizontalPadding = remember { 96.dp }

        val columnCount = remember(maxWidth, itemMinWidth, spacing, horizontalPadding) {
            val availableWidth = maxWidth - horizontalPadding
            val itemWidthPx = with(density) { itemMinWidth.toPx() }
            val spacingPx = with(density) { spacing.toPx() }
            val availableWidthPx = with(density) { availableWidth.toPx() }

            ((availableWidthPx + spacingPx) / (itemWidthPx + spacingPx)).toInt().coerceAtLeast(1)
        }

        val gridCells = remember(columnCount) { GridCells.Fixed(columnCount) }
        val gridWidth = remember(columnCount, itemMinWidth, spacing, horizontalPadding) {
            (itemMinWidth * columnCount) + (spacing * (columnCount - 1)) + horizontalPadding
        }

        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides dev.mudrock.tiviyomitvlauncher.ui.util.NuvioScrollDefaults.smoothScrollSpec
        ) {
            LazyVerticalGrid(
                state = listState,
                contentPadding = PaddingValues(
                    vertical = 4.dp,
                    horizontal = 48.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                columns = gridCells,
                modifier = Modifier
                    .width(gridWidth)
                    .fillMaxHeight()
                    .dpadRepeatThrottle(horizontalGateMs = 70L, verticalGateMs = 100L)
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
            // Visible Apps
            if (hasApps) {
                itemsIndexed(
                    items = apps,
                    key = { _, app -> app.id },
                    contentType = { _, _ -> "app_card" }
                ) { index, app ->
                    val isInMoveMode = moveAppId == app.id
                    val isFavorite = remember(app.favoriteOrder) { app.favoriteOrder != null }

                    val appFocusRequester = remember(app.id) {
                        focusRequesters.getOrPut(app.id) { FocusRequester() }
                    }

                    LaunchedEffect(focusedAppId) {
                        if (focusedAppId == app.id) {
                            try {
                                appFocusRequester.requestFocus()
                                if (!isInMoveMode) focusedAppId = null
                            } catch (e: Exception) {
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .run { if (areAppMoveAnimationsEnabled && isInMoveMode) animateItem() else this }
                            .zIndex(if (isInMoveMode) 1f else 0f)
                    ) {
                        MoveableAppCard(
                            app = app,
                            baseHeight = appCardSize.dp,
                            areAnimationsEnabled = areAnimationsEnabled,
                            modifier = Modifier
                                .focusRequester(appFocusRequester)
                                .then(
                                    if (index == 0) Modifier.focusRequester(firstItemFocusRequester)
                                    else Modifier
                                )
                                .focusGroup(),
                            isInMoveMode = isInMoveMode,
                            isFavorite = isFavorite,
                            onMoveModeChanged = { inMoveMode ->
                                moveAppId = if (inMoveMode) app.id else null
                                if (inMoveMode) focusedAppId = app.id
                            },
                            onMove = { direction ->
                                var targetIndex = -1
                                when (direction) {
                                    MoveDirection.LEFT -> if (index > 0) targetIndex = index - 1
                                    MoveDirection.RIGHT -> if (index < apps.size - 1) targetIndex = index + 1
                                    MoveDirection.UP -> if (index >= columnCount) targetIndex = index - columnCount
                                    MoveDirection.DOWN -> if (index + columnCount < apps.size) targetIndex =
                                        index + columnCount
                                }

                                if (targetIndex != -1) {
                                    focusedAppId = app.id
                                    val targetApp = apps[targetIndex]
                                    val targetOrder = targetApp.allAppsOrder?.toInt() ?: targetIndex
                                    viewModel.moveApp(app, targetOrder)
                                }
                            },
                            onToggleFavorite = { favorite -> viewModel.favoriteApp(app, favorite) },
                            onToggleHidden = { _ -> viewModel.hideApp(app) },
                            onClick = null
                        )
                    }
                }
            }

            // Hidden Apps Header
            if (hasHiddenApps) {
                item(
                    key = "hidden_apps_header",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "section_header"
                ) {
                    Text(
                        text = stringResource(R.string.apps_hidden_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )
                }

                itemsIndexed(
                    items = hiddenApps,
                    key = { _, app -> "hidden_${app.id}" },
                    contentType = { _, _ -> "app_card" }
                ) { index, app ->
                    val isFavorite = remember(app.favoriteOrder) { app.favoriteOrder != null }

                    val appFocusRequester = remember(app.id) {
                        focusRequesters.getOrPut(app.id) { FocusRequester() }
                    }

                    LaunchedEffect(focusedAppId) {
                        if (focusedAppId == app.id) {
                            try {
                                appFocusRequester.requestFocus()
                                focusedAppId = null
                            } catch (e: Exception) {
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.run { if (areAppMoveAnimationsEnabled && moveAppId == app.id) animateItem() else this }
                    ) {
                        AppCard(
                            app = app,
                            baseHeight = appCardSize.dp,
                            areAnimationsEnabled = areAnimationsEnabled,
                            modifier = Modifier
                                .focusRequester(appFocusRequester)
                                .then(
                                    if (!hasApps && index == 0) Modifier.focusRequester(firstItemFocusRequester)
                                    else Modifier
                                )
                                .focusGroup(),
                            popupContent = {
                                AppPopup(
                                    isFavorite = isFavorite,
                                    isHidden = true,
                                    packageName = app.packageName,
                                    onToggleFavorite = { favorite -> viewModel.favoriteApp(app, favorite) },
                                    onToggleHidden = {
                                        focusedAppId = app.id
                                        viewModel.unhideApp(app)
                                    }
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
