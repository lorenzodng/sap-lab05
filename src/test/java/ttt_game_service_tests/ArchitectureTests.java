package ttt_game_service_tests;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import exagonal.Adapter;
import exagonal.InBoundPort;
import exagonal.OutBoundPort;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

//test dell'architettura
public class ArchitectureTests {

    //test della clean architecture
    @Test
    public void cleanArchitecture() {

        JavaClasses importedClasses = new ClassFileImporter().importPackages("ttt_game_service"); //usa la libreria "ArchUnit" per importare tutte le classi del package "ttt_game_service"
        var domainPackage = "..domain.."; //definisce il package "domain"
        var applicationPackage = "..application.."; //definisce il package "application"
        var infrastructurePackage = "..infrastructure.."; //definisce il package "infrastructure"

        var domainModelWithNoDeps = noClasses().that().resideInAPackage(domainPackage) //nessuna classe che risiede nel package "domain"
                .should().dependOnClassesThat().resideInAPackage(applicationPackage) //dovrebbe dipendere da classi che risiedono nel package application
                .orShould().dependOnClassesThat().resideInAPackage(infrastructurePackage); //o infrastructure

        //esegue la verifica della regola
        domainModelWithNoDeps.check(importedClasses);

		/*
		Il progetto deve avere una struttura a layer in cui, definiti i layer (package): domain, application, infrastructure:
		- domain può essere acceduto solo dai layer application e infrastructure.
		- application può essere acceduto solo dal layer Infrastructure;
		- infrastructure non può essere acceduto da nessun altro layer;
		 */
        var layeredRule = layeredArchitecture().consideringAllDependencies()
                .layer("Domain").definedBy(domainPackage)
                .layer("Application").definedBy(applicationPackage)
                .layer("Infrastructure").definedBy(infrastructurePackage)
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer();

        //esegue la verifica della regola
        layeredRule.check(importedClasses);
    }

    //test dell'architettura esagonale
    @Test
    public void hexagonalArchitecture() {

		//esegue la verifica della clean architecture
		cleanArchitecture();

        JavaClasses importedClasses = new ClassFileImporter().importPackages("ttt_game_service"); //usa ArchUnit per importare tutte le classi del package "ttt_game_service"
        var domainPackage = "..domain.."; //definisce il package "domain"
        var applicationPackage = "..application.."; //definisce il package "application"
        var infrastructurePackage = "..infrastructure.."; //definisce il package "infrastructure"

        var portsRule = classes().that().areAnnotatedWith(InBoundPort.class).or().areAnnotatedWith(OutBoundPort.class) //tutte le classi che sono annotate come "InBoundPort" o "OutBoundPort"
                .should().resideInAPackage(applicationPackage) //dovrebbero risiedere nel package application
                .orShould().resideInAPackage(domainPackage); //o domain

        //esegue la verifica della regola
        portsRule.check(importedClasses);

        var adaptersRule = classes().that().areAnnotatedWith(Adapter.class) //tutte le classi che sono annotate come "Adapter"
                .should().resideInAPackage(infrastructurePackage); //dovrebbero risiedere nel package infrastructure

        //esegue la verifica della regola
        adaptersRule.check(importedClasses);
    }
}
