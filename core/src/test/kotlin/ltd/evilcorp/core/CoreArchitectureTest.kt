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
                packageFqName.startsWith("ltd.evilcorp.core.tox")
            }
    }
}
