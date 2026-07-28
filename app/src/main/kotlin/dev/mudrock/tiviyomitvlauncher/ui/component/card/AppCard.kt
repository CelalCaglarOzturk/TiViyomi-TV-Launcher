package dev.mudrock.tiviyomitvlauncher.ui.component.card

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
import dev.mudrock.tiviyomitvlauncher.data.repository.SettingsRepository
import dev.mudrock.tiviyomitvlauncher.data.sqldelight.App
import dev.mudrock.tiviyomitvlauncher.ui.component.PopupContainer
import org.koin.compose.koinInject

@Composable
fun AppCard(
    app: App,
    modifier: Modifier = Modifier,
    baseHeight: Dp = 90.dp,
    popupContent: (@Composable () -> Unit)? = null,
    onPopupVisibilityChanged: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val settingsRepository = koinInject<SettingsRepository>()
    val enableAnimations by settingsRepository.enableAnimations.collectAsStateWithLifecycle()
    val animAppIcon by settingsRepository.animAppIcon.collectAsStateWithLifecycle()
    val areAnimationsEnabled = enableAnimations && animAppIcon

    // Stable interaction source - remember without keys since it's per-composition
    val interactionSource = remember { MutableInteractionSource() }

    // Cache launch intent - only recompute when app changes
    // Parse the intent once and reuse
    val launchIntent = remember(app.id) {
        val intentUri = app.launchIntentUriLeanback ?: app.launchIntentUriDefault
        intentUri?.let { uri ->
            try {
                Intent.parseUri(uri, 0)
            } catch (e: Exception) {
                null
            }
        }
    }

    var menuVisible by remember { mutableStateOf(false) }

    // Notify parent of popup visibility changes
    LaunchedEffect(menuVisible) {
        onPopupVisibilityChanged?.invoke(menuVisible)
    }

    // Pre-calculate dimensions to avoid repeated calculations
    val cardWidth = remember(baseHeight) { baseHeight * (16f / 9f) }
    val density = LocalDensity.current

    val requestWidth = remember(cardWidth, density) { with(density) { cardWidth.roundToPx() } }
    val requestHeight = remember(baseHeight, density) { with(density) { baseHeight.roundToPx() } }

    // Memoize the image request to prevent recreating on every recomposition
    // Key on app.id since that uniquely identifies the app and its icon
    val imageRequest = remember(app.id, context, areAnimationsEnabled, requestWidth, requestHeight) {
        ImageRequest.Builder(context)
            .data(app)
            .memoryCacheKey("app_icon:${app.id}")
            .diskCacheKey("app_icon:${app.id}")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(areAnimationsEnabled)
            .size(requestWidth, requestHeight)
            .build()
    }

    val hasPopupContent = remember(popupContent) { popupContent != null }
    
    val isFocused by interactionSource.collectIsFocusedAsState()

    val highlightColors = remember {
        listOf(
            Color.White.copy(alpha = 0.3f),
            Color.White.copy(alpha = 0.0f)
        )
    }

    PopupContainer(
        visible = menuVisible && hasPopupContent,
        onDismiss = { menuVisible = false },
        content = {
            StandardCardContainer(
                modifier = modifier.width(cardWidth),
                interactionSource = interactionSource,
                title = {
                    AppCardTitle(
                        title = app.displayName,
                        interactionSource = interactionSource,
                        enableAnimations = areAnimationsEnabled
                    )
                },
                imageCard = { _ ->
                    Card(
                        modifier = Modifier
                            .height(baseHeight)
                            .aspectRatio(16f / 9f)
                            .then(
                                if (isFocused) {
                                    Modifier.drawBehind {
                                        drawRect(
                                            brush = Brush.radialGradient(
                                                colors = highlightColors,
                                                radius = size.maxDimension * 0.8f
                                            )
                                        )
                                    }
                                } else Modifier
                            ),
                        interactionSource = interactionSource,
                        border = CardDefaults.border(
                            focusedBorder = Border(
                                border = BorderStroke(
                                    3.dp,
                                    Color.White
                                ),
                            )
                        ),
                        onClick = {
                            if (onClick != null) {
                                onClick()
                            } else {
                                launchIntent?.let { intent ->
                                    context.startActivity(intent)
                                }
                            }
                        },
                        onLongClick = {
                            if (hasPopupContent) {
                                menuVisible = true
                            }
                        }
                    ) {
                        AsyncImage(
                            modifier = Modifier.fillMaxSize(),
                            model = imageRequest,
                            contentDescription = app.displayName,
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            )
        },
        popupContent = {
            if (popupContent != null) popupContent()
        }
    )
}

@Composable
private fun AppCardTitle(
    title: String,
    interactionSource: InteractionSource,
    enableAnimations: Boolean
) {
    val focused by interactionSource.collectIsFocusedAsState()

    Text(
        text = title,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        softWrap = false,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold
        ),
        modifier = Modifier
            .then(
                if (focused && enableAnimations) {
                    Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 0,
                    )
                } else {
                    Modifier
                }
            )
            .padding(top = 6.dp),
    )
}
