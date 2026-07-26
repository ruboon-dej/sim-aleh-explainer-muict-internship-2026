package sim.explainer.library.framework.reasoner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sim.explainer.library.framework.descriptiontree.Tree;
import sim.explainer.library.framework.descriptiontree.TreeNode;
import sim.explainer.library.framework.explainer.BacktraceTable;
import sim.explainer.library.framework.explainer.FL0BacktraceTable;
import sim.explainer.library.framework.explainer.FL0Record;
import sim.explainer.library.framework.explainer.SimRecord;
import sim.explainer.library.framework.unfolding.IRoleUnfolder;

public class TopDownFL0SimReasonerImpl implements IReasoner, IFlatExplainableReasoner<FL0BacktraceTable>{

    protected FL0BacktraceTable fl0BacktraceTable = new FL0BacktraceTable();
    protected BacktraceTable backtraceTable = new BacktraceTable();
    protected Set<String> primitiveConceptUniverse = null;

    public TopDownFL0SimReasonerImpl() {
    }

    protected Map<String, Set<List<String>>> buildWordSets(TreeNode<Set<String>> root) {
        Map<String, Set<List<String>>> wordSets = new HashMap<>();
        collectWords(root, new ArrayList<>(), wordSets);
        return wordSets;
    }

    private void collectWords(TreeNode<Set<String>> node, List<String> currentPath,
                               Map<String, Set<List<String>>> wordSets) {
        for (String primitive : node.getData()) {
            wordSets.computeIfAbsent(primitive, k -> new HashSet<>()).add(new ArrayList<>(currentPath));
        }
        for (TreeNode<Set<String>> child : node.getUniversalChildren()) {
            List<String> nextPath = new ArrayList<>(currentPath);
            nextPath.add(child.getEdgeToParent());
            collectWords(child, nextPath, wordSets);
        }
    }

    protected BigDecimal measureDirectedSimilaritySim(SimRecord record,
                                                    Map<String, Set<List<String>>> wC,
                                                    Map<String, Set<List<String>>> wD,
                                                    Set<String> allPrimitives,
                                                    boolean isForward) {

        int total = allPrimitives.size();
        if (total == 0) return BigDecimal.ONE;

        int matchCount = 0;

        for (String P : allPrimitives) {
            Set<List<String>> wdp = wD.getOrDefault(P, Collections.emptySet());
            Set<List<String>> wcp = wC.getOrDefault(P, Collections.emptySet());

            boolean included = wcp.containsAll(wdp);

            if (included) {
                matchCount++;
                record.appendPri(P, P);
            }

            FL0Record fl0Record = new FL0Record(P, included ? P : null,
                    included ? BigDecimal.ONE : BigDecimal.ZERO, included);

            if (isForward) {
                fl0BacktraceTable.addForwardRecord(P, fl0Record);
            } else {
                fl0BacktraceTable.addBackwardRecord(P, fl0Record);
            }
        }

        return new BigDecimal(matchCount).divide(new BigDecimal(total), 5, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal measureDirectedSimilarity(Tree<Set<String>> tree1, Tree<Set<String>> tree2) {
        this.backtraceTable = new BacktraceTable();

        if (fl0BacktraceTable == null) {
            fl0BacktraceTable = new FL0BacktraceTable();
        }
        boolean isForward = fl0BacktraceTable.getForwardRecords().isEmpty()
                        && fl0BacktraceTable.getBackwardRecords().isEmpty();

        TreeNode<Set<String>> rootC = tree1.getNodes().get(0);
        TreeNode<Set<String>> rootD = tree2.getNodes().get(0);

        Map<String, Set<List<String>>> wC = buildWordSets(rootC);
        Map<String, Set<List<String>>> wD = buildWordSets(rootD);

        Set<String> allPrimitives = new HashSet<>();
        if (primitiveConceptUniverse != null) {
            allPrimitives.addAll(primitiveConceptUniverse);
        }
        allPrimitives.addAll(wC.keySet());
        allPrimitives.addAll(wD.keySet());

        SimRecord record = new SimRecord();
        BigDecimal result = measureDirectedSimilaritySim(record, wC, wD, allPrimitives, isForward);
        record.setDeg(result);
        backtraceTable.addRecord(0, rootC, rootD, record);

        if (isForward) {
            fl0BacktraceTable.setForwardConcepts(rootC.getConceptName(), rootD.getConceptName());
            fl0BacktraceTable.setForwardDeg(result);
        } else {
            fl0BacktraceTable.setBackwardConcepts(rootC.getConceptName(), rootD.getConceptName());
            fl0BacktraceTable.setBackwardDeg(result);
        }

        return result;
    }

    @Override
    public FL0BacktraceTable getExplanationTable() {
        return fl0BacktraceTable;
    }

    @Override
    public BacktraceTable getBacktraceTable() { return backtraceTable; }

    public void setPrimitiveConceptUniverse(Set<String> universe) {
        this.primitiveConceptUniverse = universe;
    }

    public void resetFl0BacktraceTable() {
        this.fl0BacktraceTable = new FL0BacktraceTable();
    }

    @Override
    public void setRoleUnfoldingStrategy(IRoleUnfolder iRoleUnfolder) {
        // not needed for FL0
    }
    
    public FL0BacktraceTable getFl0BacktraceTable() {
        return fl0BacktraceTable;
    }

    @Override
    public List<String> getExecutionTimes() { return new ArrayList<>(); }
}