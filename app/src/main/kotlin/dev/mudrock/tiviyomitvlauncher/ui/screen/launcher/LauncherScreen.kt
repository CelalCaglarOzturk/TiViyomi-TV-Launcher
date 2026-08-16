package dev.mudrock.tiviyomitvlauncher.ui.screen.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.mudrock.tiviyomitvlauncher.data.Destinations
import dev.mudrock.tiviyomitvlauncher.ui.settings.LauncherSettingsDialog
import dev.mudrock.tiviyomitvlauncher.ui.tab.apps.AppsTab
import dev.mudrock.tiviyomitvlauncher.ui.tab.home.HomeTab
import dev.mudrock.tiviyomitvlauncher.ui.toolbar.Toolbar
import dev.mudrock.tiviyomitvlauncher.util.DeepLink
import dev.mudrock.tiviyomitvlauncher.util.FocusController
import dev.mudrock.tiviyomitvlauncher.util.composition.LocalBackStack
import dev.mudrock.tiviyomitvlauncher.util.composition.ProvideNavigation
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LauncherScreen(
	pendingDeepLink: DeepLink? = null,
	onDeepLinkHandled: () -> Unit = {}
) {
    val focusController = koinInject<FocusController>()
    var showLauncherSettings by remember { mutableStateOf(false) }
    var pendingLink by remember { mutableStateOf(pendingDeepLink) }

    if (showLauncherSettings) {
        LauncherSettingsDialog(
            onDismissRequest = { showLauncherSettings = false }
        )
    }

    BackHandler {
        focusController.requestFocusReset()
    }

    ProvideNavigation {
        val backStack = LocalBackStack.current
        
        LaunchedEffect(pendingLink) {
            pendingLink?.let { deepLink ->
                when (deepLink.action) {
                    DeepLink.ACTION_SETTINGS -> {
                        showLauncherSettings = true
                    }
                    DeepLink.ACTION_APPS -> {
                        backStack.add(Destinations.Apps)
                    }
                }
                pendingLink = null
                onDeepLinkHandled()
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Toolbar(
                modifier = Modifier
                    .padding(
                        vertical = 27.dp,
                        horizontal = 48.dp,
                    ),
                onOpenLauncherSettings = { showLauncherSettings = true }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val currentDestination by remember {
                    derivedStateOf {
                        backStack.lastOrNull() ?: Destinations.Home
                    }
                }

                val isHomeVisible = currentDestination == Destinations.Home
                val isAppsVisible = currentDestination == Destinations.Apps

                if (isHomeVisible) {
                    HomeTab(
                        modifier = Modifier.fillMaxSize(),
                        isActive = true
                    )
                } else if (isAppsVisible) {
                    AppsTab(
                        modifier = Modifier.fillMaxSize(),
                        isActive = true
                    )
                }
            }
        }
    }
}