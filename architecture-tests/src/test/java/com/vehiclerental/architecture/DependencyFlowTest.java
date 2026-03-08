package com.vehiclerental.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.vehiclerental", importOptions = ImportOption.DoNotIncludeTests.class)
class DependencyFlowTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application_or_infrastructure =
            noClasses().that().resideInAnyPackage("com.vehiclerental..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.vehiclerental..application..", "com.vehiclerental..infrastructure..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure =
            noClasses().that().resideInAnyPackage("com.vehiclerental..application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.vehiclerental..infrastructure..");
}
