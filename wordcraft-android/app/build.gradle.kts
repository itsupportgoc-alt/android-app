import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.wordcraft.app"
    compileSdk = 35

    // Local release keystore (generated on this machine); release stays unsigned without it.
    val releaseKs = rootProject.file("release.keystore")
    if (releaseKs.exists()) {
        signingConfigs.create("release") {
            storeFile = releaseKs
            storePassword = "wordcraft123"
            keyAlias = "wordcraft"
            keyPassword = "wordcraft123"
        }
    }

    defaultConfig {
        applicationId = "com.wordcraft.android"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.6.1"
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseKs.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Copies the cross-compiled WordCraft engine (rust-core → libwordcraft_core.so) into jniLibs.
val rustCoreSo = rootProject.layout.projectDirectory.file("rust-core/target/aarch64-linux-android/release/libwordcraft_core.so")
val jniLibsDir = layout.projectDirectory.dir("src/main/jniLibs/arm64-v8a")

tasks.register<Copy>("copyRustLib") {
    onlyIf { rustCoreSo.asFile.exists() }
    from(rustCoreSo)
    into(jniLibsDir)
}

tasks.named("preBuild") {
    dependsOn("copyRustLib")
}

dependencies {
    implementation(project(":editor"))
    implementation(project(":document-core"))
    implementation(project(":storage"))
    implementation(project(":ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
