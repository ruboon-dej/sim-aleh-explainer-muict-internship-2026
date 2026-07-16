package sim.explainer.library.framework.reasoner;

import java.math.BigDecimal;
import java.util.Set;

import sim.explainer.library.framework.descriptiontree.Tree;

public class DynamicFL0SimReasonerImpl extends TopDownFL0SimReasonerImpl {

    public DynamicFL0SimReasonerImpl() {
        super();
    }

    @Override
    public BigDecimal measureDirectedSimilarity(Tree<Set<String>> tree1, Tree<Set<String>> tree2) {
        // FL0's word-set construction is already a single linear-time DFS pass;
        // there is no repeated subtree recomputation to memoize like in ELH/ALEH's
        // tree-homomorphism recursion, so this delegates directly to the top-down version.
        return super.measureDirectedSimilarity(tree1, tree2);
    }
}