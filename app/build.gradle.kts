plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    ndkVersion = "25.2.9519653"
    namespace = "com.adrj.music"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.adrj.music"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    buildTypes {
        debug {
            // FIX PESO: R8 + shrink para reducir APK
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ""
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xjvm-default=all",
            "-Xinline-classes",
            "-Xallow-unstable-dependencies"
        )
    }

    buildFeatures { compose = true }
    
    // FIX FLUIDEZ: baseline profiles para AOT
    // (Android compila el código crítico a nativo en primer arranque)
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += listOf(
                "**/libpython.zip.so",
                "**/libffmpeg.zip.so",
                "**/libpython.so",
                "**/libffmpeg.so"
            )
            excludes += listOf(
                "lib/armeabi-v7a/**",
                "lib/x86/**",
                "lib/x86_64/**"
            )
        }
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        resources.excludes.add("/META-INF/LICENSE*")
        resources.excludes.add("/META-INF/NOTICE*")
        resources.excludes.add("/META-INF/*.kotlin_module")
        resources.excludes.add("/META-INF/*.version")
        resources.excludes.add("kotlin-tooling-metadata.json")
        resources.excludes.add("DebugProbesKt.bin")
        resources.excludes.add("kotlin/**")
        resources.excludes.add("/META-INF/DEPENDENCIES")
        resources.excludes.add("/META-INF/LICENSE.md")
        resources.excludes.add("/META-INF/LICENSE-notice.md")
        resources.excludes.add("/META-INF/INDEX.LIST")
        resources.excludes.add("/META-INF/versions/**")
        resources.excludes.add("**/*.kotlin_metadata")
        resources.excludes.add("**/*.kotlin_builtins")
    }
}

dependencies {
    // Backend embebido
    implementation("io.github.deniscerri.youtubedl-android:library:0.19.0")
    implementation("org.apache.commons:commons-compress:1.26.1")
    implementation("commons-io:commons-io:2.16.1")
    implementation("io.github.deniscerri.youtubedl-android:ffmpeg:0.19.0")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.0.4")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // Media3
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coil (portadas) + Palette
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
