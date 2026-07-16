package sim.explainer.library.framework.reasoner;

import java.math.BigDecimal;
import java.util.Set;

import sim.explainer.library.framework.PreferenceProfile;
import sim.explainer.library.framework.descriptiontree.Tree;

public class DynamicFL0SimPiReasonerImpl extends TopDownFL0SimPiReasonerImpl {

    public DynamicFL0SimPiReasonerImpl(PreferenceProfile preferenceProfile) {
        super(preferenceProfile);
    }

    @Override
    public BigDecimal measureDirectedSimilarity(Tree<Set<String>> tree1, Tree<Set<String>> tree2) {
        // Same reasoning as DynamicFL0SimReasonerImpl — no repeated subtree
        // recomputation exists in the FL0 word-set formula to memoize.
        return super.measureDirectedSimilarity(tree1, tree2);
    }
}