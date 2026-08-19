package sim.explainer.library.framework.descriptiontree;

import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class TreeBuilderTest {

    @Test
    void nonDisjunctiveManchesterConcept_hasNoDisjunctChildren() {
        TreeBuilder builder = new TreeBuilder();
        Tree<Set<String>> tree = builder.constructAccordingToManchesterSyntax(
                new HashMap<>(), "TestConcept", "A and hasRole some B");

        TreeNode<Set<String>> root = tree.getNodes().get(0);
        assertTrue(root.getDisjunctChildren().isEmpty());
    }

    @Test
    void disjunctiveManchesterFiller_producesOrNodeWithMatchingDisjunctCount() {
        TreeBuilder builder = new TreeBuilder();
        Tree<Set<String>> tree = builder.constructAccordingToManchesterSyntax(
                new HashMap<>(), "TestConcept", "hasRole only (A or B or C)");

        TreeNode<Set<String>> root = tree.getNodes().get(0);
        TreeNode<Set<String>> universalChild = root.getUniversalChildren().get(0);

        assertEquals(3, universalChild.getDisjunctChildren().size());
    }
}