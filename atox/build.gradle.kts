@file:Suppress("UnstableApiUsage", "PlaySdkIndexNonCompliant", "AndroidSdkIndex", "PlaySdkIndex")

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlinKsp)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_stability.txt"))
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
        versionName = "1.0.0"
        testInstrumentationRunner = "ltd.evilcorp.atox.HiltTestRunner"
    }
    signingConfigs {
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = file("debug.keystore")
            storePassword = "android"
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }
    lint {
        disable += setOf("GoogleAppIndexingWarning", "MissingTranslation", "LocalContextGetResourceValueCall", "MissingPermission", "PlaySdkIndexNonCompliant")
        error += "HardcodedText"
    }
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
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
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.compose.material3.window.size)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

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
    val hiltCompiler = libs.google.hilt.android.compiler
    ksp(hiltCompiler)
    kspAndroidTest(hiltCompiler)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.biometric)

    implementation(libs.nayuki.qrcodegen)

    implementation(libs.square.picasso)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    val playServicesAuth = libs.google.play.services.auth
    implementation(playServicesAuth)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive) {
        exclude(group = "org.apache.httpcomponents")
    }

    val hiltTesting = libs.google.hilt.android.testing
    debugImplementation(libs.square.leakcanary)
    debugImplementation(hiltTesting)

    val testJunit = kotlin("test-junit")
    val composeUiTestJunit4 = libs.androidx.compose.ui.test.junit4

    testImplementation(testJunit)
    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.konsist.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.turbine)
    testImplementation(libs.test.mockk)
    testImplementation(composeUiTestJunit4)
    testImplementation(libs.test.roborazzi)
    testImplementation(libs.test.roborazzi.compose)

    androidTestImplementation(testJunit)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(libs.test.espresso.contrib)
    androidTestImplementation(libs.test.junit.ext)
    androidTestImplementation(composeUiTestJunit4)
    androidTestImplementation(libs.hamcrest)
    androidTestImplementation(hiltTesting)

    modules {
        module("com.google.guava:listenablefuture") {
            replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }
}
tasks.withType<Test> {
    testLogging {
        events("started", "passed", "skipped", "failed")
    }
}

configurations.configureEach {
    resolutionStrategy {
        force(libs.test.core)
        force(libs.test.monitor)
        force(libs.test.runner)
        force(libs.test.rules)
        force(libs.test.junit.ext)
    }
}
