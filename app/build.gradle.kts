import java.util.Properties

/**
 * Every developer registers their own Spotify app, because the dashboard ties an app to one package
 * name and signing fingerprint. The id is not a secret - it ships in the APK either way - it just
 * differs per person, which is why it is not checked in.
 */
val spotifyClientId: String = run {
    System.getenv("SPOTIFY_CLIENT_ID")?.takeIf { it.isNotBlank() }?.let { return@run it }

    val localProperties = rootProject.file("local.properties")
    if (localProperties.exists()) {
        val properties = Properties().apply { localProperties.inputStream().use(::load) }
        properties.getProperty("spotify.clientId")?.takeIf { it.isNotBlank() }?.let { return@run it }
    }

    throw GradleException(
        """
        Missing Spotify client id.

        Add this line to local.properties (that file is never checked in):

            spotify.clientId=<your client id>

        or set the SPOTIFY_CLIENT_ID environment variable, for example on CI.

        You get an id at https://developer.spotify.com/dashboard by registering an app with
        package name dev.hyport.hitster, the SHA-1 of your signing certificate, and the
        redirect URI digital-hitster-app://spotify-callback.
        """.trimIndent()
    )
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    // Namespace and applicationId differ on purpose: the namespace is only the package of the
    // generated R class and the Kotlin sources, while the applicationId is the identity of the
    // installed app. Spotify and Play know the latter, so only that one had to leave com.example.
    namespace = "com.example.hitster"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.hyport.hitster"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "2.3.0"

        manifestPlaceholders.putAll(
            mapOf(
                "redirectSchemeName" to "digital-hitster-app",
                "redirectHostName" to "spotify-callback"
            )
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$spotifyClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.gson)
    implementation(libs.spotify.android.auth)
    implementation(files("libs/spotify-app-remote-release-0.8.0.aar"))
    implementation(libs.squareup.okhttp3)
    implementation(libs.koin)
    implementation(libs.koinCompose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Android Studio Preview support
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // Testing
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(kotlin("test"))
}