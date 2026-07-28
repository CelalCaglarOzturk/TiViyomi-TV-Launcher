package dev.mudrock.tiviyomitvlauncher.ui

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.Surface
import androidx.tv.material3.darkColorScheme
import coil.compose.AsyncImage
import dev.mudrock.tiviyomitvlauncher.data.repository.SettingsRepository
import dev.mudrock.tiviyomitvlauncher.ui.onboarding.OnboardingDialog
import dev.mudrock.tiviyomitvlauncher.ui.screen.launcher.LauncherScreen
import dev.mudrock.tiviyomitvlauncher.util.DeepLink
import org.koin.compose.koinInject

@Composable
fun AppBase(
	showOnboarding: Boolean = false,
	pendingDeepLink: DeepLink? = null,
	onOnboardingComplete: () -> Unit = {},
	onDeepLinkHandled: () -> Unit = {}
) {
	val settingsRepository = koinInject<SettingsRepository>()
	val backgroundColor by settingsRepository.backgroundColor.collectAsStateWithLifecycle()
	val backgroundImageUri by settingsRepository.backgroundImageUri.collectAsStateWithLifecycle()
	
	var onboardingShown by remember { mutableStateOf(showOnboarding) }
	var deepLink by remember { mutableStateOf(pendingDeepLink) }

	val baseColor = Color(backgroundColor)

	MaterialTheme(
		colorScheme = darkColorScheme(
			background = baseColor,
			surface = baseColor,
		),
		shapes = Shapes(
			small = RoundedCornerShape(4.dp),
			medium = RoundedCornerShape(4.dp),
			large = RoundedCornerShape(8.dp)
		),
	) {
		CompositionLocalProvider(
			LocalOverscrollFactory provides null
		) {
			Surface(
				modifier = Modifier.fillMaxSize()
			) {
				// Background layer (image or color)
				if (backgroundImageUri != null) {
					AsyncImage(
						model = backgroundImageUri!!.toUri(),
						contentDescription = null,
						modifier = Modifier.fillMaxSize(),
						contentScale = ContentScale.Crop
					)
				} else {
					Box(
						modifier = Modifier
							.fillMaxSize()
							.background(baseColor)
					)
				}
				
				if (onboardingShown) {
					OnboardingDialog(
						onDismiss = {
							onboardingShown = false
							onOnboardingComplete()
						}
					)
				} else {
					LauncherScreen(
						pendingDeepLink = deepLink,
						onDeepLinkHandled = {
							deepLink = null
							onDeepLinkHandled()
						}
					)
				}
			}
		}
	}
}