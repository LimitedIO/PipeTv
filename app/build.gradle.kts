plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.limitedio.pipetv"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.limitedio.pipetv"
        minSdk = 21
        targetSdk = 35
        versionCode = (findProperty("vcode") as String? ?: "1").toInt()
        versionName = findProperty("vname") as String? ?: "1.0.0"
    }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/pipetv.jks")
            storePassword = "pipetv"
            keyAlias = "pipetv"
            keyPassword = "pipetv"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false // NewPipeExtractor relies on reflection; keep it intact
            signingConfig = signingConfigs.getByName("release")
        }
        debug { signingConfig = signingConfigs.getByName("release") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    sourceSets["main"].java.srcDir("src/main/kotlin")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.5")
}
