package dev.mudrock.tiviyomitvlauncher.ui.onboarding

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.mudrock.tiviyomitvlauncher.R
import dev.mudrock.tiviyomitvlauncher.util.DefaultLauncherHelper

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val pageCount = OnboardingPage.pages.size
    val isLastPage = currentPage == pageCount - 1
    val context = LocalContext.current
    val defaultLauncherHelper = remember { DefaultLauncherHelper(context) }
    
    val nextButtonFocusRequester = remember { FocusRequester() }
    val defaultLauncherButtonFocusRequester = remember { FocusRequester() }

    // Intercept physical remote back button
    BackHandler {
        if (currentPage > 0) {
            currentPage--
        } else {
            onDismiss()
        }
    }

    // Auto-focus primary button on page change
    LaunchedEffect(currentPage) {
        try {
            nextButtonFocusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus request failure on initial composition
        }
    }

    // Full 100% viewport Android TV surface with ambient gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B), // Deep indigo core
                        Color(0xFF0F172A), // Dark slate body
                        Color(0xFF070A12)  // Ultra dark edges
                    ),
                    radius = 1600f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF312E81), CircleShape)
                            .padding(4.dp)
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.tv.material3.SurfaceDefaults.colors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                ) {
                    Text(
                        text = "FAST • OPEN SOURCE • NO ADS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA5B4FC),
                            letterSpacing = 1.2.sp
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Main 2-Column Content Layout taking full middle viewport
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column (40% Viewport Width): Hero Feature Card & Indicators
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.tv.material3.SurfaceDefaults.colors(
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)
                    ),
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight()
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Hero Icon Showcase with smooth transition
                        Crossfade(
                            targetState = OnboardingPage.pages[currentPage],
                            label = "onboarding_icon_fade"
                        ) { page ->
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF4F46E5), Color(0xFF4338CA))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = page.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        }

                        // Feature Badges specific to current step
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val page = OnboardingPage.pages[currentPage]
                            page.highlights.forEach { highlight ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF818CF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = highlight,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFFE2E8F0)
                                        )
                                    )
                                }
                            }
                        }

                        // Step Pager Dots Indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(pageCount) { index ->
                                val isSelected = index == currentPage
                                Box(
                                    modifier = Modifier
                                        .height(8.dp)
                                        .width(if (isSelected) 32.dp else 10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFF818CF8)
                                            else Color(0xFF334155)
                                        )
                                )
                            }
                        }
                    }
                }

                // Right Column (60% Viewport Width): Title, Description & Action Steps
                Column(
                    modifier = Modifier
                        .weight(0.60f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "STEP ${currentPage + 1} OF $pageCount",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFF818CF8),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    AnimatedContent(
                        targetState = OnboardingPage.pages[currentPage],
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "onboarding_text_anim"
                    ) { page ->
                        Column {
                            Text(
                                text = stringResource(page.titleRes),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Text(
                                text = stringResource(page.descriptionRes),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 28.sp,
                                    fontSize = 18.sp
                                ),
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                        }
                    }

                    // Special action step for Step 4 (Default Launcher request)
                    if (isLastPage && defaultLauncherHelper.canRequestDefaultLauncher()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.tv.material3.SurfaceDefaults.colors(
                                containerColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .border(1.dp, Color(0xFF374151), RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.onboarding_page4_title),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = if (defaultLauncherHelper.isDefaultLauncher()) "Currently set as default launcher" else "Tap to prompt Android TV home app selection",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                                    )
                                }

                                if (!defaultLauncherHelper.isDefaultLauncher()) {
                                    Button(
                                        onClick = {
                                            val intent = defaultLauncherHelper.requestDefaultLauncherIntent()
                                            if (intent != null) {
                                                context.startActivity(intent)
                                            }
                                        },
                                        modifier = Modifier.focusRequester(defaultLauncherButtonFocusRequester)
                                    ) {
                                        Text("Set Default")
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Control Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back button (if available)
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = { currentPage-- },
                        colors = ButtonDefaults.colors(
                            contentColor = Color.White,
                            containerColor = Color.Transparent
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Text("Back")
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Right: Skip & Next / Finish Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isLastPage) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.colors(
                                contentColor = Color(0xFF94A3B8),
                                containerColor = Color.Transparent
                            )
                        ) {
                            Text(stringResource(R.string.onboarding_skip))
                        }

                        Button(
                            onClick = { currentPage++ },
                            modifier = Modifier.focusRequester(nextButtonFocusRequester),
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0xFF4F46E5),
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(stringResource(R.string.onboarding_next))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.focusRequester(nextButtonFocusRequester),
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                Text(stringResource(R.string.onboarding_get_started))
                            }
                        }
                    }
                }
            }
        }
    }
}

data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
    val highlights: List<String>
) {
    companion object {
        val pages = listOf(
            OnboardingPage(
                icon = Icons.Default.Home,
                titleRes = R.string.onboarding_page1_title,
                descriptionRes = R.string.onboarding_page1_description,
                highlights = listOf("Clean & Modern UX", "Zero Bloatware", "Ultra Fast Loading")
            ),
            OnboardingPage(
                icon = Icons.Default.Apps,
                titleRes = R.string.onboarding_page2_title,
                descriptionRes = R.string.onboarding_page2_description,
                highlights = listOf("Drag & Move Apps", "Favorites Section", "Hide Unwanted Apps")
            ),
            OnboardingPage(
                icon = Icons.Default.Settings,
                titleRes = R.string.onboarding_page3_title,
                descriptionRes = R.string.onboarding_page3_description,
                highlights = listOf("Custom Grid Sizes", "Smooth Animations", "Backup & Restore")
            ),
            OnboardingPage(
                icon = Icons.Default.Tv,
                titleRes = R.string.onboarding_page4_title,
                descriptionRes = R.string.onboarding_page4_description,
                highlights = listOf("One-click Home Key Setup", "Suppression Mode", "Full TV Remote Support")
            )
        )
    }
}

class OnboardingManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "onboarding"
        private const val KEY_COMPLETED = "completed"
    }

    fun isCompleted(): Boolean {
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun markCompleted() {
        prefs.edit { putBoolean(KEY_COMPLETED, true) }
    }

    fun reset() {
        prefs.edit { remove(KEY_COMPLETED) }
    }
}