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
            .scopeFromProject()
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
    fun `presentation layer atox should not declare any UseCase classes`() {
        Konsist
            .scopeFromPackage("ltd.evilcorp.atox..")
            .classes()
            .assertTrue { !it.name.endsWith("UseCase") }
    }

    @Test
    fun `composables inside atox should not use hardcoded string literals`() {
        Konsist
            .scopeFromPackage("ltd.evilcorp.atox.ui..")
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
