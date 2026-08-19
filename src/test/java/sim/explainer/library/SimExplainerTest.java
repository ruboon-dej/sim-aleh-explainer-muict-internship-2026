package sim.explainer.library;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sim.explainer.library.enumeration.CombinationStrategy;
import sim.explainer.library.enumeration.ImplementationMethod;
import sim.explainer.library.exception.JSimPiException;
import sim.explainer.library.framework.explainer.ALCExplanationTable;

class SimExplainerTest {

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

    private SimExplainer buildExplainer(Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("fixture.owl"), FIXTURE);
        return new SimExplainer(tempDir.toString());
    }

    @Test
    void callingWrongExplanationGetter_throwsInsteadOfReturningStaleData(@TempDir Path tempDir) throws IOException {
        SimExplainer explainer = buildExplainer(tempDir);

        explainer.similarity(ImplementationMethod.TOPDOWN_ALC_SIM, "TestConceptA", "TestConceptB", CombinationStrategy.AVERAGE);

        // ALC results go into alcExplanationMap, not explanationMap/fl0ExplanationMap -
        // asking for the FL0 explanation of an ALC-computed pair must fail, not silently
        // return nothing or stale data.
        assertThrows(JSimPiException.class, () ->
                explainer.getFL0Explanation("TestConceptA", "TestConceptB", ImplementationMethod.TOPDOWN_ALC_SIM, CombinationStrategy.AVERAGE));
    }

    @Test
    void callingExplanationGetter_beforeSimilarity_throws(@TempDir Path tempDir) throws IOException {
        SimExplainer explainer = buildExplainer(tempDir);

        assertThrows(JSimPiException.class, () ->
                explainer.getALCExplanation("TestConceptA", "TestConceptB", ImplementationMethod.TOPDOWN_ALC_SIM, CombinationStrategy.AVERAGE));
    }

    @Test
    void resetPreferenceProfile_clearsExplanationMapsSoOldExplanationIsGone(@TempDir Path tempDir) throws IOException {
        SimExplainer explainer = buildExplainer(tempDir);

        explainer.similarity(ImplementationMethod.TOPDOWN_ALC_SIM, "TestConceptA", "TestConceptB", CombinationStrategy.AVERAGE);
        ALCExplanationTable before = explainer.getALCExplanation(
                "TestConceptA", "TestConceptB", ImplementationMethod.TOPDOWN_ALC_SIM, CombinationStrategy.AVERAGE);
        assertEquals(true, before != null);

        explainer.resetPreferenceProfile();

        assertThrows(JSimPiException.class, () ->
                explainer.getALCExplanation("TestConceptA", "TestConceptB", ImplementationMethod.TOPDOWN_ALC_SIM, CombinationStrategy.AVERAGE));
    }
}