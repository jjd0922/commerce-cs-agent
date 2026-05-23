package com.commerce.cs.test;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("아키텍처 계층 의존성 규칙")
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
        )
        .as("domain 계층은 application, api, infra, bootstrap 계층에 의존하지 않는다");

    @ArchTest
    static final ArchRule application_should_not_depend_on_api_or_infra = noClasses()
        .that().resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "..api..",
            "..infra..",
            "..bootstrap.."
        )
        .as("application 계층은 api, infra, bootstrap 계층에 의존하지 않는다");

    @ArchTest
    static final ArchRule api_should_not_depend_on_domain_or_infra = noClasses()
        .that().resideInAPackage("..api..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "..domain..",
            "..infra..",
            "..bootstrap.."
        )
        .as("api 계층은 domain, infra, bootstrap 계층에 의존하지 않는다");
}
