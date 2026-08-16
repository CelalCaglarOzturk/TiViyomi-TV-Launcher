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
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.mudrock.tiviyomitvlauncher.ui.util.NuvioScrollDefaults
import dev.mudrock.tiviyomitvlauncher.ui.util.dpadRepeatThrottle

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CardRow(
    modifier: Modifier = Modifier,
    title: String? = null,
    state: LazyListState = rememberLazyListState(),
    baseHeight: Dp = 90.dp,
    content: LazyListScope.(childFocusRequester: FocusRequester) -> Unit,
) {
    Column(
        modifier = modifier
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

        CompositionLocalProvider(LocalBringIntoViewSpec provides NuvioScrollDefaults.smoothScrollSpec) {
            LazyRow(
                state = state,
                contentPadding = PaddingValues(
                    vertical = 4.dp,
                    horizontal = 48.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .dpadRepeatThrottle(horizontalGateMs = 70L),
            ) {
                content(childFocusRequester)
            }
        }
    }
}
