package sim.explainer.library.framework.reasoner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import sim.explainer.library.exception.ErrorCode;
import sim.explainer.library.exception.JSimPiException;
import sim.explainer.library.framework.PreferenceProfile;
import sim.explainer.library.framework.descriptiontree.Tree;
import sim.explainer.library.framework.descriptiontree.TreeNode;
import sim.explainer.library.framework.explainer.BacktraceTable;
import sim.explainer.library.framework.explainer.SimRecord;
import sim.explainer.library.framework.unfolding.IRoleUnfolder;
import sim.explainer.library.framework.unfolding.ISubRoleUnfolder;

public class TopDownALEHSimReasonerImpl implements IReasoner {

    protected static final BigDecimal ZERO = BigDecimal.ZERO;
    protected static final BigDecimal ONE = BigDecimal.ONE;
    protected static final int SCALE = 5;
    protected static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private PreferenceProfile preferenceProfile;
    private IRoleUnfolder iRoleUnfolder;
    private ISubRoleUnfolder iSubRoleUnfolder;
    protected BacktraceTable backtraceTable;

    public TopDownALEHSimReasonerImpl(PreferenceProfile preferenceProfile,
                                       IRoleUnfolder iRoleUnfolder,
                                       ISubRoleUnfolder iSubRoleUnfolder) {
        this.preferenceProfile = preferenceProfile;
        this.iRoleUnfolder = iRoleUnfolder;
        this.iSubRoleUnfolder = iSubRoleUnfolder;
        this.backtraceTable = new BacktraceTable();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // mu_e: fraction of children that are existential
    protected BigDecimal muE(TreeNode<Set<String>> node) {
        int p = node.getData().size();
        int e = node.getExistentialChildren().size();
        int a = node.getUniversalChildren().size();
        int total = p + e + a;
        if (total == 0) return ZERO;
        return new BigDecimal(e).divide(new BigDecimal(total), SCALE, ROUNDING_MODE);
    }

    // mu_a: fraction of children that are universal
    protected BigDecimal muA(TreeNode<Set<String>> node) {
        int p = node.getData().size();
        int e = node.getExistentialChildren().size();
        int a = node.getUniversalChildren().size();
        int total = p + e + a;
        if (total == 0) return ZERO;
        return new BigDecimal(a).divide(new BigDecimal(total), SCALE, ROUNDING_MODE);
    }

    // p-hd (no preference): intersection / |P_D|
    protected BigDecimal pHd(SimRecord record, TreeNode<Set<String>> node1, TreeNode<Set<String>> node2) {
        Set<String> pd = node1.getData();
        Set<String> pc = node2.getData();

        if (pc.contains("BOTTOM") || pc.contains("⊥")) return ONE;
        if (pd.isEmpty()) return ONE;

        Set<String> common = Sets.intersection(pd, pc);
        for (String s : common) record.appendPri(s, s);

        return new BigDecimal(common.size())
                .divide(new BigDecimal(pd.size()), SCALE, ROUNDING_MODE);
    }

    // e-gamma (no preference): |R∃r ∩ R∃s| / |R∃r|
    protected BigDecimal eGamma(String r, String s) {
        Set<String> rSuper = iRoleUnfolder.unfoldRoleHierarchy(r);
        Set<String> sSuper = iRoleUnfolder.unfoldRoleHierarchy(s);
        if (rSuper.isEmpty()) return ONE;
        Set<String> common = Sets.intersection(rSuper, sSuper);
        return new BigDecimal(common.size())
                .divide(new BigDecimal(rSuper.size()), SCALE, ROUNDING_MODE);
    }

    // a-gamma (no preference): |R∀r ∩ R∀s| / |R∀r|
    protected BigDecimal aGamma(String r, String s) {
        Set<String> rSub = iSubRoleUnfolder.unfoldSubRoleHierarchy(r);
        Set<String> sSub = iSubRoleUnfolder.unfoldSubRoleHierarchy(s);
        if (rSub.isEmpty()) return ONE;
        Set<String> common = Sets.intersection(rSub, sSub);
        return new BigDecimal(common.size())
                .divide(new BigDecimal(rSub.size()), SCALE, ROUNDING_MODE);
    }

    protected BigDecimal dHat(String r) {
        return new BigDecimal("0.4");
    }

    // e-hd (Eq.9 without preference)
    protected  BigDecimal eHd(int level, TreeNode<Set<String>> node1, TreeNode<Set<String>> node2) {
        String r = node1.getEdgeToParent();
        String s = node2.getEdgeToParent();
        BigDecimal gamma = eGamma(r, s);
        BigDecimal d = dHat(r);
        BigDecimal sub = measureDirectedSimilarity(level + 1, node1, node2);
        return gamma.multiply(d.add(ONE.subtract(d).multiply(sub)));
    }

    // a-hd (Eq.12 without preference)
    protected BigDecimal aHd(int level, TreeNode<Set<String>> node1, TreeNode<Set<String>> node2) {
        String r = node1.getEdgeToParent();
        String s = node2.getEdgeToParent();
        BigDecimal gamma = aGamma(r, s);
        if (node2.getData().contains("BOTTOM") || node2.getData().contains("⊥")) return gamma;
        BigDecimal d = dHat(r);
        BigDecimal sub = measureDirectedSimilarity(level + 1, node1, node2);
        return gamma.multiply(d.add(ONE.subtract(d).multiply(sub)));
    }

    // e-set-hd (Eq.8 without preference)
    protected BigDecimal eSetHd(int level, SimRecord record,
                               TreeNode<Set<String>> node1, TreeNode<Set<String>> node2) {
        List<TreeNode<Set<String>>> e1 = node1.getExistentialChildren();
        List<TreeNode<Set<String>>> e2 = node2.getExistentialChildren();
        if (e1.isEmpty()) return ONE;
        if (e2.isEmpty()) return ZERO;

        BigDecimal sum = ZERO;
        for (TreeNode<Set<String>> child1 : e1) {
            BigDecimal max = ZERO;
            TreeNode<Set<String>> best = null;
            for (TreeNode<Set<String>> child2 : e2) {
                BigDecimal val = eHd(level, child1, child2);
                if (val.compareTo(max) > 0) { max = val; best = child2; }
            }
            sum = sum.add(max);
            if (best != null) record.appendExi(child1.getEdgeToParent(), best.getEdgeToParent());
        }
        return sum.divide(new BigDecimal(e1.size()), SCALE, ROUNDING_MODE);
    }

    // a-set-hd (Eq.11 without preference)
    protected BigDecimal aSetHd(int level, SimRecord record,
                               TreeNode<Set<String>> node1, TreeNode<Set<String>> node2) {
        List<TreeNode<Set<String>>> a1 = node1.getUniversalChildren();
        List<TreeNode<Set<String>>> a2 = node2.getUniversalChildren();
        if (a1.isEmpty()) return ONE;
        if (a2.isEmpty()) return ZERO;

        BigDecimal sum = ZERO;
        for (TreeNode<Set<String>> child1 : a1) {
            BigDecimal max = ZERO;
            TreeNode<Set<String>> best = null;
            for (TreeNode<Set<String>> child2 : a2) {
                BigDecimal val = aHd(level, child1, child2);
                if (val.compareTo(max) > 0) { max = val; best = child2; }
            }
            sum = sum.add(max);
            if (best != null) record.appendUni(child1.getEdgeToParent(), best.getEdgeToParent());
        }
        return sum.divide(new BigDecimal(a1.size()), SCALE, ROUNDING_MODE);
    }

    // Eq.4 without preference
    protected BigDecimal measureDirectedSimilarity(int level,
                                                    TreeNode<Set<String>> node1,
                                                    TreeNode<Set<String>> node2) {
        SimRecord record = new SimRecord();
        BigDecimal muE = muE(node1);
        BigDecimal muA = muA(node1);
        BigDecimal muP = ONE.subtract(muE).subtract(muA);

        BigDecimal result = muP.multiply(pHd(record, node1, node2))
                .add(muE.multiply(eSetHd(level, record, node1, node2)))
                .add(muA.multiply(aSetHd(level, record, node1, node2)));

        record.setDeg(result);
        backtraceTable.addRecord(level, node1, node2, record);
        return result;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public BigDecimal measureDirectedSimilarity(Tree<Set<String>> tree1, Tree<Set<String>> tree2) {
        if (tree1 == null || tree2 == null)
            throw new JSimPiException("Trees cannot be null.", ErrorCode.Application_IllegalArguments);
        this.backtraceTable = new BacktraceTable();
        return measureDirectedSimilarity(0, tree1.getNodes().get(0), tree2.getNodes().get(0));
    }

    @Override
    public BacktraceTable getBacktraceTable() { return backtraceTable; }

    @Override
    public void setRoleUnfoldingStrategy(IRoleUnfolder iRoleUnfolder) { this.iRoleUnfolder = iRoleUnfolder; }

    public void setSubRoleUnfoldingStrategy(ISubRoleUnfolder iSubRoleUnfolder) { this.iSubRoleUnfolder = iSubRoleUnfolder; }

    @Override
    public List<String> getExecutionTimes() { return null; }
    protected IRoleUnfolder getRoleUnfolder() { return iRoleUnfolder; }
    protected ISubRoleUnfolder getSubRoleUnfolder() { return iSubRoleUnfolder; }
    protected PreferenceProfile getPreferenceProfile() { return preferenceProfile; }
}