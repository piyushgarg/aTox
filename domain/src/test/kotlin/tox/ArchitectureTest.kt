package tox

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.Test

class ArchitectureTest {

    @Test
    fun domain_layer_should_not_depend_on_core_or_atox_implementation() {
        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ltd.evilcorp..")

        // Проверяем, что доменный слой не импортирует детали реализации БД, сети и JNI из core
        noClasses().that().resideInAPackage("ltd.evilcorp.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "ltd.evilcorp.core.db..",
                "ltd.evilcorp.core.repository..",
                "ltd.evilcorp.core.tox.runtime..",
                "ltd.evilcorp.atox.."
            )
            .check(importedClasses)
    }

    @Test
    fun domain_managers_should_only_depend_on_repository_interfaces() {
        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ltd.evilcorp..")

        // Проверяем, что доменные фичи/менеджеры не зависят от конкретных репозиториев из core
        noClasses().that().resideInAPackage("ltd.evilcorp.domain.feature..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "ltd.evilcorp.core.repository..",
                "ltd.evilcorp.core.db..",
                "ltd.evilcorp.core.tox.runtime..",
                "ltd.evilcorp.atox.."
            )
            .check(importedClasses)
    }
}

