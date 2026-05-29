plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlinKsp)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
    }
}

android {
    namespace = "ltd.evilcorp.atox"
    compileSdk = libs.versions.sdk.target.get().toInt()
    ndkVersion = libs.versions.ndk.get()
    defaultConfig {
        applicationId = "ltd.evilcorp.atox"
        minSdk = libs.versions.sdk.min.get().toInt()
        targetSdk = libs.versions.sdk.target.get().toInt()
        versionCode = 14
        versionName = "0.8.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
    signingConfigs {
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = file("debug.keystore")
            storePassword = "android"
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }
    lint {
        disable += setOf("GoogleAppIndexingWarning", "MissingTranslation", "LocalContextGetResourceValueCall", "MissingPermission")
        error += "HardcodedText"
    }
}

dependencies {
    lintChecks(platform(libs.androidx.compose.bom))
    lintChecks(libs.androidx.compose.material3.lint)

    implementation(project(":core"))
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment)

    implementation(libs.google.android.material)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation("androidx.compose.material3:material3-window-size-class")
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.preference)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.google.dagger.core)
    ksp(libs.google.dagger.compiler)
    implementation(libs.google.hilt.android)
    ksp(libs.google.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.biometric)

    implementation(libs.nayuki.qrcodegen)

    implementation(libs.square.picasso)

    debugImplementation(libs.square.leakcanary)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.konsist.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)

    androidTestImplementation(kotlin("test-junit"))
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(libs.test.espresso.contrib)
    androidTestImplementation(libs.test.junit.ext)
    kspAndroidTest(libs.google.dagger.compiler)

    modules {
        module("com.google.guava:listenablefuture") {
            replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }
}
