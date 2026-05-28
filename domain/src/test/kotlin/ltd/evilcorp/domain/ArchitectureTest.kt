package ltd.evilcorp.domain

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class ArchitectureTest {

    private fun getProjectScope() = Konsist.scopeFromDirectory("domain") + Konsist.scopeFromDirectory("core") + Konsist.scopeFromDirectory("atox")

    @Test
    fun `project architecture should conform to strict Clean Architecture rules`() {
        getProjectScope()
            .assertArchitecture {
                val domain = Layer("Domain", "ltd.evilcorp.domain..")
                val core = Layer("Core", "ltd.evilcorp.core..")
                val ui = Layer("UI", "ltd.evilcorp.atox.ui..")
                val appInfrastructure = Layer("AppInfrastructure", "ltd.evilcorp.atox.infrastructure..")

                domain.dependsOnNothing()
                core.dependsOn(domain)
                ui.dependsOn(domain) // CRITICAL: Presentation/UI depends ONLY on Domain and is unaware of Core!
                appInfrastructure.dependsOn(domain, core) // Infrastructure/DI can depend on Core for binding
            }
    }

    @Test
    fun `domain layer must be KMP compliant and completely independent of any JVM and OS specific IO`() {
        Konsist.scopeFromDirectory("domain")
            .imports
            .assertTrue { import ->
                // Block any JVM disk operations and system IO classes in business logic
                !import.name.startsWith("java.io.File") &&
                !import.name.startsWith("java.io.RandomAccessFile") &&
                !import.name.startsWith("java.io.FileOutputStream") &&
                !import.name.startsWith("java.io.FileInputStream") &&
                !import.name.startsWith("java.nio") &&
                // Block any platform-dependent Android libraries
                !import.name.startsWith("android.") &&
                !import.name.startsWith("androidx.")
            }
    }

    @Test
    fun `domain layer must have zero database and repository implementation dependencies`() {
        Konsist.scopeFromDirectory("domain")
            .imports
            .assertTrue { import ->
                !import.name.startsWith("androidx.room") &&
                !import.name.startsWith("ltd.evilcorp.core.db") &&
                !import.name.startsWith("ltd.evilcorp.core.repository")
            }
    }

    @Test
    fun `presentation layer including all subpackages must never access Room database or DAOs directly`() {
        Konsist.scopeFromProduction("atox")
            .files
            .filter { file ->
                val pkg = file.packagee?.name ?: ""
                pkg.startsWith("ltd.evilcorp.atox.ui") || pkg.startsWith("ltd.evilcorp.atox.appearance") || file.name == "MainActivity"
            }
            .flatMap { it.imports }
            .assertTrue { import ->
                !import.name.startsWith("ltd.evilcorp.core.db") &&
                !import.name.startsWith("androidx.room")
            }
    }

    @Test
    fun `presentation layer must never import concrete Core Repositories directly`() {
        Konsist.scopeFromProduction("atox")
            .files
            .filter { file ->
                val pkg = file.packagee?.name ?: ""
                pkg.startsWith("ltd.evilcorp.atox.ui") || pkg.startsWith("ltd.evilcorp.atox.appearance") || file.name == "MainActivity"
            }
            .flatMap { it.imports }
            .assertTrue { import ->
                !import.name.startsWith("ltd.evilcorp.core.repository")
            }
    }

    @Test
    fun `core layer must not contain any Compose or UI components`() {
        Konsist.scopeFromProduction("core")
            .imports
            .assertTrue { import ->
                !import.name.startsWith("androidx.compose") &&
                !import.name.startsWith("android.view") &&
                !import.name.startsWith("android.widget")
            }
    }

    @Test
    fun `use cases must reside strictly in usecase package and have UseCase suffix`() {
        Konsist.scopeFromProduction("domain")
            .classes()
            .filter { it.name.endsWith("UseCase") }
            .assertTrue { it.resideInPackage("ltd.evilcorp.domain.features..usecase") }
    }

    @Test
    fun `no file should exceed 400 lines`() {
        Konsist.scopeFromProduction("domain")
            .plus(Konsist.scopeFromProduction("core"))
            .plus(Konsist.scopeFromProduction("atox"))
            .files
            .assertTrue { file ->
                val lines = file.text.lines().size
                val ok = lines <= 400
                if (!ok) {
                    println("Violating file: ${file.path} (${lines} lines)")
                }
                ok
            }
    }

    @Test
    fun `no file should contain Russian comments`() {
        Konsist.scopeFromProduction("domain")
            .plus(Konsist.scopeFromProduction("core"))
            .plus(Konsist.scopeFromProduction("atox"))
            .files
            .assertTrue { file ->
                val hasRussianComment = file.text.lines().any { line ->
                    val trimmed = line.trim()
                    (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*") || trimmed.startsWith("/**")) &&
                            trimmed.contains(Regex("[а-яёА-ЯЁ]"))
                }
                if (hasRussianComment) {
                    println("Violating file with Russian comment: ${file.path}")
                }
                !hasRussianComment
            }
    }

    @Test
    fun `repository interfaces must reside strictly in repository package and follow strict naming rules`() {
        Konsist.scopeFromDirectory("domain")
            .interfaces()
            .filter { it.name.endsWith("Repository") }
            .assertTrue {
                it.resideInPackage("..repository..") &&
                it.name.startsWith("I")
            }
    }

    @Test
    fun `use cases must have a constructor annotated with Inject`() {
        Konsist.scopeFromDirectory("domain")
            .classes()
            .filter { it.name.endsWith("UseCase") }
            .assertTrue { useCase ->
                useCase.hasConstructor { constructor ->
                    constructor.hasAnnotation { annotation ->
                        annotation.name == "Inject"
                    }
                }
            }
    }

    @Test
    fun `domain layer files should reside only inside core or features packages`() {
        Konsist
            .scopeFromProduction("domain")
            .files
            .assertTrue { file ->
                val packageFqName = file.packagee?.name ?: ""
                packageFqName.startsWith("ltd.evilcorp.domain.core") ||
                packageFqName.startsWith("ltd.evilcorp.domain.features")
            }
    }

    @Test
    fun `JNI delegates and runtime components must never use raw synchronized(this) locks`() {
        Konsist.scopeFromProduction("core")
            .files
            .filter { file ->
                val pkg = file.packagee?.name ?: ""
                pkg.startsWith("ltd.evilcorp.core.tox.runtime")
            }
            .assertTrue { file ->
                !file.text.contains("synchronized(this)")
            }
    }
}
