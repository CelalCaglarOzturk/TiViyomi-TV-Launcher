package dev.mudrock.tiviyomitvlauncher.ui.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
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

    // Intercept physical remote back button
    BackHandler {
        if (currentPage > 0) {
            currentPage--
        } else {
            onDismiss()
        }
    }

    // Auto-focus primary action button on page change for instant DPAD control
    LaunchedEffect(currentPage) {
        try {
            nextButtonFocusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus request failure on initial composition
        }
    }

    // Full 100% viewport Android TV surface with ambient background
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
                .padding(horizontal = 48.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(42.dp)
                            .background(Color(0xFF312E81), CircleShape)
                            .padding(4.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Comprehensive Setup & Capabilities Walkthrough",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.tv.material3.SurfaceDefaults.colors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)
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
                // Left Column (40% Viewport Width): Visual Feature Showcase & Indicators
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.tv.material3.SurfaceDefaults.colors(
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.9f)
                    ),
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight()
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Category Badge
                        Text(
                            text = OnboardingPage.pages[currentPage].categoryBadge.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF818CF8),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )

                        // Hero Icon Showcase with smooth transition
                        Crossfade(
                            targetState = OnboardingPage.pages[currentPage],
                            label = "onboarding_icon_fade"
                        ) { page ->
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(page.accentColor, page.secondaryAccentColor)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = page.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        // Feature Badges specific to current step
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val page = OnboardingPage.pages[currentPage]
                            page.highlights.forEach { highlight ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = page.accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = highlight,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFFE2E8F0),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }

                        // Step Pager Dots Indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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

                // Right Column (60% Viewport Width): Title, Capabilities Detail & Live Action Prompts
                Column(
                    modifier = Modifier
                        .weight(0.60f)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "CAPABILITY ${currentPage + 1} OF $pageCount",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFF818CF8),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    AnimatedContent(
                        targetState = OnboardingPage.pages[currentPage],
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "onboarding_text_anim"
                    ) { page ->
                        Column {
                            Text(
                                text = page.title,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = page.description,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 26.sp,
                                    fontSize = 17.sp
                                ),
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                        }
                    }

                    // Interactive Step Action Controls based on capability page
                    when (currentPage) {
                        2 -> { // Step 3: Default Launcher & Accessibility setup
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ActionCard(
                                    title = "Set as Default Home Launcher",
                                    subtitle = if (defaultLauncherHelper.isDefaultLauncher()) "Currently set as default launcher" else "Prompt Android TV home application selection",
                                    buttonText = "Set Default",
                                    isDone = defaultLauncherHelper.isDefaultLauncher(),
                                    onClick = {
                                        defaultLauncherHelper.requestDefaultLauncherIntent()?.let { intent ->
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                                ActionCard(
                                    title = "Home Key Accessibility Interceptor",
                                    subtitle = "Allows launcher to catch Home key when leaving external apps",
                                    buttonText = "Open Accessibility",
                                    isDone = false,
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        } catch (e: Exception) {
                                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                        }
                                    }
                                )
                            }
                        }
                        3 -> { // Step 4: Appearance & Animations Preview
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                colors = androidx.tv.material3.SurfaceDefaults.colors(
                                    containerColor = Color(0xFF1E293B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CapabilityPill(icon = Icons.Default.Palette, label = "Custom Wallpaper & Color Presets")
                                    CapabilityPill(icon = Icons.Default.TouchApp, label = "App & Channel Card Resizing")
                                }
                            }
                        }
                        4 -> { // Step 5: Toolbar & Inputs Preview
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                colors = androidx.tv.material3.SurfaceDefaults.colors(
                                    containerColor = Color(0xFF1E293B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CapabilityPill(icon = Icons.AutoMirrored.Filled.Input, label = "HDMI 1/2 Input Switcher")
                                    CapabilityPill(icon = Icons.Default.Settings, label = "Reorderable Top Toolbar")
                                }
                            }
                        }
                        5 -> { // Step 6: Backup & Safety
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                colors = androidx.tv.material3.SurfaceDefaults.colors(
                                    containerColor = Color(0xFF1E293B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CapabilityPill(icon = Icons.Default.Backup, label = "JSON Storage Backup")
                                    CapabilityPill(icon = Icons.Default.Security, label = "Automatic Crash Recovery")
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Control Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back button
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
                            Text("Previous Step")
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
                                Text("Complete Setup & Start")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    buttonText: String,
    isDone: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(
            containerColor = Color(0xFF1E293B)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )
            }

            if (!isDone) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    )
                ) {
                    Text(buttonText)
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

@Composable
private fun CapabilityPill(
    icon: ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFA5B4FC),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

data class OnboardingPage(
    val categoryBadge: String,
    val icon: ImageVector,
    val accentColor: Color,
    val secondaryAccentColor: Color,
    val title: String,
    val description: String,
    val highlights: List<String>
) {
    companion object {
        val pages = listOf(
            // Page 1: Overview
            OnboardingPage(
                categoryBadge = "Core Philosophy",
                icon = Icons.Default.Home,
                accentColor = Color(0xFF4F46E5),
                secondaryAccentColor = Color(0xFF4338CA),
                title = "Clean, Fast & Open Source Home Screen",
                description = "TiViyomi TV Launcher is built specifically for Android TV and Google TV devices. It provides a lightweight, bloatware-free home screen with zero ads and lightning-fast responsiveness.",
                highlights = listOf(
                    "100% Ad-free & privacy focused",
                    "Built with Jetpack Compose for Android TV",
                    "Lightweight memory & CPU footprint"
                )
            ),
            // Page 2: Apps & Channels
            OnboardingPage(
                categoryBadge = "Apps & Feeds",
                icon = Icons.Default.Apps,
                accentColor = Color(0xFF0EA5E9),
                secondaryAccentColor = Color(0xFF0284C7),
                title = "Custom App Grid & Channel Rows",
                description = "Easily manage your favorite applications and Watch Next channel feeds. Long-press any app card or channel row with your TV remote to reorder, add to favorites, or hide apps.",
                highlights = listOf(
                    "Long-press DPAD / Menu key for App Options",
                    "Hide unwanted stock or system apps",
                    "Show non-TV mobile apps toggle"
                )
            ),
            // Page 3: System Interception
            OnboardingPage(
                categoryBadge = "System Integration",
                icon = Icons.Default.Tv,
                accentColor = Color(0xFF8B5CF6),
                secondaryAccentColor = Color(0xFF7C3AED),
                title = "Home Button Interception & Default Launcher",
                description = "Set TiViyomi as your default launcher and enable Accessibility Interception so pressing the Home button on your TV remote always brings you directly back home.",
                highlights = listOf(
                    "Default Home launcher integration",
                    "Home key press interception from external apps",
                    "Smooth transition back to home screen"
                )
            ),
            // Page 4: Visual Personalization
            OnboardingPage(
                categoryBadge = "Personalization",
                icon = Icons.Default.Palette,
                accentColor = Color(0xFFEC4899),
                secondaryAccentColor = Color(0xFFDB2777),
                title = "Card Resizing, Wallpapers & Animations",
                description = "Customize card dimensions for apps and channel rows. Choose custom background wallpapers, preset colors, and configure smooth marquee text and row scaling animations.",
                highlights = listOf(
                    "Adjust app card size (70dp to 220dp)",
                    "Custom image wallpapers & color presets",
                    "Granular control over focus animations"
                )
            ),
            // Page 5: Toolbar & Inputs
            OnboardingPage(
                categoryBadge = "Quick Controls",
                icon = Icons.AutoMirrored.Filled.Input,
                accentColor = Color(0xFFF59E0B),
                secondaryAccentColor = Color(0xFFD97706),
                title = "Reorderable Toolbar & HDMI Switcher",
                description = "Instantly switch between HDMI inputs and customize top bar items. Drag and reorder Wi-Fi, System Settings, Launcher Settings, Notifications, and Channel Refresh actions.",
                highlights = listOf(
                    "HDMI 1, HDMI 2, AV input fast switcher",
                    "Customize top bar icon visibility & order",
                    "One-tap channel list reloader"
                )
            ),
            // Page 6: Backup & Crash Recovery
            OnboardingPage(
                categoryBadge = "Data & Safety",
                icon = Icons.Default.Backup,
                accentColor = Color(0xFF10B981),
                secondaryAccentColor = Color(0xFF059669),
                title = "Backup, Restore & Crash Protection",
                description = "Safely back up your entire launcher configuration to your TV storage. If a system failure occurs, built-in crash loop detection automatically launches Recovery Mode to restore your launcher.",
                highlights = listOf(
                    "JSON layout backup to Documents folder",
                    "Timestamped backup history snapshots",
                    "Automatic crash loop protection"
                )
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