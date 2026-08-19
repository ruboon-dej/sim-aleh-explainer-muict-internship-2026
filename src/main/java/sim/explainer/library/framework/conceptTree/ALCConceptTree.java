package sim.explainer.library.framework.conceptTree;

import sim.explainer.library.exception.ErrorCode;
import sim.explainer.library.exception.JSimPiException;

import java.util.ArrayList;
import java.util.List;

/**
 * A concept in ALC normal form (Def. 3.3): either the top concept, the bottom
 * concept, or a disjunction D_1 (or) ... (or) D_n of conjunctive terms, each
 * represented by one {@link ALCConceptTreeNode}.
 */
public class ALCConceptTree {

    private boolean top;
    private boolean bottom;
    private final List<ALCConceptTreeNode> disjuncts = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ALCConceptTree topConcept() {
        ALCConceptTree tree = new ALCConceptTree();
        tree.top = true;
        return tree;
    }

    public static ALCConceptTree bottomConcept() {
        ALCConceptTree tree = new ALCConceptTree();
        tree.bottom = true;
        return tree;
    }

    public void addDisjunct(ALCConceptTreeNode disjunct) {
        if (disjunct == null) {
            throw new JSimPiException("Unable to add disjunct as it is null.", ErrorCode.ALCConceptTree_IllegalArguments);
        }
        this.disjuncts.add(disjunct);
    }

    public boolean isTop() {
        return top;
    }

    public boolean isBottom() {
        return bottom;
    }

    public List<ALCConceptTreeNode> getDisjuncts() {
        return disjuncts;
    }

    @Override
    public String toString() {
        if (top) return "TOP";
        if (bottom) return "BOTTOM";
        return disjuncts.toString();
    }
}
