package com.vehiclerental.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.vehiclerental", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainPurityTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring =
            noClasses().that().resideInAnyPackage("com.vehiclerental..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_jpa =
            noClasses().that().resideInAnyPackage("com.vehiclerental..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application =
            noClasses().that().resideInAnyPackage("com.vehiclerental..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.vehiclerental..application..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
            noClasses().that().resideInAnyPackage("com.vehiclerental..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.vehiclerental..infrastructure..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_common_messaging =
            noClasses().that().resideInAnyPackage("com.vehiclerental..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.vehiclerental.common.messaging..");
}
