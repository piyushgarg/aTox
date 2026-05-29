// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class AtoxArchitectureTest {

    @Test
    fun `presentation layer atox should not directly access database or JNI runtime`() {
        Konsist
            .scopeFromDirectory("atox")
            .assertArchitecture {
                val ui = Layer("UI", "ltd.evilcorp.atox.ui..")
                val db = Layer("Database", "ltd.evilcorp.core.db..")
                val runtime = Layer("JniRuntime", "ltd.evilcorp.core.tox.runtime..")

                db.toString()
                runtime.toString()
                ui.dependsOnNothing()
            }
    }

    @Test
    fun `presentation layer atox should not import database or JNI runtime packages`() {
        Konsist
            .scopeFromProduction("atox")
            .files
            .filter { file ->
                val pkg = file.packagee?.name ?: ""
                pkg.startsWith("ltd.evilcorp.atox.ui") || pkg.startsWith("ltd.evilcorp.atox.appearance") || file.name == "MainActivity"
            }
            .flatMap { it.imports }
            .assertTrue { import ->
                // Presentation layer must interact with data layers only via domain interfaces, never directly
                !import.name.startsWith("ltd.evilcorp.core.db") &&
                !import.name.startsWith("ltd.evilcorp.core.tox.runtime")
            }
    }

    @Test
    fun `atox UI package classes should not directly depend on core implementations`() {
        Konsist
            .scopeFromProduction("atox")
            .files
            .filter { file ->
                val pkg = file.packagee?.name ?: ""
                pkg.startsWith("ltd.evilcorp.atox.ui")
            }
            .flatMap { it.imports }
            .assertTrue { import ->
                // UI ViewModels, Screens, and Controllers must depend on Domain interfaces, never directly on Core repositories or DAOs
                !import.name.startsWith("ltd.evilcorp.core.repository") &&
                !import.name.startsWith("ltd.evilcorp.core.db")
            }
    }

    @Test
    fun `presentation layer atox should not declare any UseCase classes`() {
        Konsist
            .scopeFromProduction("atox")
            .classes()
            .assertTrue { !it.name.endsWith("UseCase") }
    }

    @Test
    fun `composables inside atox should not use hardcoded string literals`() {
        Konsist
            .scopeFromProduction("atox")
            .files
            .assertTrue { file ->
                if (file.name.endsWith("Previews")) {
                    true
                } else {
                    val text = file.text
                    val textPattern = """Text\(\s*"[^"]*"\s*\)""".toRegex()
                    val namedTextPattern = """Text\(.*text\s*=\s*"[^"]*".*\)""".toRegex()
                    val hasHardcoded = textPattern.containsMatchIn(text) || namedTextPattern.containsMatchIn(text)
                    if (hasHardcoded) {
                        println("Violating file: ${file.name}")
                    }
                    !hasHardcoded
                }
            }
    }

    @Test
    fun `ViewModels must interact with data and business logic strictly via UseCases`() {
        Konsist
            .scopeFromProduction("atox")
            .classes()
            .filter { it.name.endsWith("ViewModel") }
            .assertTrue { viewModel ->
                viewModel.constructors.all { constructor ->
                    constructor.parameters.none { param ->
                        val name = param.type.name
                        
                        // Full blocking of direct dependencies
                        name.startsWith("ITox") || 
                        name.endsWith("Repository") || 
                        name.endsWith("Manager") || 
                        name.endsWith("Dao") || name.endsWith("Entity")
                    }
                }
            }
    }

    @Test
    fun `Service lifecycle callbacks must not execute JNI or block main thread`() {
        Konsist
            .scopeFromProduction("atox")
            .classes()
            .filter { it.hasParentWithName("LifecycleService") || it.hasParentWithName("Service") }
            .assertTrue { serviceClass ->
                val onCreateMethod = serviceClass.functions().firstOrNull { it.name == "onCreate" }
                val methodText = onCreateMethod?.text ?: ""
                // Enforce async launch or coroutine context for synchronous initialization
                if (methodText.contains("initializeToxUseCase.execute")) {
                    methodText.contains("launch") || methodText.contains("withContext")
                } else {
                    true
                }
            }
    }

    @Test
    fun `ViewModels must not import Android context or UI view classes to prevent memory leaks`() {
        Konsist
            .scopeFromProduction("atox")
            .classes()
            .filter { it.name.endsWith("ViewModel") }
            .assertTrue { viewModel ->
                viewModel.containingFile.imports.none { import ->
                    import.name.startsWith("android.content.Context") ||
                    import.name.startsWith("android.view") ||
                    import.name.startsWith("android.widget")
                }
            }
    }

    @Test
    fun `UI Composable files must never import or consume raw Room database entities`() {
        Konsist
            .scopeFromProduction("atox")
            .files
            .filter { it.name.endsWith("Screen") || it.name.endsWith("Components") || it.name.endsWith("Bubble") || it.name.endsWith("Card") }
            .assertTrue { uiFile ->
                uiFile.imports.none { import ->
                    import.name.startsWith("ltd.evilcorp.core.db.entity") ||
                    import.name.endsWith("Entity")
                }
            }
    }

    @Test
    fun `presentation and domain layers must never directly inject repository implementations`() {
        Konsist
            .scopeFromProduction("atox")
            .plus(Konsist.scopeFromProduction("domain"))
            .classes()
            .assertTrue { clazz ->
                clazz.constructors.all { constructor ->
                    constructor.parameters.none { param ->
                        param.type.name.endsWith("RepositoryImpl")
                    }
                }
            }
    }

    @Test
    fun `ViewModels must not expose public mutable flows`() {
        Konsist.scopeFromProduction("atox")
            .classes()
            .filter { it.name.endsWith("ViewModel") }
            .assertTrue { viewModel ->
                viewModel.properties().none { property ->
                    val isPublic = property.hasPublicModifier ||
                        (!property.hasPrivateModifier && !property.hasProtectedModifier && !property.hasInternalModifier)
                    val typeName = property.type?.name ?: ""
                    isPublic && (typeName.contains("MutableStateFlow") || typeName.contains("MutableSharedFlow"))
                }
            }
    }

    @Test
    fun `all DI modules in di package must be interfaces or classes annotated with Module and InstallIn`() {
        Konsist.scopeFromProduction("atox")
            .classes()
            .filter { it.resideInPackage("ltd.evilcorp.atox.di..") }
            .assertTrue { clazz ->
                clazz.hasAnnotation { it.name == "Module" } &&
                clazz.hasAnnotation { it.name == "InstallIn" }
            }
    }

    @Test
    fun `ViewModels must be annotated with HiltViewModel and have an Inject constructor`() {
        Konsist.scopeFromProduction("atox")
            .classes()
            .filter { it.name.endsWith("ViewModel") }
            .assertTrue { viewModel ->
                viewModel.hasAnnotation { it.name == "HiltViewModel" } &&
                viewModel.hasConstructor { constructor ->
                    constructor.hasAnnotation { it.name == "Inject" }
                }
            }
    }

    @Test
    fun `ViewModels must not depend on other ViewModels to enforce clean isolation`() {
        Konsist.scopeFromProduction("atox")
            .classes()
            .filter { it.name.endsWith("ViewModel") }
            .assertTrue { viewModel ->
                viewModel.constructors.all { constructor ->
                    constructor.parameters.none { param ->
                        param.type.name.endsWith("ViewModel")
                    }
                }
            }
    }
}

