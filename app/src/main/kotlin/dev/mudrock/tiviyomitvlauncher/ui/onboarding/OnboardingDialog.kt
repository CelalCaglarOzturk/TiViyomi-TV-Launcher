package dev.mudrock.tiviyomitvlauncher.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.mudrock.tiviyomitvlauncher.R
import dev.mudrock.tiviyomitvlauncher.accessibility.LauncherAccessibilityService
import dev.mudrock.tiviyomitvlauncher.util.DefaultLauncherHelper

private const val PERMISSION_READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS"

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val onboardingManager = remember { OnboardingManager(context) }
    var currentPage by remember { mutableIntStateOf(onboardingManager.getSavedPage()) }
    val pageCount = OnboardingPage.pages.size
    val isLastPage = currentPage == pageCount - 1
    val defaultLauncherHelper = remember { DefaultLauncherHelper(context) }
    
    val nextButtonFocusRequester = remember { FocusRequester() }

    // Dynamic Permission States
    var isTvListingsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, PERMISSION_READ_TV_LISTINGS) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isStorageGranted by remember {
        mutableStateOf(checkStoragePermission(context))
    }

    var isAccessibilityGranted by remember {
        mutableStateOf(checkAccessibilityServiceEnabled(context))
    }

    var isDefaultLauncherGranted by remember {
        mutableStateOf(defaultLauncherHelper.isDefaultLauncher())
    }

    // Activity Result Launchers
    val tvListingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isTvListingsGranted = granted
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isStorageGranted = checkStoragePermission(context)
    }

    val defaultLauncherPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        isDefaultLauncherGranted = defaultLauncherHelper.isDefaultLauncher()
    }

    val accessibilitySettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        isAccessibilityGranted = checkAccessibilityServiceEnabled(context)
    }

    // Intercept physical remote back button
    BackHandler {
        if (currentPage > 0) {
            currentPage--
        } else {
            onDismiss()
        }
    }

    // Auto-focus primary action button on page change & save progress
    LaunchedEffect(currentPage) {
        onboardingManager.setSavedPage(currentPage)
        isTvListingsGranted = ContextCompat.checkSelfPermission(context, PERMISSION_READ_TV_LISTINGS) == PackageManager.PERMISSION_GRANTED
        isStorageGranted = checkStoragePermission(context)
        isAccessibilityGranted = checkAccessibilityServiceEnabled(context)
        isDefaultLauncherGranted = defaultLauncherHelper.isDefaultLauncher()

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
                .padding(horizontal = 44.dp, vertical = 24.dp),
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
                            .size(40.dp)
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
                            text = "Capabilities & Permissions Walkthrough",
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
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column (38% Viewport Width): Visual Feature Showcase & Indicators
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.tv.material3.SurfaceDefaults.colors(
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.9f)
                    ),
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
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
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(24.dp))
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
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }

                        // Feature Badges specific to current step
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
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
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = highlight,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFFE2E8F0),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
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

                // Right Column (62% Viewport Width): Title, Rationale, Permission Prompts & Warnings
                Column(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "STAGE ${currentPage + 1} OF $pageCount",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFF818CF8),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
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
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = page.description,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 24.sp,
                                    fontSize = 16.sp
                                ),
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                        }
                    }

                    // Interactive Permission Prompts & Warning Banners based on current stage
                    when (currentPage) {
                        0 -> { // Stage 1: Installed Apps Query Rationale
                            InfoBanner(
                                title = "Installed Apps Auto-Discovery (QUERY_ALL_PACKAGES)",
                                message = "Android TV requires package query permission so the launcher can automatically detect all installed apps on your device and populate your launcher grid without remote telemetry."
                            )
                        }
                        1 -> { // Stage 2: TV Listings Permission Prompt & Warning
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PermissionActionCard(
                                    title = "TV Listings Permission (READ_TV_LISTINGS)",
                                    rationale = "Allows TiViyomi to fetch live recommendations, Watch Next feeds, and media channels from streaming apps like YouTube and Netflix.",
                                    isGranted = isTvListingsGranted,
                                    buttonText = "Grant TV Listings Permission",
                                    onClick = {
                                        tvListingsLauncher.launch(PERMISSION_READ_TV_LISTINGS)
                                    }
                                )

                                if (!isTvListingsGranted) {
                                    WarningBanner(
                                        title = "Warning: TV Channels Disabled",
                                        message = "If denied, the launcher will be unable to read channel recommendations or Watch Next programs from installed apps, keeping channel rows empty."
                                    )
                                }
                            }
                        }
                        2 -> { // Stage 3: Default Launcher & Accessibility Interceptor
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PermissionActionCard(
                                    title = "Default Home Launcher Role",
                                    rationale = "Ensures your device starts directly into TiViyomi TV Launcher when powered on.",
                                    isGranted = isDefaultLauncherGranted,
                                    buttonText = "Set Default Launcher",
                                    onClick = {
                                        var launched = false
                                        val roleIntent = defaultLauncherHelper.requestDefaultLauncherIntent()
                                        if (roleIntent != null) {
                                            try {
                                                defaultLauncherPickerLauncher.launch(roleIntent)
                                                launched = true
                                            } catch (e: Exception) {
                                                // RoleManager intent failed
                                            }
                                        }
                                        if (!launched) {
                                            try {
                                                defaultLauncherPickerLauncher.launch(Intent(Settings.ACTION_HOME_SETTINGS))
                                                launched = true
                                            } catch (e: Exception) {
                                                // Home settings failed
                                            }
                                        }
                                        if (!launched) {
                                            try {
                                                val homeChooserIntent = Intent(Intent.ACTION_MAIN).apply {
                                                    addCategory(Intent.CATEGORY_HOME)
                                                    addCategory(Intent.CATEGORY_DEFAULT)
                                                }
                                                val chooser = Intent.createChooser(homeChooserIntent, "Select Home Launcher")
                                                defaultLauncherPickerLauncher.launch(chooser)
                                            } catch (e: Exception) {
                                                try {
                                                    defaultLauncherPickerLauncher.launch(Intent(Settings.ACTION_SETTINGS))
                                                } catch (e2: Exception) {
                                                    // Settings failed
                                                }
                                            }
                                        }
                                    }
                                )

                                PermissionActionCard(
                                    title = "Home Button Accessibility Interceptor",
                                    rationale = "Catches physical Home key presses on your remote when returning from external apps like Netflix or YouTube.",
                                    isGranted = isAccessibilityGranted,
                                    buttonText = "Open Accessibility Settings",
                                    onClick = {
                                        val intents = listOf(
                                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                                            Intent("android.settings.ACCESSIBILITY_SETTINGS"),
                                            Intent(Settings.ACTION_SETTINGS)
                                        )
                                        for (intent in intents) {
                                            try {
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                                break
                                            } catch (e: Exception) {
                                                // Try next fallback
                                            }
                                        }
                                    }
                                )

                                if (!isAccessibilityGranted) {
                                    WarningBanner(
                                        title = "Warning: Home Key Bypass Risk",
                                        message = "Without Accessibility service enabled, pressing the Home button inside third-party apps may exit to the manufacturer's stock launcher instead of TiViyomi."
                                    )
                                }
                            }
                        }
                        3 -> { // Stage 4: Personalization & Media Read Access Rationale
                            InfoBanner(
                                title = "Wallpaper Storage Rationale (READ_EXTERNAL_STORAGE)",
                                message = "Selecting custom background wallpaper images requires read access to image files stored on your TV storage. Preset background colors do not require any permission."
                            )
                        }
                        4 -> { // Stage 5: Quick Controls & TV Input Manager
                            InfoBanner(
                                title = "HDMI TV Input Manager Rationale",
                                message = "Input switching utilizes Android TV's built-in TvInputManager framework to detect connected HDMI 1, HDMI 2, and AV sources without accessing private hardware signals."
                            )
                        }
                        5 -> { // Stage 6: Storage Access for Backups Prompt & Warning
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PermissionActionCard(
                                    title = "All Files Storage Access (MANAGE_EXTERNAL_STORAGE)",
                                    rationale = "Required to write and read layout backup JSON files at Documents/TVLauncher/tv_launcher_backup.json.",
                                    isGranted = isStorageGranted,
                                    buttonText = "Grant Storage Permission",
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = "package:${context.packageName}".toUri()
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            try {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    })
                                                } else {
                                                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                }
                                            } catch (e2: Exception) {
                                                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                })
                                            }
                                        }
                                    }
                                )

                                if (!isStorageGranted) {
                                    WarningBanner(
                                        title = "Warning: Backup Feature Disabled",
                                        message = "Without storage permission, saving layout backups or restoring preferences from previous backup files will fail."
                                    )
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
                // Left: Previous Step button
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
                                Text("Complete Setup & Start Launcher")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionActionCard(
    title: String,
    rationale: String,
    isGranted: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(
            containerColor = Color(0xFF1E293B)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.tv.material3.SurfaceDefaults.colors(
                            containerColor = if (isGranted) Color(0xFF065F46) else Color(0xFF78350F)
                        )
                    ) {
                        Text(
                            text = if (isGranted) "GRANTED" else "ACTION REQUIRED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isGranted) Color(0xFF34D399) else Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = rationale,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (!isGranted) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Text(buttonText)
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun WarningBanner(
    title: String,
    message: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(
            containerColor = Color(0xFF451A03).copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color(0xFFFDE68A),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFFEF3C7),
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun InfoBanner(
    title: String,
    message: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(
            containerColor = Color(0xFF1E293B)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF818CF8),
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}

private fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun checkAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedService = "${context.packageName}/${LauncherAccessibilityService::class.java.canonicalName}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(expectedService) || enabledServices.contains(context.packageName)
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
            // Stage 1: Overview
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
            // Stage 2: Apps & Channels
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
            // Stage 3: System Interception
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
            // Stage 4: Visual Personalization
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
            // Stage 5: Toolbar & Inputs
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
            // Stage 6: Backup & Crash Recovery
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
        private const val KEY_SAVED_PAGE = "saved_page"
    }

    fun isCompleted(): Boolean {
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun markCompleted() {
        prefs.edit {
            putBoolean(KEY_COMPLETED, true)
            remove(KEY_SAVED_PAGE)
        }
    }

    fun getSavedPage(): Int {
        return prefs.getInt(KEY_SAVED_PAGE, 0)
    }

    fun setSavedPage(page: Int) {
        prefs.edit { putInt(KEY_SAVED_PAGE, page) }
    }

    fun reset() {
        prefs.edit { clear() }
    }
}