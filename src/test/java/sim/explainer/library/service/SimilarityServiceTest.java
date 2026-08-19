package sim.explainer.library.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sim.explainer.library.enumeration.CombinationStrategy;
import sim.explainer.library.enumeration.FileTypeConstant;
import sim.explainer.library.enumeration.ImplementationMethod;
import sim.explainer.library.framework.ABoxServiceContext;
import sim.explainer.library.framework.ALCPreferenceProfile;
import sim.explainer.library.framework.KRSSServiceContext;
import sim.explainer.library.framework.OWLServiceContext;
import sim.explainer.library.framework.PreferenceProfile;
import sim.explainer.library.framework.reasoner.OverlapReasoner;

class SimilarityServiceTest {

    private static final String FIXTURE = """
            <?xml version="1.0"?>
            <rdf:RDF xmlns="http://example.org/test#"
                 xml:base="http://example.org/test"
                 xmlns:owl="http://www.w3.org/2002/07/owl#"
                 xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                 xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                 xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
                <owl:Ontology rdf:about="http://example.org/test"/>
                <owl:ObjectProperty rdf:about="http://example.org/test#hasPart"/>
                <owl:Class rdf:about="http://example.org/test#TestConceptA">
                    <rdfs:subClassOf>
                        <owl:Restriction>
                            <owl:onProperty rdf:resource="http://example.org/test#hasPart"/>
                            <owl:someValuesFrom rdf:resource="http://example.org/test#TestConceptC"/>
                        </owl:Restriction>
                    </rdfs:subClassOf>
                </owl:Class>
                <owl:Class rdf:about="http://example.org/test#TestConceptB">
                    <rdfs:subClassOf>
                        <owl:Restriction>
                            <owl:onProperty rdf:resource="http://example.org/test#hasPart"/>
                            <owl:someValuesFrom rdf:resource="http://example.org/test#TestConceptC"/>
                        </owl:Restriction>
                    </rdfs:subClassOf>
                </owl:Class>
                <owl:Class rdf:about="http://example.org/test#TestConceptC"/>
                <owl:NamedIndividual rdf:about="http://example.org/test#ind1">
                    <rdf:type rdf:resource="http://example.org/test#TestConceptC"/>
                </owl:NamedIndividual>
            </rdf:RDF>
            """;

    private SimilarityService buildService(Path tempDir) throws IOException {
        Path fixtureFile = tempDir.resolve("fixture.owl");
        Files.writeString(fixtureFile, FIXTURE);

        OWLServiceContext owlServiceContext = new OWLServiceContext();
        owlServiceContext.init(fixtureFile.toString());

        KRSSServiceContext krssServiceContext = new KRSSServiceContext();
        PreferenceProfile preferenceProfile = new PreferenceProfile();

        ABoxServiceContext aboxServiceContext = new ABoxServiceContext();
        aboxServiceContext.initFromOWL(owlServiceContext);
        OverlapReasoner overlapReasoner = new OverlapReasoner(aboxServiceContext, new ALCPreferenceProfile());

        return new SimilarityService(owlServiceContext, krssServiceContext, preferenceProfile, overlapReasoner);
    }

    @Test
    void alcMethod_routesToAlcReasoner_identicalConceptsScoreOne(@TempDir Path tempDir) throws IOException {
        SimilarityService service = buildService(tempDir);

        BigDecimal result = service.measureConceptWithType(
                "TestConceptA", "TestConceptA", ImplementationMethod.TOPDOWN_ALC_SIM,
                FileTypeConstant.OWL_FILE, CombinationStrategy.AVERAGE);

        assertEquals(0, new BigDecimal("1").compareTo(result));
    }

    @Test
    void alehMethod_routesToAlehReasoner_identicalConceptsScoreOne(@TempDir Path tempDir) throws IOException {
        SimilarityService service = buildService(tempDir);

        BigDecimal result = service.measureConceptWithType(
                "TestConceptA", "TestConceptA", ImplementationMethod.TOPDOWN_ALEH_SIM,
                FileTypeConstant.OWL_FILE, CombinationStrategy.AVERAGE);

        assertEquals(0, new BigDecimal("1").compareTo(result));
    }
}