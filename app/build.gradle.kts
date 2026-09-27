plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.kurdishtv.live"
    minSdk = 24
    targetSdk = 36

    // Android refuses to install a build whose versionCode is not at least the
    // installed one, and reports it as a broken update. CI overrides this with the
    // run number; local builds used to fall back to 1, which could never be
    // installed over a published release. kurdishTvVersionCode in
    // gradle.properties is the local floor and must be kept at or above the last
    // published release.
    val ciVersionCode = project.findProperty("versionCode")?.toString()?.toIntOrNull()
    val localVersionCode =
        project.findProperty("kurdishTvVersionCode")?.toString()?.toIntOrNull() ?: 1
    val resolvedVersionCode = ciVersionCode ?: localVersionCode
    versionCode = resolvedVersionCode

    // The human-facing version (Settings → About) is kept separate from versionCode
    // so a CI run number never leaks into the UI as "1.0.36265844407".
    versionName =
        project.findProperty("kurdishTvVersionName")?.toString()?.takeIf { it.isNotBlank() }
            ?: "1.0.$resolvedVersionCode"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      // Priority: a real upload keystore from the environment, then the committed
      // release key, and only then the local debug key.
      //
      // Every build in a chain MUST be signed with the same key. Android refuses to
      // install an update whose signature differs from the installed app
      // (INSTALL_FAILED_UPDATE_INCOMPATIBLE, which surfaces as "App not installed as
      // package conflicts with an existing package"). Previously CI generated a
      // brand-new debug key on every run whenever the base64 key was missing, so
      // each release was signed differently and updates could never install.
      val envKeystore = System.getenv("KEYSTORE_PATH")?.let { file(it) }
      val bundledKeystore = file("${rootDir}/release-key.jks")

      when {
        envKeystore != null && envKeystore.exists() -> {
          storeFile = envKeystore
          storePassword = System.getenv("STORE_PASSWORD")
          keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
          keyPassword = System.getenv("KEY_PASSWORD")
        }
        bundledKeystore.exists() -> {
          storeFile = bundledKeystore
          storePassword = System.getenv("STORE_PASSWORD") ?: "android"
          keyAlias = "upload"
          keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
        }
        else -> {
          rootProject.logger.warn(
            "WARNING: no release keystore found. Falling back to the local debug key, " +
              "which will NOT match previously published builds. Restore " +
              "release-key.jks (see README) or set KEYSTORE_PATH before publishing."
          )
          storeFile = file("${rootDir}/debug.keystore")
          storePassword = "android"
          keyAlias = "androiddebugkey"
          keyPassword = "android"
        }
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      // Lossless PNG optimisation. Safe here because every image is referenced as a
      // drawable/mipmap resource rather than loaded by raw file name.
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "/META-INF/*.version"
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  lint {
    abortOnError = false
    checkReleaseBuilds = false
    ignoreWarnings = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.exoplayer.hls)
  implementation(libs.androidx.media3.datasource.okhttp)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  // "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
