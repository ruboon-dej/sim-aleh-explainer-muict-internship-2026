package sim.explainer.library.framework.reasoner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sim.explainer.library.exception.ErrorCode;
import sim.explainer.library.exception.JSimPiException;
import sim.explainer.library.framework.PreferenceProfile;
import sim.explainer.library.framework.descriptiontree.Tree;
import sim.explainer.library.framework.descriptiontree.TreeNode;
import sim.explainer.library.framework.explainer.BacktraceTable;
import sim.explainer.library.framework.explainer.SimRecord;
import sim.explainer.library.framework.unfolding.IRoleUnfolder;
import sim.explainer.library.framework.unfolding.ISubRoleUnfolder;

public class TopDownALEHSimPiReasonerImpl implements IReasoner {

    protected static final BigDecimal ZERO = BigDecimal.ZERO;
    protected static final BigDecimal ONE = BigDecimal.ONE;
    protected static final BigDecimal TWO = new BigDecimal(2);
    protected static final int SCALE = 5;
    protected static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private PreferenceProfile preferenceProfile;
    private IRoleUnfolder iRoleUnfolder;
    private ISubRoleUnfolder iSubRoleUnfolder;
    protected BacktraceTable backtraceTable;

    public TopDownALEHSimPiReasonerImpl(PreferenceProfile preferenceProfile,
                                         IRoleUnfolder iRoleUnfolder,
                                         ISubRoleUnfolder iSubRoleUnfolder) {
        this.preferenceProfile = preferenceProfile;
        this.iRoleUnfolder = iRoleUnfolder;
        this.iSubRoleUnfolder = iSubRoleUnfolder;
        this.backtraceTable = new BacktraceTable();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private BigDecimal sumImportancePrimitives(TreeNode<Set<String>> node) {
        BigDecimal sum = ZERO;
        for (String p : node.getData()) {
            sum = sum.add(iHat(p));
        }
        return sum;
    }

    private BigDecimal sumImportanceExistentials(TreeNode<Set<String>> node) {
        BigDecimal sum = ZERO;
        for (TreeNode<Set<String>> child : node.getExistentialChildren()) {
            sum = sum.add(iHat(child.getEdgeToParent()));
        }
        return sum;
    }

    private BigDecimal sumImportanceUniversals(TreeNode<Set<String>> node) {
        BigDecimal sum = ZERO;
        for (TreeNode<Set<String>> child : node.getUniversalChildren()) {
            sum = sum.add(iHat(child.getEdgeToParent()));
        }
        return sum;
    }
    
    // Eq.1 — importance function î(x)
    private BigDecimal iHat(String x) {
        Map<String, BigDecimal> conceptImportance = preferenceProfile.getPrimitiveConceptImportance();
        if (conceptImportance != null && conceptImportance.containsKey(x)) return conceptImportance.get(x);
        Map<String, BigDecimal> roleImportance = preferenceProfile.getRoleImportance();
        if (roleImportance != null && roleImportance.containsKey(x)) return roleImportance.get(x);
        return ONE;
    }

    // Eq.2 — similarity function ŝ(x,y)
    private BigDecimal sHat(String x, String y) {
        if (x.equals(y)) return ONE;

        // handle negation: ŝ(¬A, B) = 1 - ŝ(A, B)
        boolean xNegated = x.startsWith("NOT_");
        boolean yNegated = y.startsWith("NOT_");

        String cleanX = xNegated ? x.substring(4) : x;
        String cleanY = yNegated ? y.substring(4) : y;

        BigDecimal base = null;
        if (cleanX.equals(cleanY)) {
            base = ONE;
        } else {
            Map<String, Map<String, BigDecimal>> simMap = preferenceProfile.getPrimitiveConceptsSimilarity();
            base = ZERO;
            if (simMap != null && simMap.containsKey(cleanX) && simMap.get(cleanX) != null) {
                BigDecimal v = simMap.get(cleanX).get(cleanY);
                if (v != null) base = v;
            }
        }
        if (xNegated != yNegated) base = ONE.subtract(base);
        return base.max(ZERO);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Eq.3 — discount factor d̂(r)
    protected BigDecimal dHat(String r) {
        Map<String, BigDecimal> discountMap = preferenceProfile.getRoleDiscountFactor();
        BigDecimal val = (discountMap != null) ? discountMap.get(r) : null;
        return (val != null) ? val : new BigDecimal("0.4");
    }

    // Eq.5 — µe
    protected BigDecimal muE(TreeNode<Set<String>> node) {
        BigDecimal sumP = sumImportancePrimitives(node);
        BigDecimal sumE = sumImportanceExistentials(node);
        BigDecimal sumA = sumImportanceUniversals(node);
        BigDecimal total = sumP.add(sumE).add(sumA);
        if (total.compareTo(ZERO) == 0) return ZERO;
        return sumE.divide(total, SCALE, ROUNDING_MODE);
    }

    // Eq.6 — µa
    protected BigDecimal muA(TreeNode<Set<String>> node) {
        BigDecimal sumP = sumImportancePrimitives(node);
        BigDecimal sumE = sumImportanceExistentials(node);
        BigDecimal sumA = sumImportanceUniversals(node);
        BigDecimal total = sumP.add(sumE).add(sumA);
        if (total.compareTo(ZERO) == 0) return ZERO;
        return sumA.divide(total, SCALE, ROUNDING_MODE);
    }

    // Eq.7 — p-hd^π
    protected BigDecimal pHdPi(SimRecord record, TreeNode<Set<String>> node1, TreeNode<Set<String>> node2) {
        Set<String> pd = node1.getData();
        Set<String> pc = node2.getData();

        BigDecimal sumImportancePd = sumImportancePrimitives(node1);

        // P_C = {⊥} case
        if (pc.contains("⊥") || pc.contains("BOTTOM")) return ONE;

        if (sumImportancePd.compareTo(ZERO) == 0) return ONE;

        BigDecimal sumPC = sumImportancePrimitives(node2);
        if (sumPC.compareTo(ZERO) == 0) return ZERO;

        BigDecimal sum = ZERO;
        for (String a : pd) {
            BigDecimal maxSim = ZERO;
            String bestMatch = null;
            for (String b : pc) {
                BigDecimal sim = sHat(a, b);
                if (sim.compareTo(maxSim) > 0) {
                    maxSim = sim;
                    bestMatch = b;
                }
            }
            sum = sum.add(iHat(a).multiply(maxSim));
            if (bestMatch != null) record.appendPri(a, bestMatch);
        }

        return sum.divide(sumImportancePd, SCALE, ROUNDING_MODE);
    }

    // Eq.10 — e-γ^π
    protected BigDecimal eGamma(String r, String s) {
        Set<String> rSuperRoles = iRoleUnfolder.unfoldRoleHierarchy(r);
        Set<String> sSuperRoles = iRoleUnfolder.unfoldRoleHierarchy(s);

        BigDecimal sumR = ZERO;
        for (String rr : rSuperRoles) sumR = sumR.add(iHat(rr));

        if (sumR.compareTo(ZERO) == 0) return ONE;

        BigDecimal sum = ZERO;
        for (String rPrime : rSuperRoles) {
            BigDecimal maxSim = ZERO;
            for (String sPrime : sSuperRoles) {
                BigDecimal sim = sHat(rPrime, sPrime);
                if (sim.compareTo(maxSim) > 0) maxSim = sim;
            }
            sum = sum.add(iHat(rPrime).multiply(maxSim));
        }

        return sum.divide(sumR, SCALE, ROUNDING_MODE);
    }

    // Eq.13 — a-γ^π
    protected BigDecimal aGamma(String r, String s) {
        Set<String> rSubRoles = iSubRoleUnfolder.unfoldSubRoleHierarchy(r);
        Set<String> sSubRoles = iSubRoleUnfolder.unfoldSubRoleHierarchy(s);

        BigDecimal sumR = ZERO;
        for (String rr : rSubRoles) sumR = sumR.add(iHat(rr));

        if (sumR.compareTo(ZERO) == 0) return ONE;

        BigDecimal sum = ZERO;
        for (String rPrime : rSubRoles) {
            BigDecimal maxSim = ZERO;
            for (String sPrime : sSubRoles) {
                BigDecimal sim = sHat(rPrime, sPrime);
                if (sim.compareTo(maxSim) > 0) maxSim = sim;
            }
            sum = sum.add(iHat(rPrime).multiply(maxSim));
        }

        return sum.divide(sumR, SCALE, ROUNDING_MODE);
    }

    // Eq.9 — e-hd^π
    protected BigDecimal eHdPi(int level, SimRecord record,
                              TreeNode<Set<String>> node1, TreeNode<Set<String>> node2) {
        String r = node1.getEdgeToParent();
        String s = node2.getEdgeToParent();
        BigDecimal gamma = eGamma(r, s);
        BigDecimal d = dHat(r);
        BigDecimal subTree = measureDirectedSimilarity(level + 1, node1, node2);
        return gamma.multiply(d.add(ONE.subtract(d).multiply(subTree)));
    }

    // Eq.8 — e-set-hd^π
    protected BigDecimal eSetHdPi(int level, SimRecord record,
                                 TreeNode<Set<String>> node1, TreeNode<Set<String>> node2) {
        List<TreeNode<Set<String>>> eChildren1 = node1.getExistentialChildren();
        List<TreeNode<Set<String>>> eChildren2 = node2.getExistentialChildren();

        BigDecimal sumImportanceE1 = ZERO;
        for (TreeNode<Set<String>> child : eChildren1) sumImportanceE1 = sumImportanceE1.add(iHat(child.getEdgeToParent()));

        if (sumImportanceE1.compareTo(ZERO) == 0) return ONE;

        BigDecimal sumImportanceE2 = ZERO;
        for (TreeNode<Set<String>> child : eChildren2) sumImportanceE2 = sumImportanceE2.add(iHat(child.getEdgeToParent()));

        if (sumImportanceE2.compareTo(ZERO) == 0) return ZERO;

        BigDecimal sum = ZERO;
        for (TreeNode<Set<String>> child1 : eChildren1) {
            BigDecimal maxVal = ZERO;
            TreeNode<Set<String>> bestMatch = null;
            for (TreeNode<Set<String>> child2 : eChildren2) {
                BigDecimal val = eHdPi(level, record, child1, child2);
                if (val.compareTo(maxVal) > 0) {
                    maxVal = val;
                    bestMatch = child2;
                }
            }
            sum = sum.add(iHat(child1.getEdgeToParent()).multiply(maxVal));
            if (bestMatch != null) record.appendExi(child1.getEdgeToParent(), bestMatch.getEdgeToParent());
        }

        return sum.divide(sumImportanceE1, SCALE, ROUNDING_MODE);
    }

    // Eq.12 — a-hd^π
    protected BigDecimal aHdPi(int level, SimRecord record,
                              TreeNode<Set<String>> node1, TreeNode<Set<String>> node2) {
        String r = node1.getEdgeToParent();
        String s = node2.getEdgeToParent();
        BigDecimal gamma = aGamma(r, s);

        // special case: P_Y = {⊥}
        if (node2.getData().contains("⊥") || node2.getData().contains("BOTTOM")) {
            return gamma;
        }

        BigDecimal d = dHat(r);
        BigDecimal subTree = measureDirectedSimilarity(level + 1, node1, node2);
        return gamma.multiply(d.add(ONE.subtract(d).multiply(subTree)));
    }

    // Eq.11 — a-set-hd^π
    protected BigDecimal aSetHdPi(int level, SimRecord record,
                                 TreeNode<Set<String>> node1, TreeNode<Set<String>> node2) {
        List<TreeNode<Set<String>>> aChildren1 = node1.getUniversalChildren();
        List<TreeNode<Set<String>> > aChildren2 = node2.getUniversalChildren();

        BigDecimal sumImportanceA1 = ZERO;
        for (TreeNode<Set<String>> child : aChildren1) sumImportanceA1 = sumImportanceA1.add(iHat(child.getEdgeToParent()));

        if (sumImportanceA1.compareTo(ZERO) == 0) return ONE;

        BigDecimal sumImportanceA2 = ZERO;
        for (TreeNode<Set<String>> child : aChildren2) sumImportanceA2 = sumImportanceA2.add(iHat(child.getEdgeToParent()));

        if (sumImportanceA2.compareTo(ZERO) == 0) return ZERO;

        BigDecimal sum = ZERO;
        for (TreeNode<Set<String>> child1 : aChildren1) {
            BigDecimal maxVal = ZERO;
            TreeNode<Set<String>> bestMatch = null;
            for (TreeNode<Set<String>> child2 : aChildren2) {
                BigDecimal val = aHdPi(level, record, child1, child2);
                if (val.compareTo(maxVal) > 0) {
                    maxVal = val;
                    bestMatch = child2;
                }
            }
            sum = sum.add(iHat(child1.getEdgeToParent()).multiply(maxVal));
            if (bestMatch != null) record.appendUni(child1.getEdgeToParent(), bestMatch.getEdgeToParent());
        }

        return sum.divide(sumImportanceA1, SCALE, ROUNDING_MODE);
    }

    // Eq.4 — hd^π (three-way split)
    protected BigDecimal measureDirectedSimilarity(int level,
                                                    TreeNode<Set<String>> node1,
                                                    TreeNode<Set<String>> node2) {
        SimRecord record = new SimRecord();

        BigDecimal muE = muE(node1);
        BigDecimal muA = muA(node1);
        BigDecimal muP = ONE.subtract(muE).subtract(muA);

        BigDecimal pHdVal = pHdPi(record, node1, node2);
        BigDecimal eSetHdVal = eSetHdPi(level, record, node1, node2);
        BigDecimal aSetHdVal = aSetHdPi(level, record, node1, node2);

        BigDecimal result = muP.multiply(pHdVal)
                .add(muE.multiply(eSetHdVal))
                .add(muA.multiply(aSetHdVal));

        record.setDeg(result);
        backtraceTable.addRecord(level, node1, node2, record);

        return result;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public BigDecimal measureDirectedSimilarity(Tree<Set<String>> tree1, Tree<Set<String>> tree2) {
        if (tree1 == null || tree2 == null) {
            throw new JSimPiException("Unable to measure directed similarity as tree is null.",
                    ErrorCode.Application_IllegalArguments);
        }
        this.backtraceTable = new BacktraceTable();
        return measureDirectedSimilarity(0, tree1.getNodes().get(0), tree2.getNodes().get(0));
    }

    @Override
    public BacktraceTable getBacktraceTable() {
        return backtraceTable;
    }

    @Override
    public void setRoleUnfoldingStrategy(IRoleUnfolder iRoleUnfolder) {
        this.iRoleUnfolder = iRoleUnfolder;
    }
    
    public void setSubRoleUnfoldingStrategy(ISubRoleUnfolder iSubRoleUnfolder) {
        this.iSubRoleUnfolder = iSubRoleUnfolder;
    }

    @Override
    public List<String> getExecutionTimes() {
        return null;
    }
}