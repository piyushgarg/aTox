// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class ArchitectureTest {

    @Test
    fun `project architecture should conform to strict Clean Architecture rules`() {
        Konsist
            .scopeFromProject()
            .assertArchitecture {
                val domain = Layer("Domain", "ltd.evilcorp.domain..")
                val core = Layer("Core", "ltd.evilcorp.core..")
                val atox = Layer("Presentation", "ltd.evilcorp.atox..")

                domain.dependsOnNothing()
                core.dependsOn(domain)
                atox.dependsOn(domain) // КРИТИЧЕСКИ: Presentation больше НЕ имеет права напрямую импортировать Core!
            }
    }

    @Test
    fun `domain layer must be KMP compliant and completely independent of any JVM and OS specific IO`() {
        Konsist
            .scopeFromPackage("ltd.evilcorp.domain..")
            .imports
            .assertTrue { import ->
                // Блокируем любые JVM-дисковые операции и системные классы ввода-вывода в бизнес-логике
                !import.name.startsWith("java.io.File") &&
                !import.name.startsWith("java.io.RandomAccessFile") &&
                !import.name.startsWith("java.io.FileOutputStream") &&
                !import.name.startsWith("java.io.FileInputStream") &&
                !import.name.startsWith("java.nio") &&
                // Блокируем любые платформозависимые библиотеки Android
                !import.name.startsWith("android.") &&
                !import.name.startsWith("androidx.")
            }
    }

    @Test
    fun `domain layer must have zero database and repository implementation dependencies`() {
        Konsist
            .scopeFromPackage("ltd.evilcorp.domain..")
            .imports
            .assertTrue { import ->
                !import.name.startsWith("androidx.room") &&
                !import.name.startsWith("ltd.evilcorp.core.db") &&
                !import.name.startsWith("ltd.evilcorp.core.repository")
            }
    }

    @Test
    fun `presentation layer including all subpackages must never access Room database or DAOs directly`() {
        Konsist
            .scopeFromPackage("ltd.evilcorp.atox..")
            .files
            .filterNot { file ->
                file.path.contains("/di/") ||
                file.path.contains("/src/androidTest/") ||
                file.path.contains("/src/test/")
            }
            .flatMap { it.imports }
            .assertTrue { import ->
                !import.name.startsWith("ltd.evilcorp.core.db") &&
                !import.name.startsWith("androidx.room")
            }
    }

    @Test
    fun `presentation layer must never import concrete Core Repositories directly`() {
        Konsist
            .scopeFromPackage("ltd.evilcorp.atox..")
            .files
            .filterNot { file ->
                file.path.contains("/di/") ||
                file.path.contains("/src/androidTest/") ||
                file.path.contains("/src/test/")
            }
            .flatMap { it.imports }
            .assertTrue { import ->
                !import.name.startsWith("ltd.evilcorp.core.repository")
            }
    }

    @Test
    fun `core layer must not contain any Compose or UI components`() {
        Konsist
            .scopeFromPackage("ltd.evilcorp.core..")
            .imports
            .assertTrue { import ->
                !import.name.startsWith("androidx.compose") &&
                !import.name.startsWith("android.view") &&
                !import.name.startsWith("android.widget")
            }
    }

    @Test
    fun `use cases must reside strictly in usecase package and have UseCase suffix`() {
        Konsist
            .scopeFromProject()
            .classes()
            .filter { it.name.endsWith("UseCase") }
            .assertTrue { it.resideInPackage("ltd.evilcorp.domain.usecase") }
    }
}
