package com.smartdx.app;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Modular Monolith のモジュール境界を自動検証する (ADR-006 / CLAUDE.md 禁止事項)。
 * <ul>
 *   <li>ドメインモジュール間の直接 Java クラス参照禁止 (libs 経由のみ)</li>
 *   <li>libs からドメインモジュールへの依存禁止 (libs に業務ロジックを置かない)</li>
 * </ul>
 * 違反はビルド時に検出され、規約の形骸化を防ぐ。
 */
@AnalyzeClasses(
        packages = "com.smartdx",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
public class ModularBoundaryArchTest {

    private static final String PROPERTY = "com.smartdx.property..";
    private static final String SYSTEM = "com.smartdx.system..";
    private static final String RETAIL = "com.smartdx.retail..";

    @ArchTest
    static final ArchRule property_must_not_depend_on_other_domains =
            noClasses().that().resideInAPackage(PROPERTY)
                    .should().dependOnClassesThat().resideInAnyPackage(SYSTEM, RETAIL);

    @ArchTest
    static final ArchRule system_must_not_depend_on_other_domains =
            noClasses().that().resideInAPackage(SYSTEM)
                    .should().dependOnClassesThat().resideInAnyPackage(PROPERTY, RETAIL);

    @ArchTest
    static final ArchRule retail_must_not_depend_on_other_domains =
            noClasses().that().resideInAPackage(RETAIL)
                    .should().dependOnClassesThat().resideInAnyPackage(PROPERTY, SYSTEM);

    @ArchTest
    static final ArchRule libs_must_not_depend_on_domain_modules =
            noClasses().that().resideInAnyPackage(
                            "com.smartdx.core..",
                            "com.smartdx.security..",
                            "com.smartdx.tenant..",
                            "com.smartdx.redis..",
                            "com.smartdx.opensearch..",
                            "com.smartdx.observability..")
                    .should().dependOnClassesThat().resideInAnyPackage(PROPERTY, SYSTEM, RETAIL);
}
