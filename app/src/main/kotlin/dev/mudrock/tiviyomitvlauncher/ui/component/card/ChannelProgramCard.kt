package dev.mudrock.tiviyomitvlauncher.ui.component.card

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.StandardCardContainer
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import dev.mudrock.tiviyomitvlauncher.R
import dev.mudrock.tiviyomitvlauncher.data.repository.SettingsRepository
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.ChannelProgram
import dev.mudrock.tiviyomitvlauncher.ui.component.FocusMarqueeText

import org.koin.compose.koinInject

@Composable
fun ChannelProgramCard(
    program: ChannelProgram,
    modifier: Modifier = Modifier,
    baseHeight: Dp = 100.dp,
    overrideAspectRatio: Float? = null,
    isMoving: Boolean = false,
    enableAnimations: Boolean = true
) {
    val context = LocalContext.current

    // Stable interaction source - remember without keys since it's per-composition
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Always use 16:9 aspect ratio for channel cards
    val aspectRatio = remember(overrideAspectRatio) {
        overrideAspectRatio ?: (16f / 9f)
    }

    // Memoize card width calculation
    val cardWidth = remember(baseHeight, aspectRatio) { baseHeight * aspectRatio }
    val density = LocalDensity.current

    val requestWidth = remember(cardWidth, density) { with(density) { cardWidth.roundToPx() } }
    val requestHeight = remember(baseHeight, density) { with(density) { baseHeight.roundToPx() } }

    // Memoize the image request to prevent recreating on every recomposition
    // Key on posterArtUri to ensure we reload if the image changes
    val imageRequest = remember(program.posterArtUri, program.id, context, requestWidth, requestHeight) {
        ImageRequest.Builder(context)
            .data(program.posterArtUri)
            // Include the URI in the cache key so that when a channel app updates
            // its artwork (same program ID, different content) the Refresh button
            // correctly busts the cache and loads the new image.
            .memoryCacheKey("program:${program.id}:${program.posterArtUri}_${requestWidth}x${requestHeight}")
            .diskCacheKey("program:${program.id}:${program.posterArtUri}_${requestWidth}x${requestHeight}")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true)
            .allowRgb565(true)
            .crossfade(false)
            .precision(coil.size.Precision.INEXACT)
            .size(requestWidth, requestHeight)
            .build()
    }

    // Memoize the launch intent to avoid parsing on every click
    val launchIntent = remember(program.intentUri) {
        program.intentUri?.let { uri ->
            try {
                Intent.parseUri(uri, 0)
            } catch (e: Exception) {
                null
            }
        }
    }

    // Memoize whether we have a title to display
    val hasTitle = remember(program.title) { !program.title.isNullOrEmpty() }

    // Calculate progress for watch next items
    val progress = remember(program.lastPlaybackPositionMillis, program.durationMillis) {
        val current = program.lastPlaybackPositionMillis
        val total = program.durationMillis
        if (current != null && total != null && total > 0 && current > 0) {
            (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }
    }

    Column(
        modifier = modifier.width(cardWidth)
    ) {
        Card(
            modifier = Modifier
                .height(baseHeight)
                .aspectRatio(aspectRatio),
            interactionSource = interactionSource,
            colors = CardDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(3.dp, Color.White),
                )
            ),
            scale = CardDefaults.scale(
                focusedScale = if (enableAnimations) 1.05f else 1.0f
            ),
            onClick = {
                launchIntent?.let { intent ->
                    context.startActivity(intent)
                }
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = imageRequest,
                    contentDescription = program.title,
                    contentScale = ContentScale.Crop,
                )

                if (progress != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomStart)
                            .background(Color.Black.copy(alpha = 0.5f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .align(Alignment.BottomStart)
                            .background(Color.Red)
                    )
                }
                
                if (isMoving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRoundRect(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    cornerRadius = CornerRadius(0.dp.toPx())
                                )
                            }
                    ) {
                        Text(
                            text = stringResource(R.string.channel_moving_instructions),
                            color = Color.White,
                            fontSize = with(LocalDensity.current) { (baseHeight.value / 10).toInt().dp.toSp() },
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        if (hasTitle) {
            Column(
                modifier = Modifier.padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FocusMarqueeText(
                    text = program.title!!,
                    focused = isFocused && enableAnimations && !isMoving,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
