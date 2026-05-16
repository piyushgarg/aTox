import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
    }
}

android {
    namespace = "ltd.evilcorp.domain"
    compileSdk = libs.versions.sdk.target.get().toInt()
    ndkVersion = libs.versions.ndk.get()
    defaultConfig {
        minSdk = libs.versions.sdk.min.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
}


tasks.register<Exec>("buildNativeDependencies") {
    group = "build"
    description = "Builds native Tox dependencies (libsodium, opus, vpx, toxcore) for aarch64"
    
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val stream = FileInputStream(localPropertiesFile)
        localProperties.load(stream)
        stream.close()
    }
    
    val sdkDir = localProperties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME") ?: ""
    var ndkDir = localProperties.getProperty("ndk.dir") ?: System.getenv("ANDROID_NDK_HOME") ?: ""
    
    // If NDK dir is missing, try to find it in the SDK
    if (ndkDir.isEmpty() && sdkDir.isNotEmpty()) {
        val ndkBaseDir = file("$sdkDir/ndk")
        if (ndkBaseDir.exists()) {
            // Try to find the version from catalog or fallback
            val expectedVersion = libs.versions.ndk.get()
            val expectedDir = file("${ndkBaseDir.absolutePath}/$expectedVersion")
            ndkDir = if (expectedDir.exists()) {
                expectedDir.absolutePath
            } else {
                ndkBaseDir.listFiles()?.filter { it.isDirectory }?.firstOrNull()?.absolutePath ?: ""
            }
        }
    }
    
    val cmakeDir = "$sdkDir/cmake"
    
    // Find the first available cmake version in the SDK
    val cmakeExecutable = file(cmakeDir).listFiles()?.filter { it.isDirectory }?.firstOrNull()?.let { 
        file(it.absolutePath + "/bin/cmake").absolutePath 
    } ?: "cmake"

    workingDir = rootDir
    commandLine(file("${rootDir}/scripts/build-aarch64-linux-android").absolutePath, "-j${Runtime.getRuntime().availableProcessors()}", "release")
    
    environment("ANDROID_NDK_HOME", ndkDir)
    environment("CMAKE", cmakeExecutable)
    environment("LIBSODIUM_VERSION", libs.versions.libsodium.get())
    environment("OPUS_VERSION", libs.versions.opus.get())
    environment("LIBVPX_VERSION", libs.versions.libvpx.get())
    environment("TOXCORE_VERSION", libs.versions.toxcore.get())
    
    // Make sure the script is executable
    doFirst {
        if (!org.gradle.internal.os.OperatingSystem.current().isWindows) {
            ant.withGroovyBuilder {
                "chmod"("file" to file("${rootDir}/scripts/build-aarch64-linux-android").absolutePath, "perm" to "+x")
            }
        }
    }
}

// Ensure native dependencies are built before any build starts
tasks.named("preBuild") {
    dependsOn("buildNativeDependencies")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.javax.inject)
    api(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test-junit"))
    androidTestImplementation(kotlin("test-junit"))
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.junit.ext)
    androidTestImplementation(libs.kotlinx.coroutines.test) {
        // Conflicts with a lot of things due to having embedded "byte buddy" instead of depending on it.
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-debug")
    }

    modules {
        module("com.google.guava:listenablefuture") {
            replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }
}
