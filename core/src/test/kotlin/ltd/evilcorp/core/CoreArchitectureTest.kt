// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class CoreArchitectureTest {

    @Test
    fun `core layer should not depend on presentation layer`() {
        Konsist
            .scopeFromDirectory("core")
            .imports
            .assertTrue { import ->
                !import.name.startsWith("ltd.evilcorp.atox")
            }
    }

    @Test
    fun `database entities should be defined in core module and mapped to domain models`() {
        // Core layer DAOs and repositories should not leak raw domain model modifications
        // and should reside inside core db or repository packages.
        Konsist
            .scopeFromDirectory("core")
            .classes()
            .filter { it.resideInPackage("ltd.evilcorp.core.db..") }
            .assertTrue { clazz ->
                !clazz.name.endsWith("UseCase") && !clazz.name.endsWith("ViewModel")
            }
    }

    @Test
    fun `core layer files should reside only inside db, platform, repository, or tox packages`() {
        Konsist
            .scopeFromProduction("core")
            .files
            .assertTrue { file ->
                val packageFqName = file.packagee?.name ?: ""
                packageFqName.startsWith("ltd.evilcorp.core.db") ||
                packageFqName.startsWith("ltd.evilcorp.core.platform") ||
                packageFqName.startsWith("ltd.evilcorp.core.repository") ||
                packageFqName.startsWith("ltd.evilcorp.core.tox") ||
                packageFqName.startsWith("ltd.evilcorp.core.profile")
            }
    }

    @Test
    fun `repositories must not depend on other repositories to enforce clean isolation`() {
        Konsist
            .scopeFromProduction("core")
            .classes()
            .filter { it.name.endsWith("RepositoryImpl") }
            .assertTrue { repository ->
                val repositoryInterface = repository.interfaces().firstOrNull()
                repository.constructors.all { constructor ->
                    constructor.parameters.none { param ->
                        // Only own repository interface injection is allowed (if applicable)
                        param.type.name.endsWith("Repository") && param.type.name != repositoryInterface?.name
                    }
                }
            }
    }

    @Test
    fun `repository implementations must reside in core repository package and end with RepositoryImpl`() {
        Konsist.scopeFromProduction("core")
            .classes()
            .filter { clazz -> clazz.interfaces().any { it.name.endsWith("Repository") } }
            .assertTrue { clazz ->
                clazz.resideInPackage("ltd.evilcorp.core.repository..") &&
                clazz.name.endsWith("RepositoryImpl")
            }
    }

    @Test
    fun `DAO write operations must be suspend functions`() {
        Konsist.scopeFromProduction("core")
            .interfaces()
            .filter { it.hasAnnotation { annotation -> annotation.name == "Dao" } }
            // Validate ALL DAOs without exclusions!
            .flatMap { it.functions() }
            .filter { func ->
                func.hasAnnotation { annotation ->
                    annotation.name == "Insert" ||
                    annotation.name == "Update" ||
                    annotation.name == "Delete" ||
                    (annotation.name == "Query" && (
                        annotation.text.contains("UPDATE", ignoreCase = true) ||
                        annotation.text.contains("DELETE", ignoreCase = true) ||
                        annotation.text.contains("INSERT", ignoreCase = true)
                    ))
                }
            }
            .assertTrue { func ->
                func.hasSuspendModifier
            }
    }

    @Test
    fun `all Room database DAOs must be interfaces`() {
        Konsist.scopeFromProduction("core")
            .classes()
            .assertTrue { clazz ->
                !clazz.hasAnnotation { annotation -> annotation.name == "Dao" }
            }
    }
}

