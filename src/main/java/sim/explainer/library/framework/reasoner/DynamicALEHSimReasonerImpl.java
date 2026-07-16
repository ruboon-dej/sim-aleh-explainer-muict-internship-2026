package sim.explainer.library.framework.reasoner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sim.explainer.library.framework.PreferenceProfile;
import sim.explainer.library.framework.descriptiontree.BreadthFirstTreeIterator;
import sim.explainer.library.framework.descriptiontree.Tree;
import sim.explainer.library.framework.descriptiontree.TreeNode;
import sim.explainer.library.framework.explainer.BacktraceTable;
import sim.explainer.library.framework.explainer.SimRecord;
import sim.explainer.library.framework.unfolding.IRoleUnfolder;
import sim.explainer.library.framework.unfolding.ISubRoleUnfolder;

public class DynamicALEHSimReasonerImpl extends TopDownALEHSimReasonerImpl {

    private Map<Integer, Map<Integer, BigDecimal>> nodePairHdValMap;

    public DynamicALEHSimReasonerImpl(PreferenceProfile preferenceProfile,
                                       IRoleUnfolder iRoleUnfolder,
                                       ISubRoleUnfolder iSubRoleUnfolder) {
        super(preferenceProfile, iRoleUnfolder, iSubRoleUnfolder);
        this.nodePairHdValMap = new HashMap<>();
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    // Override to use memoized values
        private BigDecimal getStoredSim(TreeNode<Set<String>> n1, TreeNode<Set<String>> n2) {
            Map<Integer, BigDecimal> inner = nodePairHdValMap.get(n1.getId());
            return (inner != null && inner.containsKey(n2.getId())) ? inner.get(n2.getId()) : ZERO;
        }

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
                    String r = child1.getEdgeToParent();
                    String s = child2.getEdgeToParent();
                    BigDecimal gamma = eGamma(r, s);
                    BigDecimal d = dHat(r);
                    BigDecimal sub = getStoredSim(child1, child2);
                    BigDecimal val = gamma.multiply(d.add(ONE.subtract(d).multiply(sub)));
                    if (val.compareTo(max) > 0) { max = val; best = child2; }
                }
                sum = sum.add(max);
                if (best != null) record.appendExi(child1.getEdgeToParent(), best.getEdgeToParent());
            }
            return sum.divide(new BigDecimal(e1.size()), SCALE, ROUNDING_MODE);
        }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
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
                String r = child1.getEdgeToParent();
                String s = child2.getEdgeToParent();
                BigDecimal gamma = aGamma(r, s);
                BigDecimal val;

                if (child2.getData().contains("BOTTOM") || child2.getData().contains("⊥")) {
                    val = gamma;
                } else {
                    BigDecimal d = dHat(r);
                    BigDecimal sub = getStoredSim(child1, child2);
                    val = gamma.multiply(d.add(ONE.subtract(d).multiply(sub)));
                }

                if (val.compareTo(max) > 0) { max = val; best = child2; }
            }
            sum = sum.add(max);
            if (best != null) record.appendUni(child1.getEdgeToParent(), best.getEdgeToParent());
        }
        return sum.divide(new BigDecimal(a1.size()), SCALE, ROUNDING_MODE);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    @Override
    public BigDecimal measureDirectedSimilarity(Tree<Set<String>> tree1, Tree<Set<String>> tree2) {
        this.nodePairHdValMap = new HashMap<>();
        this.backtraceTable = new BacktraceTable();

        BreadthFirstTreeIterator<Set<String>> iter1 = (BreadthFirstTreeIterator<Set<String>>) tree1.iterator(0);
        BreadthFirstTreeIterator<Set<String>> iter2 = (BreadthFirstTreeIterator<Set<String>>) tree2.iterator(0);

        int height = iter1.getNodesOnEachLevel().size();

        for (int i = height - 1; i >= 0; i--) {
            List<TreeNode<Set<String>>> list1 = iter1.getNodesOnEachLevel().get(i);
            List<TreeNode<Set<String>>> list2 = iter2.getNodesOnEachLevel().get(i);
            if (list1 == null || list2 == null) continue;

            for (TreeNode<Set<String>> n1 : list1) {
                for (TreeNode<Set<String>> n2 : list2) {
                    SimRecord record = new SimRecord();
                    BigDecimal hdVal;

                    if (i == height - 1) {
                        hdVal = pHd(record, n1, n2);
                    } else {
                        BigDecimal muE = muE(n1);
                        BigDecimal muA = muA(n1);
                        BigDecimal muP = ONE.subtract(muE).subtract(muA);
                        hdVal = muP.multiply(pHd(record, n1, n2))
                                .add(muE.multiply(eSetHd(i, record, n1, n2)))
                                .add(muA.multiply(aSetHd(i, record, n1, n2)));
                    }

                    record.setDeg(hdVal);
                    nodePairHdValMap.computeIfAbsent(n1.getId(), k -> new HashMap<>()).put(n2.getId(), hdVal);
                    this.backtraceTable.addRecord(i, n1, n2, record);
                }
            }
        }
        return nodePairHdValMap.getOrDefault(0, new HashMap<>()).getOrDefault(0, ZERO);
    }
}