plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

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
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            signingConfig = signingConfigs.getByName("debug")
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
