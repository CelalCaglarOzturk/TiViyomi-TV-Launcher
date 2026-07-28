package dev.mudrock.tiviyomitvlauncher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.tvprovider.media.tv.TvContractCompat
import kotlinx.coroutines.launch
import dev.mudrock.tiviyomitvlauncher.crash.CrashHandler
import dev.mudrock.tiviyomitvlauncher.data.repository.AppRepository
import dev.mudrock.tiviyomitvlauncher.data.repository.ChannelRepository
import dev.mudrock.tiviyomitvlauncher.data.repository.InputRepository
import dev.mudrock.tiviyomitvlauncher.data.repository.SettingsRepository
import dev.mudrock.tiviyomitvlauncher.ui.AppBase
import dev.mudrock.tiviyomitvlauncher.ui.onboarding.OnboardingManager
import dev.mudrock.tiviyomitvlauncher.util.DefaultLauncherHelper
import dev.mudrock.tiviyomitvlauncher.util.DeepLink
import dev.mudrock.tiviyomitvlauncher.util.DeepLinkHandler
import dev.mudrock.tiviyomitvlauncher.util.FocusController
import dev.mudrock.tiviyomitvlauncher.util.LauncherConstants
import org.koin.android.ext.android.inject
import timber.log.Timber

class LauncherActivity : ComponentActivity() {
	companion object {
		@SuppressLint("RestrictedApi")
		const val PERMISSION_READ_CHANNELS = TvContractCompat.PERMISSION_READ_TV_LISTINGS
		const val PERMISSION_WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
		val PERMISSIONS = listOf(PERMISSION_READ_CHANNELS, PERMISSION_WRITE_STORAGE)
		private const val REQUEST_DEFAULT_LAUNCHER = 100
		private var testCrashPressCount = 0
		private var lastTestCrashPressTime = 0L
		private const val TEST_CRASH_TIMEOUT_MS = 10000L // Reset counter after 10 seconds
	}

	private val defaultLauncherHelper: DefaultLauncherHelper by inject()
	private val focusController: FocusController by inject()
	private val appRepository: AppRepository by inject()
	private val inputRepository: InputRepository by inject()
	private val channelRepository: ChannelRepository by inject()
	private val settingsRepository: SettingsRepository by inject()
	private lateinit var onboardingManager: OnboardingManager

	private var showOnboarding = false
	private var pendingDeepLink: DeepLink? = null

	private val permissionsLauncher =
		registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
			// Refresh channels when permission is granted
			if (permissions[PERMISSION_READ_CHANNELS] == true) lifecycleScope.launch { channelRepository.refreshAllChannels() }
		}

	private var shouldCheckAllFilesAccess = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		onboardingManager = OnboardingManager(this)
		showOnboarding = !onboardingManager.isCompleted()

		handleIntent(intent)

		setContent {
			AppBase(
				showOnboarding = showOnboarding,
				pendingDeepLink = pendingDeepLink,
				onOnboardingComplete = {
					showOnboarding = false
					onboardingManager.markCompleted()
				},
				onDeepLinkHandled = { pendingDeepLink = null }
			)
		}

		if (!showOnboarding) {
			validateDefaultLauncher()
		}

		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.RESUMED) {
				Timber.d("LauncherActivity: Starting refresh of all data")

				// Delay clearing crash history to allow crash loop detection to work
				// Only clear if app has been running successfully for a few seconds
				kotlinx.coroutines.delay(5000)
				CrashHandler.clearCrashHistory(this@LauncherActivity)
				Timber.d("LauncherActivity: Crash history cleared after successful startup")

				try {
					Timber.d("LauncherActivity: Calling refreshAllApplications")
					appRepository.refreshAllApplications()
					Timber.d("LauncherActivity: refreshAllApplications completed")
				} catch (e: Exception) {
					Timber.e(e, "LauncherActivity: Error refreshing applications")
				}
				try {
					inputRepository.refreshAllInputs()
				} catch (e: Exception) {
					Timber.e(e, "LauncherActivity: Error refreshing inputs")
				}
				try {
					channelRepository.refreshAllChannels()
				} catch (e: Exception) {
					Timber.e(e, "LauncherActivity: Error refreshing channels")
				}
				Timber.d("LauncherActivity: All refresh operations completed")
			}
		}
	}

	override fun onResume() {
		super.onResume()

		// Do not auto-prompt permissions while onboarding is active
		if (showOnboarding || !onboardingManager.isCompleted()) {
			return
		}

		// Request missing permissions (excluding storage on Android 11+ which needs special handling)
		val missingPermissions = PERMISSIONS
			.filter { permission ->
				// On Android 11+, WRITE_EXTERNAL_STORAGE is not requestable normally
				// We need MANAGE_EXTERNAL_STORAGE instead
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && permission == PERMISSION_WRITE_STORAGE) {
					return@filter false
				}
				checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
			}
			.toTypedArray()
		if (missingPermissions.isNotEmpty()) permissionsLauncher.launch(missingPermissions)

		// For Android 11+, check and request MANAGE_EXTERNAL_STORAGE if needed
		// Note: This requires user to go to settings, so we don't auto-request it
		// But we check if it's granted for backup functionality
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			if (!Environment.isExternalStorageManager()) {
				// Log that all files access is not granted - user can enable it via backup dialog
				Timber.d("MANAGE_EXTERNAL_STORAGE permission not granted - backup will prompt user")
			}
		}
	}

	private fun validateDefaultLauncher() {
		if (!defaultLauncherHelper.isDefaultLauncher() && defaultLauncherHelper.canRequestDefaultLauncher()) {
			val intent = defaultLauncherHelper.requestDefaultLauncherIntent()
			if (intent != null) {
				shouldCheckAllFilesAccess = true
				@Suppress("DEPRECATION")
				startActivityForResult(intent, REQUEST_DEFAULT_LAUNCHER)
			}
		}
	}

	@Deprecated("Deprecated in Java")
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == REQUEST_DEFAULT_LAUNCHER) {
			// User returned from default launcher selection
			// Check if we need to prompt for all files access
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && shouldCheckAllFilesAccess) {
				shouldCheckAllFilesAccess = false
				if (!Environment.isExternalStorageManager()) {
					showAllFilesAccessDialog()
				}
			}
		}
	}

	private fun showAllFilesAccessDialog() {
		androidx.appcompat.app.AlertDialog.Builder(this)
			.setTitle(getString(R.string.backup_permission_title))
			.setMessage(getString(R.string.backup_permission_message))
			.setPositiveButton(getString(R.string.backup_permission_open_settings)) { _, _ ->
				// Open app settings where user can manage permissions
				val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
					data = "package:$packageName".toUri()
				}
				startActivity(intent)
			}
			.setNegativeButton(getString(R.string.close)) { dialog, _ ->
				dialog.dismiss()
			}
			.setCancelable(false)
			.show()
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		handleIntent(intent)
		if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
			focusController.requestFocusReset()
		}
	}

	private fun handleIntent(intent: Intent) {
		when (intent.action) {
			Intent.ACTION_VIEW -> {
				intent.data?.let { uri ->
					DeepLinkHandler.parse(uri)?.let { deepLink ->
						Timber.d("DeepLink received: $deepLink")
						pendingDeepLink = deepLink
					}
				}
			}
		}
	}

	@SuppressLint("RestrictedApi")
	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		// Developer mode test crash: Press "9" key 5 times within 10 seconds
		if (keyCode == KeyEvent.KEYCODE_9 && settingsRepository.developerMode.value) {
			val currentTime = System.currentTimeMillis()
			val triggerCount = LauncherConstants.Developer.TEST_CRASH_TRIGGER_COUNT
			if (currentTime - lastTestCrashPressTime > TEST_CRASH_TIMEOUT_MS) {
				testCrashPressCount = 0
			}
			lastTestCrashPressTime = currentTime
			testCrashPressCount++
			Timber.d("Test crash trigger: press $testCrashPressCount (requires $triggerCount)")
			if (testCrashPressCount >= triggerCount) {
				Timber.w("Triggering test crash")
				throw RuntimeException("Test crash triggered by developer mode (key 9 pressed $testCrashPressCount times)")
			}
			return true
		}

		// Map "menu" key to a long press on the dpad because the compose TV library doesn't do that yet
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			event.startTracking()
			val longPressEvent = KeyEvent(
				SystemClock.uptimeMillis(),
				SystemClock.uptimeMillis(),
				KeyEvent.ACTION_DOWN,
				KeyEvent.KEYCODE_DPAD_CENTER,
				1
			)
			return dispatchKeyEvent(longPressEvent)
		}

		return super.onKeyDown(keyCode, event)
	}
}
