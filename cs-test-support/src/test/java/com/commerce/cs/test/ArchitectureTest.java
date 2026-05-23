package com.commerce.cs.test;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "com.commerce.cs",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_other_layers = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "..application..",
            "..api..",
            "..infra..",
            "..bootstrap.."
        );

    @ArchTest
    static final ArchRule application_should_not_depend_on_api_or_infra = noClasses()
        .that().resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "..api..",
            "..infra..",
            "..bootstrap.."
        );

    @ArchTest
    static final ArchRule api_should_not_depend_on_domain_or_infra = noClasses()
        .that().resideInAPackage("..api..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "..domain..",
            "..infra..",
            "..bootstrap.."
        );
}
