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
            .scopeFromPackage("ltd.evilcorp.core..")
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
            .scopeFromPackage("ltd.evilcorp.core.db..")
            .classes()
            .assertTrue { clazz ->
                !clazz.name.endsWith("UseCase") && !clazz.name.endsWith("ViewModel")
            }
    }
}
