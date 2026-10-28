import java.util.Base64
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// AGP 9 applies Kotlin itself: org.jetbrains.kotlin.android must NOT be listed here (it fails).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// ---------------------------------------------------------------------------------------------
// Integration keys — read from local.properties (never committed); CI may pass them as env vars.
// ---------------------------------------------------------------------------------------------
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(name: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() } ?: localProperties.getProperty(name, "").trim()

// ---------------------------------------------------------------------------------------------
// Firebase config. google-services.json never lands in the repo: it arrives as GOOGLE_SERVICES_JSON
// (base64) locally or from CI secrets. When it is absent we write a stub so the build still
// produces a signed, installable APK — one WITHOUT working push. CI must fail on the missing
// secret instead (a push-less build looks healthy and is caught weeks later).
// ---------------------------------------------------------------------------------------------
fun stubGoogleServicesJson(packageName: String): String = """
{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "stub-chickie-runway",
    "storage_bucket": "stub-chickie-runway.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
        "android_client_info": { "package_name": "$packageName" }
      },
      "oauth_client": [],
      "api_key": [ { "current_key": "AIzaSyStubStubStubStubStubStubStubStub00" } ],
      "services": { "appinvite_service": { "other_platform_oauth_client": [] } }
    }
  ],
  "configuration_version": "1"
}
""".trimIndent()

run {
    val target = project.file("google-services.json")
    val encoded = secret("GOOGLE_SERVICES_JSON")
    when {
        encoded.isNotBlank() -> runCatching {
            target.writeBytes(Base64.getDecoder().decode(encoded))
        }.onFailure {
            logger.warn("GOOGLE_SERVICES_JSON is not valid base64; leaving google-services.json untouched.")
        }
        !target.exists() -> {
            logger.warn("GOOGLE_SERVICES_JSON not set - writing a stub google-services.json. Push will NOT work in this build.")
            target.writeText(stubGoogleServicesJson("com.chickierunway.game"))
        }
        else -> Unit
    }
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
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.chickierunway.game"
        minSdk = 24
        targetSdk = 37
        versionCode = 4
        versionName = "1.0.3"
        vectorDrawables { useSupportLibrary = true }

        // buildConfigField pastes the value into generated Java verbatim, hence the escaped quotes.
        buildConfigField("String", "ADJUST_APP_TOKEN", "\"${secret("ADJUST_APP_TOKEN")}\"")
        buildConfigField("String", "OFFER_BASE_URL", "\"${secret("OFFER_BASE_URL")}\"")
        // aso | prily — the Keitaro campaign group decides the link shape (sub1 in path for prily).
        buildConfigField("String", "OFFER_ROUTING", "\"${secret("OFFER_ROUTING").ifBlank { "aso" }}\"")
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


    buildFeatures {
        compose = true
        buildConfig = true
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
    implementation(libs.androidx.appcompat)

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

    implementation(libs.adjust)
    implementation(libs.installreferrer)
    implementation(libs.play.services.ads.identifier)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.webkit)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.serialization.json)
}

// AGP 9 registers the Kotlin extension itself; the old android { kotlinOptions {} } is gone.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
