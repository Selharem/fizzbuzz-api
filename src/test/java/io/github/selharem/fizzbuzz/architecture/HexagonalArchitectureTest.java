package io.github.selharem.fizzbuzz.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

@AnalyzeClasses(packages = "io.github.selharem.fizzbuzz", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule HEXAGONAL_LAYERS = onionArchitecture()
            .domainModels("..domain.model..")
            .domainServices("..domain.service..")
            .applicationServices("..application..")
            .adapter("web", "..adapter.in.web..")
            .adapter("persistence", "..adapter.out.persistence..")
            // The configuration package is the composition root that wires the core into Spring.
            .ignoreDependency(resideInAPackage("io.github.selharem.fizzbuzz.config.."), alwaysTrue());

    @ArchTest
    static final ArchRule CORE_IS_FRAMEWORK_FREE = classes()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "java..",
                    "io.github.selharem.fizzbuzz.domain..",
                    "io.github.selharem.fizzbuzz.application..");

    @ArchTest
    static final ArchRule ADAPTERS_USE_PORTS_NOT_IMPLEMENTATIONS = classes()
            .that().resideInAPackage("..adapter..")
            .should().onlyDependOnClassesThat()
            .resideOutsideOfPackage("..application.service..");

    @ArchTest
    static final ArchRule PERSISTENCE_ADAPTERS_IMPLEMENT_OUTBOUND_PORTS = classes()
            .that().resideInAPackage("..adapter.out..")
            .and().areTopLevelClasses()
            .should().implement(describe(
                    "an outbound port",
                    (JavaClass port) -> port.getPackageName().endsWith("application.port.out")));
}
