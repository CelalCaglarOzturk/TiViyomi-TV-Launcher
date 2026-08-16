plugins {
	alias(libs.plugins.android.app)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.sqldelight)
	alias(libs.plugins.kotlin.serialization)
}

kotlin {
	jvmToolchain(libs.versions.java.jdk.get().toInt())
}

android {
	namespace = "dev.mudrock.tiviyomitvlauncher"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetSdk = libs.versions.android.targetSdk.get().toInt()

		applicationId = "dev.mudrock.TiViyomiTVLauncher"
		versionCode = 1_00_00
		versionName = "1.0.0"
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
		debug {
			isMinifyEnabled = false
		}
	}

	buildFeatures {
		buildConfig = true
		compose = true
	}

	lint {
		warningsAsErrors = false
		abortOnError = false
		
		// Check for missing translations
		disable += setOf(
			"ExtraTranslation",  // Allow translations without base language strings
			"DuplicateStrings"   // Allow duplicate strings (common in different contexts)
		)
	}
}

composeCompiler {
	metricsDestination = layout.buildDirectory.dir("compose_metrics")
	reportsDestination = layout.buildDirectory.dir("compose_reports")
	stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_stability_config.conf"))
}

sqldelight {
	databases {
		create("Database") {
			packageName.set("dev.mudrock.tiviyomitvlauncher.data.sqldelight")
			generateAsync.set(false)
		}
	}
}

dependencies {
	// System
	implementation(libs.bundles.androidx.core)
	implementation(libs.bundles.koin)
	implementation(libs.androidx.tvprovider)
	implementation(libs.androidx.profileinstaller)
	implementation(libs.timber)

	// Data
	implementation(libs.bundles.sqldelight)
	implementation(libs.kotlinx.serialization.json)

	// UI
	implementation(libs.bundles.androidx.compose)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.navigation3.ui)
	implementation(libs.androidx.palette)
	implementation(libs.androidx.savedstate)
	implementation(libs.androidx.tv.material)
	implementation(libs.material.icons.extended)
	implementation(libs.coil.compose)
	debugImplementation(libs.androidx.compose.ui.tooling)

	// Testing
	testImplementation(libs.bundles.testing.unit)
	androidTestImplementation(libs.bundles.testing.instrumentation)
}
