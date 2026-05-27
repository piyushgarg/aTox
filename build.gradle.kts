plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlinKsp) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

tasks.register("clean").configure {
    delete("build")
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    dependencies {
        add("detektPlugins", "io.nlopez.compose.rules:detekt:0.3.13")
    }

    detekt {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("detekt.yml"))
        baseline = file("detekt-baseline.xml")
        source.setFrom(files("src/main/kotlin", "src/test/kotlin"))
    }

    plugins.withType<org.gradle.language.base.plugins.LifecycleBasePlugin> {
        tasks.named("check") {
            dependsOn(":checkFileLength")
        }
    }
}

tasks.register("checkFileLength") {
    group = "verification"
    description = "Checks that no Kotlin file exceeds 400 lines."
    doLast {
        val maxLines = 400
        val excludedFiles = listOf(
            "SettingsScreen.kt",
            "GroupEventHandler.kt",
            "Theme.kt",
            "NativeTox.kt",
            "ToxWrapper.kt",
            "ToxRuntime.kt",
            "ChatScreen.kt",
            "GroupChatScreen.kt",
            "AToxNavGraph.kt",
            "ChatInputBar.kt",
            "MessageBubble.kt",
            "UserProfileScreen.kt",
            "GroupManager.kt",
            "FileTransferManager.kt"
        )
        val violations = mutableListOf<String>()
        fileTree(projectDir) {
            include("**/*.kt")
            exclude("**/build/**")
            exclude("**/test/**")
            exclude("**/androidTest/**")
            exclude("**/commonTest/**")
            exclude("**/jvmTest/**")
        }.forEach { file ->
            if (file.name !in excludedFiles) {
                val lineCount = file.readLines().size
                if (lineCount > maxLines) {
                    violations.add("${file.absolutePath} ($lineCount lines)")
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("The following Kotlin files exceed the $maxLines-line limit:\n" + violations.joinToString("\n"))
        }
    }
}
