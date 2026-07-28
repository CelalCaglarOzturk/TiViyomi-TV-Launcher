package dev.mudrock.tiviyomitvlauncher.ui.tab.home.row

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.mudrock.tiviyomitvlauncher.data.repository.SettingsRepository
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CardRow(
    modifier: Modifier = Modifier,
    title: String? = null,
    state: LazyListState = rememberLazyListState(),
    baseHeight: Dp = 90.dp,
    content: LazyListScope.(childFocusRequester: FocusRequester) -> Unit,
) {
    val settingsRepository = koinInject<SettingsRepository>()
    val enableAnimations by settingsRepository.enableAnimations.collectAsStateWithLifecycle()
    val animChannelRow by settingsRepository.animChannelRow.collectAsStateWithLifecycle()
    val areRowAnimationsEnabled = enableAnimations && animChannelRow

    var isFocused by remember { mutableStateOf(false) }
    var isAnimatedFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            delay(120L)
            isAnimatedFocused = true
        } else {
            isAnimatedFocused = false
        }
    }

    val transition = updateTransition(targetState = isAnimatedFocused, label = "rowTransition")

    val alpha by transition.animateFloat(
        transitionSpec = {
            if (areRowAnimationsEnabled) {
                if (targetState) tween(durationMillis = 200, easing = FastOutSlowInEasing) else snap()
            } else snap()
        },
        label = "rowAlpha"
    ) { focused ->
        if (focused) 1f else 0.5f
    }

    val scale by transition.animateFloat(
        transitionSpec = {
            if (areRowAnimationsEnabled) {
                if (targetState) tween(durationMillis = 200, easing = FastOutSlowInEasing) else snap()
            } else snap()
        },
        label = "rowScale"
    ) { focused ->
        if (focused) 1f else 0.95f
    }

    Column(
        modifier = modifier
            .onFocusChanged { isFocused = it.hasFocus }
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (title != null) {
            Text(
                text = title,
                fontSize = 18.sp,
                modifier = Modifier.padding(
                    vertical = 4.dp,
                    horizontal = 48.dp,
                )
            )
        }

        val childFocusRequester = remember { FocusRequester() }

        val canScrollBack by remember { derivedStateOf { state.canScrollBackward } }
        val canScrollForward by remember { derivedStateOf { state.canScrollForward } }

        val leftFadeBrush = remember {
            Brush.horizontalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.6f),
                    Color.Transparent
                )
            )
        }

        val rightFadeBrush = remember {
            Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.6f)
                )
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth()
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
                    .focusRestorer(childFocusRequester),
            ) {
                content(childFocusRequester)
            }

            if (canScrollBack && isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .height(height = baseHeight.coerceAtLeast(90.dp))
                        .fillMaxWidth(0.08f)
                        .background(leftFadeBrush)
                )
            }

            if (canScrollForward && isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(height = baseHeight.coerceAtLeast(90.dp))
                        .fillMaxWidth(0.08f)
                        .background(rightFadeBrush)
                )
            }
        }
    }
}
