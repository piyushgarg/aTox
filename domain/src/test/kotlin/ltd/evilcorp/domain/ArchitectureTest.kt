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
    fun `domain layer should be clean and independent`() {
        Konsist
            .scopeFromProject()
            .assertArchitecture {
                val domain = Layer("Domain", "ltd.evilcorp.domain..")
                val core = Layer("Core", "ltd.evilcorp.core..")
                val atox = Layer("Presentation", "ltd.evilcorp.atox..")

                domain.dependsOnNothing()
            }
    }

    @Test
    fun `use cases should reside strictly in usecase package`() {
        Konsist
            .scopeFromProject()
            .classes()
            .filter { it.name.endsWith("UseCase") }
            .assertTrue { it.resideInPackage("ltd.evilcorp.domain.usecase") }
    }
}
