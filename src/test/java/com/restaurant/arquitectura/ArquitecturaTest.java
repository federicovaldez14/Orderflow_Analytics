package com.restaurant.arquitectura;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * FUNCIONES DE APTITUD de la arquitectura hexagonal (ADR-001), con ArchUnit.
 *
 * No revisan lógica de negocio sino las fronteras del estilo: las dependencias
 * apuntan hacia adentro (infraestructura -> aplicación -> dominio) y los
 * adaptadores de entrada no se saltan los casos de uso para ir a la base de
 * datos. ArchUnit analiza el bytecode, así que detecta cualquier uso (import,
 * nombre completo, herencia, anotación, parámetro), no solo los "import".
 *
 * Corren en la etapa "Reglas de arquitectura" del pipeline y la detienen si
 * alguien rompe una regla (ver docs/devops.md).
 */
@AnalyzeClasses(packages = "com.restaurant", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    private static final String DOMINIO = "com.restaurant.dominio..";
    private static final String APLICACION = "com.restaurant.aplicacion..";
    private static final String INFRAESTRUCTURA = "com.restaurant.infraestructura..";

    /** Frameworks y tecnologías que solo pueden aparecer en los adaptadores. */
    private static final String[] TECNOLOGIA = {
        "org.springframework..", "jakarta..", "java.sql..", "javax.sql..",
        "javax.swing..", "java.awt..", "com.formdev..", "com.zaxxer..", "org.h2..", "org.postgresql..",
        "com.fasterxml.."
    };

    @ArchTest
    static final ArchRule R1_el_dominio_no_depende_de_aplicacion_ni_de_infraestructura =
            noClasses().that().resideInAPackage(DOMINIO)
                    .should().dependOnClassesThat().resideInAnyPackage(APLICACION, INFRAESTRUCTURA)
                    .because("ADR-001: el dominio es el centro del hexágono y no conoce a nadie");

    @ArchTest
    static final ArchRule R2_la_aplicacion_no_depende_de_la_infraestructura =
            noClasses().that().resideInAPackage(APLICACION)
                    .should().dependOnClassesThat().resideInAPackage(INFRAESTRUCTURA)
                    .because("ADR-001: los casos de uso hablan con el exterior solo por puertos");

    @ArchTest
    static final ArchRule R3_el_nucleo_no_usa_frameworks_ni_tecnologia =
            noClasses().that().resideInAnyPackage(DOMINIO, APLICACION)
                    .should().dependOnClassesThat().resideInAnyPackage(TECNOLOGIA)
                    .because("ADR-001: el núcleo es Java puro; Spring, JDBC, Swing y la base de datos son adaptadores");

    @ArchTest
    static final ArchRule R4_los_adaptadores_de_entrada_no_van_directo_a_la_base_de_datos =
            noClasses().that().resideInAnyPackage("com.restaurant.infraestructura.rest..", "com.restaurant.infraestructura.ui..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.restaurant.infraestructura.persistencia..", "java.sql..", "javax.sql..")
                    .because("ADR-001: REST y Swing usan los casos de uso; no se saltan el núcleo para leer o escribir datos");

    @ArchTest
    static final ArchRule R5_los_repositorios_jdbc_implementan_un_puerto_de_salida =
            classes().that().resideInAPackage("com.restaurant.infraestructura.persistencia..")
                    .and().haveSimpleNameEndingWith("RepositorioJdbc")
                    .should().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                            "un puerto de com.restaurant.aplicacion.puerto.salida",
                            (com.tngtech.archunit.core.domain.JavaClass c) ->
                                    c.getPackageName().startsWith("com.restaurant.aplicacion.puerto.salida")))
                    .because("ADR-001 y ADR-002: la persistencia es un adaptador intercambiable detrás de un puerto");

    @ArchTest
    static final ArchRule R6_solo_la_raiz_de_composicion_crea_los_adaptadores_de_salida =
            noClasses().that().resideOutsideOfPackages("com.restaurant.infraestructura.config..",
                            "com.restaurant.infraestructura.persistencia..")
                    .should().dependOnClassesThat().resideInAPackage("com.restaurant.infraestructura.persistencia..")
                    .because("ADR-001: solo ConfiguracionOrderflow (composition root) conoce los adaptadores JDBC");

    @ArchTest
    static final ArchRule R7_sin_ciclos_entre_capas =
            slices().matching("com.restaurant.(*)..")
                    .should().beFreeOfCycles()
                    .because("las dependencias entre dominio, aplicación e infraestructura van en un solo sentido");

    /**
     * Control compensatorio del riesgo aceptado en .trivyignore (docs/seguridad.md, H-07):
     * CVE-2026-47884 (XsltView) y CVE-2026-47890 (SSE con vistas) de Spring Framework 6.2 no
     * tienen parche en la rama 6.2 y solo afectan a quien usa esas piezas. Si alguien empieza a
     * usarlas, esta regla rompe el build y la aceptación del riesgo deja de ser válida.
     */
    @ArchTest
    static final ArchRule R8_no_se_usan_las_piezas_de_spring_con_cve_aceptadas =
            noClasses().should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.web.servlet.view.xslt..")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName(
                            "org.springframework.web.servlet.mvc.method.annotation.SseEmitter")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName(
                            "org.springframework.web.servlet.ModelAndView")
                    .because("ADR-006: el riesgo de CVE-2026-47884/47890 se aceptó porque la API es JSON pura, sin vistas XSLT ni SSE");
}
