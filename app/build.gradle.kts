import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

// Release signing credentials.
// Local dev: read from keystore.properties (gitignored, never committed).
// CI: read from environment variables (populated from GitHub Secrets).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingProp(propKey: String, envKey: String): String =
    keystoreProperties.getProperty(propKey) ?: System.getenv(envKey) ?: ""

val releaseStoreFilePath = signingProp("storeFile", "RELEASE_STORE_FILE")
val releaseStorePassword = signingProp("storePassword", "RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingProp("keyAlias", "RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingProp("keyPassword", "RELEASE_KEY_PASSWORD")
val hasReleaseSigningConfig = releaseStoreFilePath.isNotBlank() &&
    releaseStorePassword.isNotBlank() &&
    releaseKeyAlias.isNotBlank() &&
    releaseKeyPassword.isNotBlank()

detekt {
    config.setFrom(rootDir.resolve("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

android {
    namespace = "com.example.mdpdf"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.mdpdf"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0-beta"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = file(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigningConfig) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "No release signing credentials found (keystore.properties or " +
                        "RELEASE_STORE_FILE/RELEASE_STORE_PASSWORD/RELEASE_KEY_ALIAS/" +
                        "RELEASE_KEY_PASSWORD env vars). Falling back to debug signing " +
                        "— this APK is NOT suitable for distribution."
                )
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)


    // AndroidX
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Markdown parsing
    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.gfm.tables)
    implementation(libs.commonmark.ext.gfm.strikethrough)
    implementation(libs.commonmark.ext.task.list.items)

    // DocumentFile for SAF tree access
    implementation(libs.documentfile)

    // Debug
    debugImplementation(libs.compose.ui.tooling)

    // Test
        testImplementation(libs.junit)
        testImplementation(libs.robolectric)
        testImplementation(libs.truth)
        testImplementation(libs.compose.ui.test.junit4)
        testImplementation(libs.compose.ui.test)
        testImplementation(libs.coroutines.test)
        testImplementation("androidx.arch.core:core-testing:2.2.0")

        androidTestImplementation(libs.androidx.espresso.core)
            androidTestImplementation(libs.androidx.junit)
            androidTestImplementation(libs.compose.ui.test.junit4)
            androidTestImplementation(libs.compose.ui.test)
            androidTestImplementation("androidx.test:rules:1.5.0")
            androidTestImplementation("androidx.test:core:1.5.0")
}
