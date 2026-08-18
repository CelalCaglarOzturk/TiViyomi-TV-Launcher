package dev.mudrock.tiviyomitvlauncher.ui.component

import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

private val MarqueeVelocity = 45.dp

private fun String.isRtl(): Boolean {
    for (char in this) {
        val directionality = Character.getDirectionality(char)
        if (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
            directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
            return true
        }
        if (directionality == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
            return false
        }
    }
    return false
}

/**
 * Single-line text that smoothly scrolls (marquees) horizontally while [focused] if the content overflows,
 * and otherwise ellipsizes with zero animation overhead when unfocused.
 */
@Composable
fun FocusMarqueeText(
    text: String,
    focused: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    val marqueeText: @Composable () -> Unit = {
        Text(
            text = text,
            modifier = if (focused) {
                modifier
                    .graphicsLayer { clip = true }
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        velocity = MarqueeVelocity,
                        initialDelayMillis = 1200
                    )
            } else {
                modifier
            },
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis,
            textAlign = textAlign,
        )
    }

    if (focused) {
        val marqueeDirection = remember(text) {
            if (text.isRtl()) LayoutDirection.Rtl else LayoutDirection.Ltr
        }
        CompositionLocalProvider(LocalLayoutDirection provides marqueeDirection) {
            marqueeText()
        }
    } else {
        marqueeText()
    }
}
