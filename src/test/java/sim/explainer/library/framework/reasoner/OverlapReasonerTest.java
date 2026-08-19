package sim.explainer.library.framework.reasoner;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import sim.explainer.library.framework.ABoxServiceContext;
import sim.explainer.library.framework.ALCPreferenceProfile;
import sim.explainer.library.framework.descriptiontree.Tree;
import sim.explainer.library.framework.descriptiontree.TreeNode;

class OverlapReasonerTest {

    @Test
    void identicalConcepts_haveZeroDissimilarity() {
        ABoxServiceContext abox = new ABoxServiceContext();
        abox.getIndividuals().add("individual1");
        ALCPreferenceProfile profile = new ALCPreferenceProfile();
        OverlapReasoner reasoner = new OverlapReasoner(abox, profile);

        Tree<Set<String>> tree = new Tree<>("test");
        Set<String> data = new HashSet<>();
        data.add("PizzaTopping");
        TreeNode<Set<String>> node = tree.addNode("PizzaTopping", null, null, data);

        double dissimilarity = reasoner.computeDissimilarity(node, node);

        assertEquals(0.0, dissimilarity, 0.0001);
    }

    @Test
    void oneSideUnsatisfiable_hasFullDissimilarity() {
        ABoxServiceContext abox = new ABoxServiceContext();
        abox.getIndividuals().add("individual1");
        ALCPreferenceProfile profile = new ALCPreferenceProfile();
        OverlapReasoner reasoner = new OverlapReasoner(abox, profile);

        Tree<Set<String>> tree = new Tree<>("test");
        Set<String> satisfiableData = new HashSet<>();
        satisfiableData.add("PizzaTopping");
        TreeNode<Set<String>> satisfiable = tree.addNode("PizzaTopping", null, null, satisfiableData);

        Tree<Set<String>> tree2 = new Tree<>("test2");
        Set<String> bottomData = new HashSet<>();
        bottomData.add("BOTTOM");
        TreeNode<Set<String>> bottom = tree2.addNode("Bottom", null, null, bottomData);

        double dissimilarity = reasoner.computeDissimilarity(satisfiable, bottom);

        assertEquals(1.0, dissimilarity, 0.0001);
    }

    @Test
    void differentSatisfiableConcepts_haveDissimilarityStrictlyBetweenZeroAndOne() {
        ABoxServiceContext abox = new ABoxServiceContext();
        abox.getIndividuals().add("individual1");
        abox.getIndividuals().add("individual2");
        ALCPreferenceProfile profile = new ALCPreferenceProfile();
        OverlapReasoner reasoner = new OverlapReasoner(abox, profile);

        Tree<Set<String>> tree1 = new Tree<>("concept1");
        Set<String> data1 = new HashSet<>();
        data1.add("PizzaTopping");
        TreeNode<Set<String>> node1 = tree1.addNode("Concept1", null, null, data1);

        Tree<Set<String>> tree2 = new Tree<>("concept2");
        Set<String> data2 = new HashSet<>();
        data2.add("Pizza");
        TreeNode<Set<String>> node2 = tree2.addNode("Concept2", null, null, data2);

        double dissimilarity = reasoner.computeDissimilarity(node1, node2);

        assertTrue(dissimilarity > 0.0 && dissimilarity < 1.0,
                "Expected dissimilarity strictly between 0 and 1, was " + dissimilarity);
    }

    @Test
    void disjunctiveUnion_comparesAllDisjunctPairs() {
        ABoxServiceContext abox = new ABoxServiceContext();
        abox.getIndividuals().add("individual1");
        ALCPreferenceProfile profile = new ALCPreferenceProfile();
        OverlapReasoner reasoner = new OverlapReasoner(abox, profile);

        // Left side: a 2-disjunct union (A or B)
        Tree<Set<String>> leftTree = new Tree<>("left");
        TreeNode<Set<String>> leftOrNode = leftTree.addNode("left", null, null, new HashSet<>());
        Set<String> aData = new HashSet<>();
        aData.add("A");
        leftTree.addNode("A", "root", leftOrNode, aData, "DISJUNCT");
        Set<String> bData = new HashSet<>();
        bData.add("B");
        leftTree.addNode("B", "root", leftOrNode, bData, "DISJUNCT");

        // Right side: a 3-disjunct union (C or D or E)
        Tree<Set<String>> rightTree = new Tree<>("right");
        TreeNode<Set<String>> rightOrNode = rightTree.addNode("right", null, null, new HashSet<>());
        Set<String> cData = new HashSet<>();
        cData.add("C");
        rightTree.addNode("C", "root", rightOrNode, cData, "DISJUNCT");
        Set<String> dData = new HashSet<>();
        dData.add("D");
        rightTree.addNode("D", "root", rightOrNode, dData, "DISJUNCT");
        Set<String> eData = new HashSet<>();
        eData.add("E");
        rightTree.addNode("E", "root", rightOrNode, eData, "DISJUNCT");

        // Should not throw, and should produce a real (non-crashing) overlap value -
        // the actual pairs-compared count (2x3=6) is only visible via the traced
        // explanation (ALCExplanationRecord.getDisjunctPairsCompared()); this call
        // just confirms computeOverlap handles multi-disjunct trees without error.
        double overlap = reasoner.computeOverlap(leftOrNode, rightOrNode);

        assertTrue(overlap >= 0.0);
    }

    @Test
    void tracedExplanation_recordsCorrectDisjunctPairCount() {
        ABoxServiceContext abox = new ABoxServiceContext();
        abox.getIndividuals().add("individual1");
        ALCPreferenceProfile profile = new ALCPreferenceProfile();
        OverlapReasoner reasoner = new OverlapReasoner(abox, profile);

        Tree<Set<String>> leftTree = new Tree<>("left");
        TreeNode<Set<String>> leftOrNode = leftTree.addNode("left", null, null, new HashSet<>());
        Set<String> aData = new HashSet<>();
        aData.add("A");
        leftTree.addNode("A", "root", leftOrNode, aData, "DISJUNCT");
        Set<String> bData = new HashSet<>();
        bData.add("B");
        leftTree.addNode("B", "root", leftOrNode, bData, "DISJUNCT");

        Tree<Set<String>> rightTree = new Tree<>("right");
        TreeNode<Set<String>> rightOrNode = rightTree.addNode("right", null, null, new HashSet<>());
        Set<String> cData = new HashSet<>();
        cData.add("C");
        rightTree.addNode("C", "root", rightOrNode, cData, "DISJUNCT");
        Set<String> dData = new HashSet<>();
        dData.add("D");
        rightTree.addNode("D", "root", rightOrNode, dData, "DISJUNCT");

        sim.explainer.library.framework.explainer.ALCExplanationResult result =
                reasoner.computeDissimilarityWithExplanation(leftOrNode, rightOrNode);

        // 2 disjuncts x 2 disjuncts = 4 pairs should have been compared
        assertEquals(4, result.getTrace().getDisjunctPairsCompared());
    }

    @Test
    void bidirectionalExplanation_computesForwardAndBackwardIndependently() {
        ABoxServiceContext abox = new ABoxServiceContext();
        abox.getIndividuals().add("individual1");
        abox.getIndividuals().add("individual2");
        ALCPreferenceProfile profile = new ALCPreferenceProfile();
        OverlapReasoner reasoner = new OverlapReasoner(abox, profile);

        Tree<Set<String>> tree1 = new Tree<>("c1");
        Set<String> data1 = new HashSet<>();
        data1.add("A");
        TreeNode<Set<String>> node1 = tree1.addNode("C1", null, null, data1);

        Tree<Set<String>> tree2 = new Tree<>("c2");
        Set<String> data2 = new HashSet<>();
        data2.add("B");
        TreeNode<Set<String>> node2 = tree2.addNode("C2", null, null, data2);

        sim.explainer.library.framework.explainer.ALCBidirectionalExplanationResult result =
                reasoner.computeDissimilarityWithExplanationBothDirections(node1, node2);

        assertTrue(result.getForward() != null && result.getBackward() != null);
    }
}