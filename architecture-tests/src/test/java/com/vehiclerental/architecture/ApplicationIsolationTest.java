package com.vehiclerental.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "com.vehiclerental", importOptions = ImportOption.DoNotIncludeTests.class)
class ApplicationIsolationTest {

    @ArchTest
    static final ArchRule application_should_only_depend_on_allowed_packages =
            classes().that().resideInAnyPackage("com.vehiclerental..application..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "com.vehiclerental..domain..",
                            "com.vehiclerental..application..",
                            "com.vehiclerental.common..",
                            "java..",
                            "lombok..",
                            "org.slf4j..",
                            "com.fasterxml.jackson..",
                            "org.springframework.transaction.."
                    );
}
