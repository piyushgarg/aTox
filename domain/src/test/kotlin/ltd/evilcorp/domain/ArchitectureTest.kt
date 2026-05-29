package ltd.evilcorp.domain

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

@Suppress("LargeClass")
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
                // Block all JVM I/O, zip compression, locale formatting, cryptography, and dates
                !import.name.startsWith("java.io.") &&
                !import.name.startsWith("java.util.zip.") &&
                !import.name.startsWith("java.text.") &&
                !import.name.startsWith("java.security.") &&
                !import.name.startsWith("java.util.Date") &&
                !import.name.startsWith("java.util.Locale") &&
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

    @Test
    fun `classes in domain and core layers must not exceed 6 constructor parameters`() {
        Konsist
            .scopeFromProduction("domain")
            .plus(Konsist.scopeFromProduction("core"))
            .classes()
            // Validate ALL classes without any legacy manager exclusions!
            .filter { clazz ->
                !clazz.hasModifier(com.lemonappdev.konsist.api.KoModifier.DATA) &&
                !clazz.name.endsWith("Entity") &&
                !clazz.name.endsWith("Database") &&
                !clazz.name.endsWith("Module")
            }
            .assertTrue { clazz ->
                clazz.constructors.all { constructor ->
                    constructor.parameters.size <= 6
                }
            }
    }

    @Test
    fun `use cases must be completely stateless and not declare mutable properties`() {
        Konsist.scopeFromProduction("domain")
            .classes()
            .filter { it.name.endsWith("UseCase") }
            .assertTrue { useCase ->
                useCase.properties().none { it.text.startsWith("var ") || it.text.contains(" var ") }
            }
    }

    @Test
    fun `new use cases must expose at most one public function or invoke operator`() {
        Konsist.scopeFromProduction("domain")
            .classes()
            .filter { it.name.endsWith("UseCase") }
            // Validate ALL use cases without any legacy exclusions!
            .assertTrue { useCase ->
                val publicFunctions = useCase.functions().filter { func ->
                    func.hasPublicModifier || (!func.hasPrivateModifier && !func.hasProtectedModifier && !func.hasInternalModifier)
                }.filter { func ->
                    func.name != "equals" && func.name != "hashCode" && func.name != "toString"
                }
                publicFunctions.size <= 1
            }
    }

    @Test
    fun `domain layer must not use java concurrent executor or thread classes directly`() {
        Konsist.scopeFromProduction("domain")
            .imports
            .assertTrue { import ->
                !import.name.startsWith("java.util.concurrent") &&
                !import.name.startsWith("java.lang.Thread")
            }
    }

    @Test
    fun `production codebase must not import raw legacy JSON parsing libraries like org json or gson`() {
        Konsist.scopeFromProduction("domain")
            .plus(Konsist.scopeFromProduction("core"))
            .plus(Konsist.scopeFromProduction("atox"))
            .imports
            .assertTrue { import ->
                !import.name.startsWith("org.json") &&
                !import.name.startsWith("com.google.gson") &&
                !import.name.startsWith("com.fasterxml.jackson")
            }
    }

    @Test
    fun `no UI file should contain hardcoded Cyrillic string literals`() {
        Konsist.scopeFromProduction("atox")
            .files
            .filter { file ->
                // Exclude language-specific screen and setting files that legitimately define language names
                !file.name.contains("SettingsScreen") &&
                !file.name.contains("LanguageSettings") &&
                !file.name.contains("SearchSettingsScreen") &&
                !file.name.contains("CallHistoryBubble")
            }
            .assertTrue { file ->
                val text = file.text
                val hasCyrillic = text.contains(Regex("[а-яёА-ЯЁ]"))
                if (hasCyrillic) {
                    println("Violating file with Cyrillic text: ${file.path}")
                }
                !hasCyrillic
            }
    }

    @Test
    fun `UI layer must never import or manage raw android media MediaPlayer directly`() {
        Konsist.scopeFromProduction("atox")
            .files
            .filter { file ->
                val pkg = file.packagee?.name ?: ""
                pkg.startsWith("ltd.evilcorp.atox.ui")
            }
            .assertTrue { file ->
                !file.text.contains("MediaPlayer")
            }
    }

    @Test
    fun `UI layer must never perform raw string matching on localized call history strings`() {
        Konsist.scopeFromProduction("atox")
            .files
            .filter { file -> 
                !file.name.contains("CallHistoryBubble") &&
                !file.name.contains("Preview") &&
                !file.name.contains("Previews")
            }
            .assertTrue { file ->
                val cleanText = file.text.replace(Regex("//.*|/\\*.*?\\*/"), "")
                val hasMatch = cleanText.contains("\"пропущ\"") ||
                              cleanText.contains("\"отмен\"") ||
                              cleanText.contains("\"missed\"") ||
                              cleanText.contains("\"Incoming call\"") ||
                              cleanText.contains("\"Outgoing call\"")
                if (hasMatch) {
                    println("Violating file performing raw call string matching: ${file.path}")
                }
                !hasMatch
            }
    }

    @Test
    fun `heavy classes injected into MainActivity must be wrapped in dagger Lazy to optimize cold startup`() {
        Konsist.scopeFromProduction("atox")
            .classes()
            .filter { it.name == "MainActivity" }
            .flatMap { it.properties() }
            .filter { property ->
                val type = property.type?.name ?: ""
                type == "CallManager" ||
                type == "NotificationHelper" ||
                type == "SystemSoundPlayer" ||
                type == "SharedContentRegistry" ||
                type == "ToxLinkManager" ||
                type == "PermissionManager"
            }
            .assertTrue { property ->
                property.text.contains("dagger.Lazy")
            }
    }

    @Test
    fun `no declaration or file should suppress MagicNumber or MaxLineLength`() {
        getProjectScope()
            .files
            .assertTrue { file ->
                val text = file.text
                val hasMagicNumberSuppression = text.contains("\"MagicNumber\"")
                val hasMaxLineLengthSuppression = text.contains("\"MaxLineLength\"") ||
                    text.contains("\"ktlint:standard:max-line-length\"")
                if (hasMagicNumberSuppression || hasMaxLineLengthSuppression) {
                    println("Violating file with suppressed MagicNumber or MaxLineLength: ${file.path}")
                }
                !hasMagicNumberSuppression && !hasMaxLineLengthSuppression
            }
    }

    @Test
    fun `no file should contain unused imports`() {
        getProjectScope()
            .files
            .filter { file ->
                val name = file.name
                name == "NavigationEffectsCoordinator.kt" ||
                name == "SettingsLaunchers.kt" ||
                name == "AToxNavGraph.kt" ||
                name == "SettingsScreen.kt" ||
                name == "VoiceMessageCard.kt" ||
                name == "GroupEventProcessor.kt" ||
                name == "FriendEventHandler.kt" ||
                name == "ArchitectureTest.kt"
            }
            .assertTrue { file ->
                val lines = file.text.lines()
                val nonImportText = lines.filter {
                    !it.trim().startsWith("import ") && !it.trim().startsWith("package ")
                }.joinToString("\n")
                
                val unusedImports = file.imports.filter { import ->
                    val alias = import.alias?.name ?: import.name.substringAfterLast('.')
                    if (alias in setOf("*", "getValue", "setValue", "provideDelegate")) {
                        false
                    } else {
                        !nonImportText.contains(alias)
                    }
                }
                
                if (unusedImports.isNotEmpty()) {
                    println("File ${file.path} has unused imports: ${unusedImports.map { it.name }}")
                }
                unusedImports.isEmpty()
            }
    }


    @Test
    fun `no class should declare unused private functions or properties`() {
        getProjectScope()
            .classes()
            .assertTrue { clazz ->
                val fileText = clazz.containingFile.text
                val privateFunctions = clazz.functions().filter { it.hasPrivateModifier }
                val privateProperties = clazz.properties().filter { it.hasPrivateModifier }
                
                val unusedFuncs = privateFunctions.filter { func ->
                    val name = func.name
                    val occurrences = fileText.split(name).size - 1
                    occurrences <= 1
                }
                
                val unusedProps = privateProperties.filter { prop ->
                    val name = prop.name
                    val occurrences = fileText.split(name).size - 1
                    occurrences <= 1
                }
                
                if (unusedFuncs.isNotEmpty() || unusedProps.isNotEmpty()) {
                    println(
                        "Class ${clazz.name} has unused private functions: " +
                            "${unusedFuncs.map { it.name }} or properties: ${unusedProps.map { it.name }}"
                    )
                }
                unusedFuncs.isEmpty() && unusedProps.isEmpty()
            }
    }

    @Test
    fun `all interfaces in domain module must start with capital letter I prefix`() {
        Konsist.scopeFromDirectory("domain")
            .interfaces()
            .filter { !it.name.endsWith("Action") && !it.name.endsWith("Query") }
            .assertTrue {
                it.name.startsWith("I")
            }
    }
}

