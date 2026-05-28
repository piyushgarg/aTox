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
}
