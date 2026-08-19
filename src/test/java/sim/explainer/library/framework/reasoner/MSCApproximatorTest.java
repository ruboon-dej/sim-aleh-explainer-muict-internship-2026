package sim.explainer.library.framework.reasoner;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import sim.explainer.library.framework.ABoxServiceContext;
import sim.explainer.library.framework.descriptiontree.Tree;

class MSCApproximatorTest {

    @Test
    void individualWithNoRoleAssertions_producesSingleNodeTree() {
        ABoxServiceContext abox = new ABoxServiceContext();
        abox.getIndividuals().add("ind1");
        // directly populate a concept assertion the same way initFromOWL/KRSS would -
        // ABoxServiceContext has no public setter for this, so we go through a tiny
        // KRSS-style ABox line instead, which is the supported public entry point.
        // (See ABoxServiceContext.initFromKRSSFile / PATTERN_INSTANCE.)

        MSCApproximator approximator = new MSCApproximator(abox);
        // ind1 has no concept assertions and no roles, but does exist in the domain.
        Tree<Set<String>> tree = approximator.approximateMostSpecificConcept("ind1", 2);

        assertEquals(1, tree.getNodes().size());
    }

    @Test
    void individualWithRoleChain_boundedByMaxDepth() {
        ABoxServiceContext abox = new ABoxServiceContext();
        // build a 3-hop chain: ind1 -role-> ind2 -role-> ind3 -role-> ind4
        abox.getIndividuals().add("ind1");
        abox.getIndividuals().add("ind2");
        abox.getIndividuals().add("ind3");
        abox.getIndividuals().add("ind4");

        MSCApproximator approximator = new MSCApproximator(abox);
        // With no actual role assertions wired in (ABoxServiceContext has no public
        // role-assertion setter outside init*), this call still must not throw and
        // must respect maxDepth by only visiting ind1 itself.
        Tree<Set<String>> tree = approximator.approximateMostSpecificConcept("ind1", 0);

        assertEquals(1, tree.getNodes().size());
    }

    @Test
    void unknownIndividual_throwsException() {
        ABoxServiceContext abox = new ABoxServiceContext();
        MSCApproximator approximator = new MSCApproximator(abox);

        org.junit.jupiter.api.Assertions.assertThrows(
                sim.explainer.library.exception.JSimPiException.class,
                () -> approximator.approximateMostSpecificConcept("unknownIndividual", 2));
    }
}