package sim.explainer.library.framework.reasoner;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import sim.explainer.library.framework.ABoxServiceContext;
import sim.explainer.library.framework.ALCPreferenceProfile;
import sim.explainer.library.framework.descriptiontree.Tree;

class TopDownALCSimReasonerImplTest {

    @Test
    void measureDirectedSimilarity_equalsOneMinusDissimilarity() {
        ABoxServiceContext abox = new ABoxServiceContext();
        abox.getIndividuals().add("ind1");
        OverlapReasoner overlapReasoner = new OverlapReasoner(abox, new ALCPreferenceProfile());
        TopDownALCSimReasonerImpl reasoner = new TopDownALCSimReasonerImpl(overlapReasoner);

        Tree<Set<String>> tree1 = new Tree<>("c1");
        Set<String> data = new HashSet<>();
        data.add("A");
        tree1.addNode("C1", null, null, data);

        Tree<Set<String>> tree2 = new Tree<>("c2");
        tree2.addNode("C2", null, null, new HashSet<>(data));

        BigDecimal similarity = reasoner.measureDirectedSimilarity(tree1, tree2);

        assertEquals(new BigDecimal("1"), similarity.stripTrailingZeros());
    }

    @Test
    void measureDirectedSimilarity_populatesBacktraceTable() {
        ABoxServiceContext abox = new ABoxServiceContext();
        abox.getIndividuals().add("ind1");
        OverlapReasoner overlapReasoner = new OverlapReasoner(abox, new ALCPreferenceProfile());
        TopDownALCSimReasonerImpl reasoner = new TopDownALCSimReasonerImpl(overlapReasoner);

        Tree<Set<String>> tree1 = new Tree<>("c1");
        Set<String> data = new HashSet<>();
        data.add("A");
        tree1.addNode("C1", null, null, data);

        Tree<Set<String>> tree2 = new Tree<>("c2");
        tree2.addNode("C2", null, null, new HashSet<>(data));

        reasoner.measureDirectedSimilarity(tree1, tree2);

        assertNotNull(reasoner.getBacktraceTable());
        assertEquals(1, reasoner.getBacktraceTable().getTable().size());
    }

    @Test
    void successiveCalls_alternateForwardAndBackwardInExplanationTable() {
        ABoxServiceContext abox = new ABoxServiceContext();
        abox.getIndividuals().add("ind1");
        OverlapReasoner overlapReasoner = new OverlapReasoner(abox, new ALCPreferenceProfile());
        TopDownALCSimReasonerImpl reasoner = new TopDownALCSimReasonerImpl(overlapReasoner);

        Tree<Set<String>> tree1 = new Tree<>("c1");
        Set<String> data1 = new HashSet<>();
        data1.add("A");
        tree1.addNode("C1", null, null, data1);

        Tree<Set<String>> tree2 = new Tree<>("c2");
        Set<String> data2 = new HashSet<>();
        data2.add("B");
        tree2.addNode("C2", null, null, data2);

        reasoner.measureDirectedSimilarity(tree1, tree2); // forward
        reasoner.measureDirectedSimilarity(tree2, tree1); // backward

        assertNotNull(reasoner.getExplanationTable().getForward());
        assertNotNull(reasoner.getExplanationTable().getBackward());
    }
}