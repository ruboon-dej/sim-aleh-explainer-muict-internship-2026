package sim.explainer.library.framework.conceptTree;

import sim.explainer.library.exception.ErrorCode;
import sim.explainer.library.exception.JSimPiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One conjunctive term D_i of a concept in ALC normal form (Def. 3.3):
 * D_i = (AND P in prim(D_i)) (AND R in N_R, ALL R.val_R(D_i)) (AND R in N_R, EXISTS R.E for E in ex_R(D_i))
 *
 * val_R(D_i) is itself a full {@link ALCConceptTree} (it may be disjunctive), and
 * there can be several existentially-restricted concepts per role, so ex_R(D_i) is
 * a list of trees per role rather than a single one.
 */
public class ALCConceptTreeNode {

    private final Set<String> primitiveConcepts = new HashSet<>();
    private final Map<String, ALCConceptTree> universalRestrictions = new HashMap<>();
    private final Map<String, List<ALCConceptTree>> existentialRestrictions = new HashMap<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void addPrimitiveConcept(String primitiveConceptName) {
        if (primitiveConceptName == null) {
            throw new JSimPiException("Unable to add primitive concept as name is null.", ErrorCode.ALCConceptTreeNode_IllegalArguments);
        }
        primitiveConcepts.add(primitiveConceptName);
    }

    public void addUniversalRestriction(String role, ALCConceptTree valueTree) {
        if (role == null || valueTree == null) {
            throw new JSimPiException("Unable to add universal restriction as role or valueTree is null.", ErrorCode.ALCConceptTreeNode_IllegalArguments);
        }
        universalRestrictions.put(role, valueTree);
    }

    public void addExistentialRestriction(String role, ALCConceptTree valueTree) {
        if (role == null || valueTree == null) {
            throw new JSimPiException("Unable to add existential restriction as role or valueTree is null.", ErrorCode.ALCConceptTreeNode_IllegalArguments);
        }
        existentialRestrictions.computeIfAbsent(role, k -> new ArrayList<>()).add(valueTree);
    }

    /** Every role name appearing in either the universal or existential restrictions of this conjunct. */
    public Set<String> getMentionedRoles() {
        Set<String> roles = new HashSet<>();
        roles.addAll(universalRestrictions.keySet());
        roles.addAll(existentialRestrictions.keySet());
        return roles;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Set<String> getPrimitiveConcepts() {
        return primitiveConcepts;
    }

    public Map<String, ALCConceptTree> getUniversalRestrictions() {
        return universalRestrictions;
    }

    public Map<String, List<ALCConceptTree>> getExistentialRestrictions() {
        return existentialRestrictions;
    }

    @Override
    public String toString() {
        return "D{prim=" + primitiveConcepts + ", forall=" + universalRestrictions.keySet() + ", exists=" + existentialRestrictions.keySet() + "}";
    }
}
