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

    val libs = rootProject.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
    dependencies {
        add("detektPlugins", libs.findLibrary("detekt-compose").get())
    }

    detekt {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("detekt.yml"))
        baseline = file("detekt-baseline.xml")
        source.setFrom(files("src/main/kotlin", "src/test/kotlin"))
        ignoreFailures = false
    }

    tasks.matching { it.name == "assemble" }.configureEach {
        dependsOn(tasks.withType<io.gitlab.arturbosch.detekt.Detekt>())
    }
}

