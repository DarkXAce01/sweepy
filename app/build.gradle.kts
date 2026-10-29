import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.darkxace01.sweepy"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.darkxace01.sweepy"
        // Android 11: first version with the system trash (MediaStore.createTrashRequest).
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0-beta.1"
    }

    // Release signing reads keystore.properties at the project root (never committed). Without it,
    // release builds are simply unsigned, so anyone can still build from source.
    val keystoreFile = rootProject.file("keystore.properties")
    val releaseSigning = if (keystoreFile.exists()) {
        val keys = Properties().apply { keystoreFile.inputStream().use(::load) }
        signingConfigs.create("release") {
            storeFile = file(keys.getProperty("storeFile"))
            storePassword = keys.getProperty("storePassword")
            keyAlias = keys.getProperty("keyAlias")
            keyPassword = keys.getProperty("keyPassword")
        }
    } else {
        null
    }

    // "play" = Google Play build, "foss" = F-Droid build (only open-source dependencies).
    // Identical for now; play will later add ML Kit for the smart decks.
    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            // The Play build doesn't advertise where the source lives.
            buildConfigField("boolean", "SHOW_SOURCE_LINK", "false")
        }
        create("foss") {
            dimension = "distribution"
            buildConfigField("boolean", "SHOW_SOURCE_LINK", "true")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            signingConfig = releaseSigning
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Robolectric needs this to emulate Android 16 on Java 21.
            all { it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED") }
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.androidx.exifinterface)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Screenshot tests: render screens on the computer, no phone needed.
    // Record with ./gradlew recordRoborazziPlayDebug — images land in app/build/outputs/roborazzi.
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
