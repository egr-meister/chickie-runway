plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// ---------------------------------------------------------------------------------------------
// Release signing — read ONLY from environment variables. There is no fallback to debug signing.
// ---------------------------------------------------------------------------------------------
val releaseKeystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
val releaseKeystorePassword: String? = System.getenv("ANDROID_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
val releaseKeyAlias: String? = System.getenv("ANDROID_KEY_ALIAS")?.takeIf { it.isNotBlank() }
val releaseKeyPassword: String? = System.getenv("ANDROID_KEY_PASSWORD")?.takeIf { it.isNotBlank() }

val missingSigningVars: List<String> = buildList {
    if (releaseKeystorePath == null) add("ANDROID_KEYSTORE_PATH")
    if (releaseKeystorePassword == null) add("ANDROID_KEYSTORE_PASSWORD")
    if (releaseKeyAlias == null) add("ANDROID_KEY_ALIAS")
    if (releaseKeyPassword == null) add("ANDROID_KEY_PASSWORD")
}
val releaseKeystoreFile: File? = releaseKeystorePath?.let { file(it) }
val hasReleaseSigning: Boolean = missingSigningVars.isEmpty() && releaseKeystoreFile?.isFile == true

android {
    namespace = "com.chickierunway.game"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.chickierunway.game"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = releaseKeystoreFile
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                storeType = "pkcs12"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            // Staged R8: ship the first verified release NON-minified.
            // After a verified non-minified release, set both to true and re-verify (see README).
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Fail loudly if any release artifact is requested without complete signing configuration.
gradle.taskGraph.whenReady {
    val releaseArtifactPrefixes = listOf("assemble", "bundle", "package", "sign", "validateSigning")
    val wantsReleaseArtifact = allTasks.any { task ->
        task.project == project &&
            task.name.contains("Release") &&
            releaseArtifactPrefixes.any { prefix -> task.name.startsWith(prefix) }
    }
    if (wantsReleaseArtifact && !hasReleaseSigning) {
        val reason = buildString {
            if (missingSigningVars.isNotEmpty()) {
                append("missing environment variables: ${missingSigningVars.joinToString()}")
            } else {
                append("keystore file not found at ANDROID_KEYSTORE_PATH")
            }
        }
        throw GradleException(
            "Release signing is not configured ($reason).\n" +
                "Set ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS and " +
                "ANDROID_KEY_PASSWORD to a real PKCS12 keystore. Release builds are never signed " +
                "with the debug key. For development use assembleDebug."
        )
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.compose.ui.geometry)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime)
    implementation(libs.androidx.navigation.common)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.core)
    implementation(libs.androidx.datastore.core)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.serialization.json)
}
