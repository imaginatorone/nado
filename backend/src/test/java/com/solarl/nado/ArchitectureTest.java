package com.solarl.nado;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Архитектурные ограничения проекта Nado.
 * Контроллеры НЕ должны обращаться к репозиториям напрямую — только через сервисный слой.
 */
@AnalyzeClasses(packages = "com.solarl.nado", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_should_not_access_repositories =
            noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().accessClassesThat().resideInAPackage("..repository..")
                    .because("Контроллеры должны использовать сервисы, а не репозитории напрямую");
}
