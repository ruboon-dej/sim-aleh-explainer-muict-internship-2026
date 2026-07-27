package sim.explainer.library.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import sim.explainer.library.enumeration.ReasoningDirectionConstant;
import sim.explainer.library.exception.ErrorCode;
import sim.explainer.library.exception.JSimPiException;
import sim.explainer.library.framework.descriptiontree.TreeNode;
import sim.explainer.library.framework.explainer.BacktraceTable;
import sim.explainer.library.framework.explainer.SimRecord;
import sim.explainer.library.util.MyStringUtils;
import sim.explainer.library.util.utilstructure.SymmetricPair;

/**
 * Service class to provide explanation functionalities for similarity measures and reasoning.
 */
@Service
public class ExplanationService {
    private BigDecimal similarity;
    private BacktraceTable forwardBacktraceTable;
    private BacktraceTable backwardBacktraceTable;
    
    private final ExplanationConverterService explanationConverterService;

    /**
     * Constructs an {@code ExplanationService} with the given similarity and backtrace tables.
     *
     * @param similarity the similarity score between two concepts
     * @param forwardBacktraceTable the forward backtrace table
     * @param backwardBacktraceTable the backward backtrace table
     */
    public ExplanationService(BigDecimal similarity, BacktraceTable forwardBacktraceTable, BacktraceTable backwardBacktraceTable, ExplanationConverterService explanationConverterService) {
        this.similarity = similarity;
        this.forwardBacktraceTable = forwardBacktraceTable;
        this.backwardBacktraceTable = backwardBacktraceTable;
        this.explanationConverterService = explanationConverterService;
    }

    /**
     * Generates an ASCII representation of the tree hierarchy for the given concept.
     *
     * @param concept the concept to generate the tree hierarchy for
     * @return the ASCII representation of the tree hierarchy
     * @throws JSimPiException if the tree for the concept is not found
     */
    public String treeHierarchy(String concept, boolean includeFreshConceptName) {
        TreeNode<Set<String>> root = findRootNode(concept);
        if (root == null) throw new JSimPiException("Tree not found", ErrorCode.Application_IllegalArguments);
        StringBuilder result = new StringBuilder();
        buildTreeAscii(root, result, "", true, includeFreshConceptName);
        return result.toString();
    }
    public String treeHierarchy(String concept) { return treeHierarchy(concept, true); }

    private void buildTreeAscii(TreeNode<Set<String>> node, StringBuilder result, String prefix, boolean isTail, boolean includeFreshConceptName) {
        String label = node.getEdgeToParent() == null ? node.getConceptName() : node.getEdgeToParent();
        if (!includeFreshConceptName) label = MyStringUtils.stripFresh(label);
        result.append(prefix).append(isTail ? "└── " : "├── ")
                .append(label).append(" : ").append(node.getData()).append("\n");
        for (int i = 0; i < node.getChildren().size() - 1; i++) {
            buildTreeAscii(node.getChildren().get(i), result, prefix + (isTail ? "    " : "│   "), false, includeFreshConceptName);
        }
        if (node.getChildren().size() > 0) {
            buildTreeAscii(node.getChildren().get(node.getChildren().size() - 1), result, prefix + (isTail ? "    " : "│   "), true, includeFreshConceptName);
        }
    }

    public JSONObject treeHierarchyAsJson(String concept, boolean includeFreshConceptName) {
        TreeNode<Set<String>> root = findRootNode(concept);
        if (root == null) throw new JSimPiException("Tree not found", ErrorCode.Application_IllegalArguments);
        root = root.copy();
        root.setEdgeToParent(null);
        return buildTreeHierarchyAsJson(root, includeFreshConceptName);
    }
    public JSONObject treeHierarchyAsJson(String concept) { return treeHierarchyAsJson(concept, true); }

    private JSONObject buildTreeHierarchyAsJson(TreeNode<Set<String>> node, boolean includeFreshConceptName) {
        JSONObject jsonObject = new JSONObject();
        String role = node.getEdgeToParent();
        String cname = node.getConceptName();
        if (!includeFreshConceptName) { role = MyStringUtils.stripFresh(role); cname = MyStringUtils.stripFresh(cname); }
        jsonObject.put("roleName", role);
        jsonObject.put("conceptName", cname);

        Set<String> data = node.getData();
        if (!includeFreshConceptName) {
            Set<String> stripped = new java.util.LinkedHashSet<>();
            for (String s : data) stripped.add(MyStringUtils.stripFresh(s));
            data = stripped;
        }
        jsonObject.put("primitiveConcepts", new JSONArray(data));

        JSONArray existentials = new JSONArray();
        for (TreeNode<Set<String>> child : node.getChildren()) {
            existentials.put(buildTreeHierarchyAsJson(child, includeFreshConceptName));
        }
        jsonObject.put("existentials", existentials);
        return jsonObject;
    }

    public String explanationTree(ReasoningDirectionConstant direction, boolean includeFreshConceptName) {
        BacktraceTable backtraceTable = getBacktraceTable(direction);
        StringBuilder result = new StringBuilder();
        HashMap<SymmetricPair<TreeNode<Set<String>>>, SimRecord> levelMap = backtraceTable.getTable().get(0);
        if (levelMap == null) return "No data available at level 0.";
        for (SymmetricPair<TreeNode<Set<String>>> rootPair : levelMap.keySet()) {
            buildExplanationTreeAscii(backtraceTable, rootPair.getFirst(), result, "", true, 0, includeFreshConceptName);
        }
        return result.toString();
    }
    public String explanationTree(ReasoningDirectionConstant direction) { return explanationTree(direction, true); }

    private void buildExplanationTreeAscii(BacktraceTable backtraceTable, TreeNode<Set<String>> node, StringBuilder result, String prefix, boolean isTail, int level, boolean includeFreshConceptName) {
        if (!backtraceTable.getTable().containsKey(level)) return;
        SymmetricPair<TreeNode<Set<String>>> pair = backtraceTable.getTable().get(level).keySet().stream()
                .filter(p -> p.getFirst().equals(node) || p.getSecond().equals(node))
                .findFirst().orElse(null);
        if (pair == null) return;

        TreeNode<Set<String>> comparingNode = pair.getFirst().equals(node) ? pair.getSecond() : pair.getFirst();
        SimRecord simRecord = backtraceTable.getTable().get(level).get(pair);

        String n1 = includeFreshConceptName ? node.getConceptName() : MyStringUtils.stripFresh(node.getConceptName());
        String n2 = includeFreshConceptName ? comparingNode.getConceptName() : MyStringUtils.stripFresh(comparingNode.getConceptName());

        result.append(prefix).append(isTail ? "└── " : "├── ")
                .append("[").append(n1).append("] : [").append(n2).append("] - ").append(simRecord).append("\n");
        for (int i = 0; i < node.getChildren().size() - 1; i++) {
            buildExplanationTreeAscii(backtraceTable, node.getChildren().get(i), result, prefix + (isTail ? "    " : "│   "), false, level + 1, includeFreshConceptName);
        }
        if (node.getChildren().size() > 0) {
            buildExplanationTreeAscii(backtraceTable, node.getChildren().get(node.getChildren().size() - 1), result, prefix + (isTail ? "    " : "│   "), true, level + 1, includeFreshConceptName);
        }
    }

    public JSONObject explanationTreeAsJson(ReasoningDirectionConstant direction, boolean includeFreshConceptName) {
        BacktraceTable backtraceTable = getBacktraceTable(direction);
        HashMap<SymmetricPair<TreeNode<Set<String>>>, SimRecord> levelMap = backtraceTable.getTable().get(0);
        if (levelMap == null || levelMap.isEmpty()) return new JSONObject().put("error", "No data available at level 0.");
        TreeNode<Set<String>> root = levelMap.keySet().iterator().next().getFirst();
        return buildExplanationTreeAsJson(backtraceTable, root, 0, includeFreshConceptName);
    }
    public JSONObject explanationTreeAsJson(ReasoningDirectionConstant direction) { return explanationTreeAsJson(direction, true); }

    private JSONObject buildExplanationTreeAsJson(BacktraceTable backtraceTable, TreeNode<Set<String>> node, int level, boolean includeFreshConceptName) {
        if (!backtraceTable.getTable().containsKey(level)) return null;
        SymmetricPair<TreeNode<Set<String>>> pair = backtraceTable.getTable().get(level).keySet().stream()
                .filter(p -> p.getFirst().equals(node) || p.getSecond().equals(node))
                .findFirst().orElse(null);
        if (pair == null) return null;

        TreeNode<Set<String>> comparingNode = pair.getFirst().equals(node) ? pair.getSecond() : pair.getFirst();
        SimRecord simRecord = backtraceTable.getTable().get(level).get(pair);

        String n1 = includeFreshConceptName ? node.getConceptName() : MyStringUtils.stripFresh(node.getConceptName());
        String n2 = includeFreshConceptName ? comparingNode.getConceptName() : MyStringUtils.stripFresh(comparingNode.getConceptName());

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("comparingConcept1", n1);
        jsonObject.put("comparingConcept2", n2);
        jsonObject.put("deg", simRecord.getDeg());
        jsonObject.put("pri", stripPairArray(simRecord.getPri(), includeFreshConceptName));
        jsonObject.put("exi", stripPairArray(simRecord.getExi(), includeFreshConceptName));
        jsonObject.put("emb", new JSONObject(simRecord.getEmb()));

        List<JSONObject> childrenJson = new ArrayList<>();
        for (TreeNode<Set<String>> child : node.getChildren()) {
            JSONObject childJson = buildExplanationTreeAsJson(backtraceTable, child, level + 1, includeFreshConceptName);
            if (childJson != null) childrenJson.add(childJson);
        }
        jsonObject.put("children", childrenJson);
        return jsonObject;
    }

    private JSONArray stripPairArray(java.util.Set<SymmetricPair<String>> pairs, boolean includeFreshConceptName) {
        List<String> out = new ArrayList<>();
        for (SymmetricPair<String> p : pairs) {
            if (includeFreshConceptName) {
                out.add(p.toString());
            } else {
                out.add("(" + MyStringUtils.stripFresh(p.getFirst()) + ", " + MyStringUtils.stripFresh(p.getSecond()) + ")");
            }
        }
        return new JSONArray(out);
    }

    /**
     * Generates a natural explanation of the explanation tree for the given reasoning direction in JSON format.
     *
     * @param direction the reasoning direction (FORWARD or BACKWARD)
     * @return the JSON representation of the natural explanation
     */
    public JSONObject explanationTreeNaturalExplanation(ReasoningDirectionConstant direction) {
        BacktraceTable backtraceTable = getBacktraceTable(direction);

        HashMap<SymmetricPair<TreeNode<Set<String>>>, SimRecord> levelMap = backtraceTable.getTable().get(0);
        if (levelMap == null || levelMap.isEmpty()) {
            return new JSONObject().put("error", "No data available at level 0.");
        }

        TreeNode<Set<String>> root = levelMap.keySet().iterator().next().getFirst();
        return explanationConverterService.convertExplanationWholeTree(buildExplanationTreeAsJson(backtraceTable, root, 0));
    }

    /**
     * Generates a JSON representation of the explanation tree for the given reasoning direction.
     *
     * @param direction the reasoning direction (FORWARD or BACKWARD)
     * @return the JSON representation of the explanation tree
     */


    private JSONObject buildExplanationTreeAsJson(BacktraceTable backtraceTable, TreeNode<Set<String>> node, int level) {
        if (!backtraceTable.getTable().containsKey(level)) {
            return null;
        }

        SymmetricPair<TreeNode<Set<String>>> pair = backtraceTable.getTable().get(level).keySet().stream()
                .filter(p -> p.getFirst().equals(node) || p.getSecond().equals(node))
                .findFirst().orElse(null);

        if (pair == null) {
            return null;
        }

        TreeNode<Set<String>> comparingNode = pair.getFirst().equals(node) ? pair.getSecond() : pair.getFirst();
        SimRecord simRecord = backtraceTable.getTable().get(level).get(pair);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("comparingConcept1", node.getConceptName());
        jsonObject.put("comparingConcept2", comparingNode.getConceptName());
        jsonObject.put("deg", simRecord.getDeg());
        jsonObject.put("pri", new JSONArray(simRecord.getPri().stream().map(SymmetricPair::toString).collect(Collectors.toList())));
        jsonObject.put("exi", new JSONArray(simRecord.getExi().stream().map(SymmetricPair::toString).collect(Collectors.toList())));
        jsonObject.put("emb", new JSONObject(simRecord.getEmb()));

        List<JSONObject> childrenJson = new ArrayList<>();
        for (TreeNode<Set<String>> child : node.getChildren()) {
            JSONObject childJson = buildExplanationTreeAsJson(backtraceTable, child, level + 1);
            if (childJson != null) {
                childrenJson.add(childJson);
            }
        }

        jsonObject.put("children", childrenJson);
        return jsonObject;
    }

    /**
     * Gets the similarity score.
     *
     * @return the similarity score
     */
    public BigDecimal getSimilarity() {
        return similarity;
    }

    /**
     * Gets the backward backtrace table.
     *
     * @return the backward backtrace table
     */
    public BacktraceTable getBackwardBacktraceTable() {
        return backwardBacktraceTable;
    }

    /**
     * Gets the forward backtrace table.
     *
     * @return the forward backtrace table
     */
    public BacktraceTable getForwardBacktraceTable() {
        return forwardBacktraceTable;
    }

    private TreeNode<Set<String>> findRootNode(String concept) {
        TreeNode<Set<String>> root = null;

        for (HashMap<SymmetricPair<TreeNode<Set<String>>>, SimRecord> levelMap : forwardBacktraceTable.getTable().values()) {
            for (SymmetricPair<TreeNode<Set<String>>> pair : levelMap.keySet()) {
                if (pair.getFirst().getConceptName().equals(concept) || pair.getSecond().getConceptName().equals(concept)) {
                    root = pair.getFirst().getConceptName().equals(concept) ? pair.getFirst() : pair.getSecond();
                    break;
                }
            }
            if (root != null) {
                break;
            }
        }

        if (root == null) {
            for (HashMap<SymmetricPair<TreeNode<Set<String>>>, SimRecord> levelMap : backwardBacktraceTable.getTable().values()) {
                for (SymmetricPair<TreeNode<Set<String>>> pair : levelMap.keySet()) {
                    if (pair.getFirst().getConceptName().equals(concept) || pair.getSecond().getConceptName().equals(concept)) {
                        root = pair.getFirst().getConceptName().equals(concept) ? pair.getFirst() : pair.getSecond();
                        break;
                    }
                }
                if (root != null) {
                    break;
                }
            }
        }

        return root;
    }

    private BacktraceTable getBacktraceTable(ReasoningDirectionConstant direction) {
        if (direction.equals(ReasoningDirectionConstant.FORWARD)) {
            return forwardBacktraceTable;
        } else {
            return backwardBacktraceTable;
        }
    }
}
