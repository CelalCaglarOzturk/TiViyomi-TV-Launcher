package dev.mudrock.tiviyomitvlauncher.ui.tab.home.row

import android.view.KeyEvent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import dev.mudrock.tiviyomitvlauncher.data.repository.SettingsRepository
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.Channel
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.ChannelProgram
import dev.mudrock.tiviyomitvlauncher.ui.component.PopupContainer
import dev.mudrock.tiviyomitvlauncher.ui.component.card.ChannelProgramCard
import dev.mudrock.tiviyomitvlauncher.ui.tab.home.ChannelPopup
import dev.mudrock.tiviyomitvlauncher.ui.tab.home.WatchNextProgramPopup
import org.koin.compose.koinInject

import dev.mudrock.tiviyomitvlauncher.ui.util.dpadRepeatThrottle

private const val LONG_PRESS_DELAY_MS = 300L

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChannelProgramCardRow(
    modifier: Modifier = Modifier,
    title: String,
    programs: List<ChannelProgram>,
    channel: Channel? = null,
    baseHeight: Dp = 90.dp,
    state: LazyListState = rememberLazyListState(),
    onToggleEnabled: ((enabled: Boolean) -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemoveProgram: ((program: ChannelProgram) -> Unit)? = null,
) {
    val settingsRepository = koinInject<SettingsRepository>()
    val enableAnimations by settingsRepository.enableAnimations.collectAsStateWithLifecycle(initialValue = true)
    val animChannelRow by settingsRepository.animChannelRow.collectAsStateWithLifecycle(initialValue = true)
    val channelCardsPerRow by settingsRepository.channelCardsPerRow.collectAsStateWithLifecycle()
    val areRowAnimationsEnabled = enableAnimations && animChannelRow

    // Limit programs to configured number for performance
    val limitedPrograms = remember(programs, channelCardsPerRow) {
        programs.take(channelCardsPerRow)
    }

    var popupVisible by remember { mutableStateOf(false) }
    var isInMoveMode by remember { mutableStateOf(false) }
    var ignoreNextKeyUp by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // State for watch next program popup
    var watchNextPopupProgram by remember { mutableStateOf<ChannelProgram?>(null) }

    // Track which program should receive focus after recomposition
    var focusedProgramId by remember { mutableStateOf<String?>(null) }

    // Restore focus to the program that was being removed/moved after list recomposes
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    LaunchedEffect(focusedProgramId, programs) {
        if (focusedProgramId != null) {
            val focusRequester = focusRequesters[focusedProgramId]
            if (focusRequester != null) {
                try {
                    focusRequester.requestFocus()
                    focusedProgramId = null
                } catch (e: Exception) {
                    // Ignore focus request failures
                }
            }
        }
    }

    // Clean up focus requesters for programs that are no longer in the list
    LaunchedEffect(programs) {
        val currentProgramIds = programs.map { it.id }.toSet()
        focusRequesters.keys.retainAll(currentProgramIds)
    }

    Column(
        modifier = modifier
    ) {
        // Row title (non-focusable, just a label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp,
                    horizontal = 48.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                color = if (isInMoveMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }

        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides dev.mudrock.tiviyomitvlauncher.ui.util.TvScrollDefaults.smoothScrollSpec
        ) {
            LazyRow(
                state = state,
                contentPadding = PaddingValues(
                    vertical = 4.dp,
                    horizontal = 48.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = baseHeight + 8.dp)
                    .dpadRepeatThrottle(horizontalGateMs = 70L),
            ) {
                itemsIndexed(
                    items = limitedPrograms,
                    key = { _, program -> program.id },
                    contentType = { _, _ -> "program_card" }
                ) { index, program ->
                    // Get or create a focus requester for this program
                    val programFocusRequester = remember(program.id) {
                        focusRequesters.getOrPut(program.id) { FocusRequester() }
                    }

                    Box {
                        var isKeyHeld by remember { mutableStateOf(false) }
                        var isLongPressTriggered by remember { mutableStateOf(false) }

                        LaunchedEffect(isKeyHeld) {
                            if (isKeyHeld) {
                                delay(LONG_PRESS_DELAY_MS)
                                if (isKeyHeld) {
                                    isLongPressTriggered = true
                                    if (onRemoveProgram != null) {
                                        watchNextPopupProgram = program
                                    } else if (channel != null && onToggleEnabled != null) {
                                        popupVisible = true
                                    }
                                }
                            }
                        }

                        ChannelProgramCard(
                            program = program,
                            baseHeight = baseHeight,
                            isMoving = isInMoveMode,
                            enableAnimations = areRowAnimationsEnabled,
                            modifier = Modifier
                                .focusRequester(programFocusRequester)
                                .onPreviewKeyEvent { event ->
                                    // Consume KeyUp events when ignoreNextKeyUp is set (after exiting move mode)
                                    if (ignoreNextKeyUp && event.type == KeyEventType.KeyUp) {
                                        when (event.key.nativeKeyCode) {
                                            KeyEvent.KEYCODE_DPAD_CENTER,
                                            KeyEvent.KEYCODE_ENTER -> {
                                                ignoreNextKeyUp = false
                                                return@onPreviewKeyEvent true
                                            }
                                        }
                                    }

                                    // Handle move mode key events
                                    if (isInMoveMode) {
                                        if (event.type == KeyEventType.KeyDown) {
                                            when (event.key.nativeKeyCode) {
                                                KeyEvent.KEYCODE_DPAD_UP -> {
                                                    onMoveUp?.invoke()
                                                    return@onPreviewKeyEvent true
                                                }

                                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    onMoveDown?.invoke()
                                                    return@onPreviewKeyEvent true
                                                }

                                                KeyEvent.KEYCODE_DPAD_CENTER,
                                                KeyEvent.KEYCODE_ENTER -> {
                                                    isInMoveMode = false
                                                    ignoreNextKeyUp = true
                                                    return@onPreviewKeyEvent true
                                                }

                                                KeyEvent.KEYCODE_BACK -> {
                                                    isInMoveMode = false
                                                    return@onPreviewKeyEvent true
                                                }
                                            }
                                        } else if (event.type == KeyEventType.KeyUp) {
                                            // Consume all KeyUp events while in move mode to prevent card activation
                                            when (event.key.nativeKeyCode) {
                                                KeyEvent.KEYCODE_DPAD_CENTER,
                                                KeyEvent.KEYCODE_ENTER,
                                                KeyEvent.KEYCODE_DPAD_UP,
                                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    return@onPreviewKeyEvent true
                                                }
                                            }
                                        }
                                    }

                                    // Handle long press for popup or remove program (time-based)
                                    if (event.key.nativeKeyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                        event.key.nativeKeyCode == KeyEvent.KEYCODE_ENTER
                                    ) {
                                        if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) {
                                            // Start tracking key hold
                                            isKeyHeld = true
                                            isLongPressTriggered = false
                                        } else if (event.type == KeyEventType.KeyUp) {
                                            isKeyHeld = false
                                            if (isLongPressTriggered) {
                                                // Long press was already handled, consume the event
                                                isLongPressTriggered = false
                                                return@onPreviewKeyEvent true
                                            }
                                            // Short press - don't consume, let it through for onClick
                                        }
                                    }
                                    false
                                }
                        )
                    }
                }
            }
        }
    }

    // Popup for channel options (non-watch-next channels)
    if (channel != null && onToggleEnabled != null) {
        PopupContainer(
            visible = popupVisible,
            onDismiss = { popupVisible = false },
            content = {},
            popupContent = {
                ChannelPopup(
                    channelName = channel.displayName,
                    isEnabled = channel.enabled,
                    onToggleEnabled = onToggleEnabled,
                    onEnterMoveMode = { isInMoveMode = true },
                    onDismiss = { popupVisible = false }
                )
            }
        )
    }

    // Popup for individual watch next program removal
    if (onRemoveProgram != null) {
        PopupContainer(
            visible = watchNextPopupProgram != null,
            onDismiss = { watchNextPopupProgram = null },
            content = {},
            popupContent = {
                watchNextPopupProgram?.let { program ->
                    WatchNextProgramPopup(
                        programName = program.title ?: "Watch Next Item",
                        onRemove = {
                            // Calculate focus target before removal
                            val index = programs.indexOfFirst { it.id == program.id }
                            if (index != -1) {
                                val nextProgram = if (index < programs.size - 1) {
                                    programs[index + 1]
                                } else if (index > 0) {
                                    programs[index - 1]
                                } else {
                                    null
                                }

                                if (nextProgram != null) {
                                    focusedProgramId = nextProgram.id
                                }
                            }

                            onRemoveProgram.invoke(program)
                            watchNextPopupProgram = null
                        },
                        onDismiss = { watchNextPopupProgram = null }
                    )
                }
            }
        )
    }
}
