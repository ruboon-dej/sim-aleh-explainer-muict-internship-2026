package sim.explainer.library.framework.explainer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Records how f(C,D) (Def. 4.1) arrived at its value at one level of recursion:
 * which branch fired (structural equivalence / bottom disjointness / the general
 * recursive case), which primitive concepts on each side overlapped and what
 * f_P extension-ratio that produced, and one child record per role recursed into
 * via a universal or existential restriction. Built by
 * {@link sim.explainer.library.framework.reasoner.OverlapReasoner}'s traced
 * methods alongside (not instead of) the ordinary numeric computation.
 */
public class ALCExplanationRecord {

    public enum Branch { EQUIVALENT, DISJOINT, RECURSIVE }

    private int level;
    private double lambda;
    private double result;
    private Branch branch;
    private String directionLabel; // set only on children produced while matching existential restrictions
    private int winningDisjunctIndexC = -1;
    private int winningDisjunctIndexD = -1;

    private Set<String> primitivesLeft = new LinkedHashSet<>();
    private Set<String> primitivesRight = new LinkedHashSet<>();
    private Set<String> matchedPrimitives = new LinkedHashSet<>();
    private double fPValue;
    private int disjunctPairsCompared = 1;
    private Set<String> extensionLeft = new LinkedHashSet<>();
    private Set<String> extensionRight = new LinkedHashSet<>();
    private Set<String> extensionIntersection = new LinkedHashSet<>();

    private final Map<String, ALCExplanationRecord> universalChildren = new LinkedHashMap<>();
    private final Map<String, List<ALCExplanationRecord>> existentialChildren = new LinkedHashMap<>();

    private final java.util.List<String> missed = new java.util.ArrayList<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String toSimRecordFormat() {
        return toSimRecordFormat("");
    }

    private String toSimRecordFormat(String indent) {
        StringBuilder sb = new StringBuilder();

        double dissim;
        if (branch == Branch.DISJOINT) {
            dissim = 1.0;
        } else if (branch == Branch.EQUIVALENT) {
            dissim = 0.0;
        } else {
            dissim = 1.0 / result;
        }
        double sim = 1.0 - dissim;

        sb.append(indent).append("SimRecord{deg=").append(String.format("%.4f", sim))
        .append(", DisSimRecord{deg=").append(String.format("%.4f", dissim))
        .append(", case=").append(branch)
        .append(", branch=").append(branchLabelOrPlaceholder())
        .append(", pairs=").append(disjunctPairsCompared)
        .append(", pri=").append(matchedPrimitives)
        .append(", exi=[");
        appendRoleList(sb, existentialChildren.keySet());
        sb.append("], uni=[");
        appendRoleList(sb, universalChildren.keySet());
        sb.append("], missed=").append(missed);
        sb.append("}}");

        for (Map.Entry<String, ALCExplanationRecord> entry : universalChildren.entrySet()) {
            sb.append("\n").append(indent).append("  ALL ").append(entry.getKey()).append(": ")
            .append(entry.getValue().toSimRecordFormat(indent + "    ").trim());
        }
        for (Map.Entry<String, List<ALCExplanationRecord>> entry : existentialChildren.entrySet()) {
            for (int i = 0; i < entry.getValue().size(); i++) {
                ALCExplanationRecord child = entry.getValue().get(i);
                sb.append("\n").append(indent).append("  EXISTS ").append(entry.getKey())
                .append(" [").append(i).append("]: ")
                .append(child.toSimRecordFormat(indent + "    ").trim());
            }
        }

        return sb.toString();
    }

    private String branchLabelOrPlaceholder() {
        if (winningDisjunctIndexC < 0 || winningDisjunctIndexD < 0) {
            return "single_disjunct";
        }
        return "(C" + winningDisjunctIndexC + ",D" + winningDisjunctIndexD + ")";
    }

    private void appendRoleList(StringBuilder sb, Set<String> roles) {
        int i = 0;
        for (String role : roles) {
            if (i++ > 0) sb.append(",");
            sb.append(role);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters and Setters /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setLevel(int level) { this.level = level; }
    public void setLambda(double lambda) { this.lambda = lambda; }
    public void setResult(double result) { this.result = result; }
    public double getResult() { return result; }
    public void setBranch(Branch branch) { this.branch = branch; }
    public void setDirectionLabel(String directionLabel) { this.directionLabel = directionLabel; }

    public void setPrimitivesLeft(Set<String> primitivesLeft) { this.primitivesLeft = new LinkedHashSet<>(primitivesLeft); }
    public Set<String> getPrimitivesLeft() { return primitivesLeft; }
    public void setPrimitivesRight(Set<String> primitivesRight) { this.primitivesRight = new LinkedHashSet<>(primitivesRight); }
    public Set<String> getPrimitivesRight() { return primitivesRight; }
    public void setMatchedPrimitives(Set<String> matchedPrimitives) { this.matchedPrimitives = new LinkedHashSet<>(matchedPrimitives); }
    public Set<String> getMatchedPrimitives() { return matchedPrimitives; }
    public void setFPValue(double fPValue) { this.fPValue = fPValue; }
    public double getFPValue() { return fPValue; }
    public void setExtensions(Set<String> left, Set<String> right, Set<String> intersection) {
        this.extensionLeft = new LinkedHashSet<>(left);
        this.extensionRight = new LinkedHashSet<>(right);
        this.extensionIntersection = new LinkedHashSet<>(intersection);
    }
    public Set<String> getExtensionLeft() { return extensionLeft; }
    public Set<String> getExtensionRight() { return extensionRight; }
    public Set<String> getExtensionIntersection() { return extensionIntersection; }
    public void setDisjunctPairsCompared(int disjunctPairsCompared) { this.disjunctPairsCompared = disjunctPairsCompared; }
    public int getDisjunctPairsCompared() { return disjunctPairsCompared; }

    public Map<String, ALCExplanationRecord> getUniversalChildren() { return universalChildren; }
    public Map<String, List<ALCExplanationRecord>> getExistentialChildren() { return existentialChildren; }
    public List<String> getMissed() { return missed; }
    public void setWinningDisjunctPair(int indexC, int indexD) {
        this.winningDisjunctIndexC = indexC;
        this.winningDisjunctIndexD = indexD;
    }
    public int getWinningDisjunctIndexC() { return winningDisjunctIndexC; }
    public int getWinningDisjunctIndexD() { return winningDisjunctIndexD; }
}
