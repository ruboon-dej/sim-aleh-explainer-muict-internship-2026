package sim.explainer.library.framework.reasoner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import sim.explainer.library.exception.ErrorCode;
import sim.explainer.library.exception.JSimPiException;
import sim.explainer.library.framework.ABoxServiceContext;
import sim.explainer.library.framework.ALCPreferenceProfile;
import sim.explainer.library.framework.descriptiontree.TreeNode;
import sim.explainer.library.framework.explainer.ALCBidirectionalExplanationResult;
import sim.explainer.library.framework.explainer.ALCExplanationRecord;
import sim.explainer.library.framework.explainer.ALCExplanationResult;

/**
 * Implements the overlap function f (Def. 4.1) and the dissimilarity measure d
 * (Def. 4.2) between two concepts in ALC normal form, ported onto the shared
 * Tree<Set<String>>/TreeNode<Set<String>> structure used by ELH/ALEH/FL0.
 *
 * "Top" and "bottom" are no longer explicit flags on a wrapper object; see
 * isTopNode()/isBottomNode() below - both are inferred structurally from the
 * TreeNode itself, which produces identical results to the original flag-based
 * ALCConceptTree.isTop()/isBottom() (verified: an unflagged structurally-empty
 * node and an explicitly-flagged top node both resolve to the same f() value).
 *
 * Same two approximations as before, still true here:
 * 1. "C is equivalent to D" is approximated as structural equality of normal-form
 *    trees rather than true semantic equivalence checking.
 * 2. f_forall/f_exists sum over the roles actually mentioned by the two conjuncts
 *    being compared rather than the full role vocabulary N_R the paper sums over.
 *
 * A role is assumed to have at most one UNIVERSAL child per node (matching the
 * original Map<String,ALCConceptTree> model). If the parser ever emits more than
 * one UNIVERSAL child for the same role on the same node, only the first is used
 * here - this is the same known issue tracked separately (ALL hasTopping union
 * filler collapsing), to be resolved when the parser layer is merged.
 */
@Component
public class OverlapReasoner {

    private static final Logger logger = LoggerFactory.getLogger(OverlapReasoner.class);

    private final ABoxServiceContext aboxServiceContext;
    private final ALCPreferenceProfile preferenceProfile;

    @Autowired
    public OverlapReasoner(ABoxServiceContext aboxServiceContext, ALCPreferenceProfile preferenceProfile) {
        this.aboxServiceContext = aboxServiceContext;
        this.preferenceProfile = preferenceProfile;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private TreeNode<Set<String>> syntheticTop() {
        return new TreeNode<>("TOP", null, Collections.emptySet(), -1);
    }

    private boolean isBottomNode(TreeNode<Set<String>> node) {
        return node.getDisjunctChildren().isEmpty()
                && (node.getData().contains("BOTTOM") || node.getData().contains("⊥"));
    }

    private List<TreeNode<Set<String>>> effectiveDisjuncts(TreeNode<Set<String>> node) {
        List<TreeNode<Set<String>>> disjuncts = node.getDisjunctChildren();
        if (disjuncts.isEmpty()) {
            List<TreeNode<Set<String>>> singleton = new ArrayList<>();
            singleton.add(node);
            return singleton;
        }
        return disjuncts;
    }

    private Map<String, TreeNode<Set<String>>> universalMap(TreeNode<Set<String>> node) {
        Map<String, TreeNode<Set<String>>> map = new HashMap<>();
        for (TreeNode<Set<String>> child : node.getUniversalChildren()) {
            map.putIfAbsent(child.getEdgeToParent(), child);
        }
        return map;
    }

    private Map<String, List<TreeNode<Set<String>>>> existentialMap(TreeNode<Set<String>> node) {
        Map<String, List<TreeNode<Set<String>>>> map = new HashMap<>();
        for (TreeNode<Set<String>> child : node.getExistentialChildren()) {
            map.computeIfAbsent(child.getEdgeToParent(), k -> new ArrayList<>()).add(child);
        }
        return map;
    }

    private boolean isStructurallyEquivalent(TreeNode<Set<String>> c, TreeNode<Set<String>> d) {
        boolean cBottom = isBottomNode(c);
        boolean dBottom = isBottomNode(d);
        if (cBottom && dBottom) return true;
        if (cBottom || dBottom) return false;

        List<TreeNode<Set<String>>> disjunctsC = effectiveDisjuncts(c);
        List<TreeNode<Set<String>>> disjunctsD = effectiveDisjuncts(d);

        if (disjunctsC.size() != 1 || disjunctsD.size() != 1) {
            // Multi-disjunct structural equivalence (matching up to permutation) is
            // not attempted - falls through to the general recursive case, which
            // still produces a valid (if not maximal) overlap score.
            return false;
        }

        return nodesEquivalent(disjunctsC.get(0), disjunctsD.get(0));
    }

    private boolean nodesEquivalent(TreeNode<Set<String>> a, TreeNode<Set<String>> b) {
        if (!a.getData().equals(b.getData())) {
            return false;
        }

        Map<String, TreeNode<Set<String>>> uA = universalMap(a);
        Map<String, TreeNode<Set<String>>> uB = universalMap(b);
        if (!uA.keySet().equals(uB.keySet())) {
            return false;
        }
        for (String role : uA.keySet()) {
            if (!isStructurallyEquivalent(uA.get(role), uB.get(role))) {
                return false;
            }
        }

        Map<String, List<TreeNode<Set<String>>>> eA = existentialMap(a);
        Map<String, List<TreeNode<Set<String>>>> eB = existentialMap(b);
        if (!eA.keySet().equals(eB.keySet())) {
            return false;
        }
        for (String role : eA.keySet()) {
            List<TreeNode<Set<String>>> listA = eA.get(role);
            List<TreeNode<Set<String>>> listB = new ArrayList<>(eB.get(role));
            if (listA.size() != listB.size()) {
                return false;
            }
            for (TreeNode<Set<String>> itemA : listA) {
                TreeNode<Set<String>> match = null;
                for (TreeNode<Set<String>> itemB : listB) {
                    if (isStructurallyEquivalent(itemA, itemB)) {
                        match = itemB;
                        break;
                    }
                }
                if (match == null) {
                    return false;
                }
                listB.remove(match);
            }
        }

        return true;
    }

    private double fP(Set<String> primC, Set<String> primD) {
        Set<String> extensionC = aboxServiceContext.getConjunctionExtension(primC);
        Set<String> extensionD = aboxServiceContext.getConjunctionExtension(primD);

        if (extensionC.equals(extensionD)) {
            return aboxServiceContext.getDomainSize();
        }

        Set<String> union = new HashSet<>(extensionC);
        union.addAll(extensionD);
        Set<String> intersection = new HashSet<>(extensionC);
        intersection.retainAll(extensionD);
        Set<String> symmetricDifference = new HashSet<>(union);
        symmetricDifference.removeAll(intersection);

        if (symmetricDifference.isEmpty()) {
            return aboxServiceContext.getDomainSize();
        }

        return (double) union.size() / (double) symmetricDifference.size();
    }

    private double fForAll(TreeNode<Set<String>> ci, TreeNode<Set<String>> dj, int nextLevel) {
        Map<String, TreeNode<Set<String>>> uCi = universalMap(ci);
        Map<String, TreeNode<Set<String>>> uDj = universalMap(dj);

        Set<String> roles = new HashSet<>();
        roles.addAll(uCi.keySet());
        roles.addAll(uDj.keySet());

        double sum = 0.0;
        for (String role : roles) {
            TreeNode<Set<String>> valueC = uCi.getOrDefault(role, syntheticTop());
            TreeNode<Set<String>> valueD = uDj.getOrDefault(role, syntheticTop());
            sum += f(valueC, valueD, nextLevel);
        }
        return sum;
    }

    private double bestMatchSum(List<TreeNode<Set<String>>> from, List<TreeNode<Set<String>>> to, int nextLevel) {
        List<TreeNode<Set<String>>> candidates = to.isEmpty() ? Collections.singletonList(syntheticTop()) : to;

        double sum = 0.0;
        for (TreeNode<Set<String>> item : from) {
            double max = 0.0;
            for (TreeNode<Set<String>> candidate : candidates) {
                max = Math.max(max, f(item, candidate, nextLevel));
            }
            sum += max;
        }
        return sum;
    }

    private double fExists(TreeNode<Set<String>> ci, TreeNode<Set<String>> dj, int nextLevel) {
        Map<String, List<TreeNode<Set<String>>>> eCi = existentialMap(ci);
        Map<String, List<TreeNode<Set<String>>>> eDj = existentialMap(dj);

        Set<String> roles = new HashSet<>();
        roles.addAll(eCi.keySet());
        roles.addAll(eDj.keySet());

        double sum = 0.0;
        for (String role : roles) {
            List<TreeNode<Set<String>>> listC = eCi.getOrDefault(role, Collections.emptyList());
            List<TreeNode<Set<String>>> listD = eDj.getOrDefault(role, Collections.emptyList());

            // Def. 4.1: only the larger existential set matches into the smaller one.
            // This can legitimately make f(C,D) != f(D,C) - expected and intentional
            // (see computeDissimilarityWithExplanationBothDirections).
            if (listC.size() >= listD.size()) {
                sum += bestMatchSum(listC, listD, nextLevel);
            } else {
                sum += bestMatchSum(listD, listC, nextLevel);
            }
        }
        return sum;
    }

    private double fU(TreeNode<Set<String>> ci, TreeNode<Set<String>> dj, int nextLevel) {
        return fP(ci.getData(), dj.getData())
                + fForAll(ci, dj, nextLevel)
                + fExists(ci, dj, nextLevel);
    }

    private double f(TreeNode<Set<String>> c, TreeNode<Set<String>> d, int level) {
        if (isStructurallyEquivalent(c, d)) {
            return aboxServiceContext.getDomainSize();
        }

        if (isBottomNode(c) || isBottomNode(d)) {
            return 0.0;
        }

        List<TreeNode<Set<String>>> disjunctsC = effectiveDisjuncts(c);
        List<TreeNode<Set<String>>> disjunctsD = effectiveDisjuncts(d);

        double max = 0.0;
        for (TreeNode<Set<String>> ci : disjunctsC) {
            for (TreeNode<Set<String>> dj : disjunctsD) {
                max = Math.max(max, fU(ci, dj, level + 1));
            }
        }

        double lambda = preferenceProfile.getLevelDiscountFactor(level);
        return 1 + lambda * max;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Explanation-tracing (mirrors f/fU/fP/fForAll/fExists exactly, additionally filling an ///////////////////////////
    // ALCExplanationRecord as it goes) ///////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private double fTraced(TreeNode<Set<String>> c, TreeNode<Set<String>> d, int level, ALCExplanationRecord record) {
        if (isStructurallyEquivalent(c, d)) {
            record.setBranch(ALCExplanationRecord.Branch.EQUIVALENT);
            record.setResult(aboxServiceContext.getDomainSize());
            return record.getResult();
        }

        if (isBottomNode(c) || isBottomNode(d)) {
            record.setBranch(ALCExplanationRecord.Branch.DISJOINT);
            record.setResult(0.0);
            return 0.0;
        }

        List<TreeNode<Set<String>>> disjunctsC = effectiveDisjuncts(c);
        List<TreeNode<Set<String>>> disjunctsD = effectiveDisjuncts(d);

        double max = 0.0;
        ALCExplanationRecord bestChild = null;
        int bestI = -1, bestJ = -1;
        int i = 0;
        for (TreeNode<Set<String>> ci : disjunctsC) {
            int j = 0;
            for (TreeNode<Set<String>> dj : disjunctsD) {
                ALCExplanationRecord candidate = new ALCExplanationRecord();
                double val = fUTraced(ci, dj, level + 1, candidate);
                if (bestChild == null || val >= max) {
                    max = val;
                    bestChild = candidate;
                    bestI = i;
                    bestJ = j;
                }
                j++;
            }
            i++;
        }

        double lambda = preferenceProfile.getLevelDiscountFactor(level);
        double result = 1 + lambda * max;

        record.setBranch(ALCExplanationRecord.Branch.RECURSIVE);
        record.setLevel(level);
        record.setLambda(lambda);
        record.setResult(result);
        record.setDisjunctPairsCompared(disjunctsC.size() * disjunctsD.size());
        if (disjunctsC.size() > 1 || disjunctsD.size() > 1) {
            record.setWinningDisjunctPair(bestI, bestJ);
        }
        if (bestChild != null) {
            record.setPrimitivesLeft(bestChild.getPrimitivesLeft());
            record.setPrimitivesRight(bestChild.getPrimitivesRight());
            record.setMatchedPrimitives(bestChild.getMatchedPrimitives());
            record.setFPValue(bestChild.getFPValue());
            record.setExtensions(bestChild.getExtensionLeft(), bestChild.getExtensionRight(), bestChild.getExtensionIntersection());
            record.getUniversalChildren().putAll(bestChild.getUniversalChildren());
            record.getExistentialChildren().putAll(bestChild.getExistentialChildren());
        }

        return result;
    }

    private double fUTraced(TreeNode<Set<String>> ci, TreeNode<Set<String>> dj, int nextLevel, ALCExplanationRecord record) {
        record.setPrimitivesLeft(ci.getData());
        record.setPrimitivesRight(dj.getData());

        Set<String> matched = new HashSet<>(ci.getData());
        matched.retainAll(dj.getData());
        record.setMatchedPrimitives(matched);

        double fp = fP(ci.getData(), dj.getData());
        record.setFPValue(fp);

        Set<String> extLeft = aboxServiceContext.getConjunctionExtension(ci.getData());
        Set<String> extRight = aboxServiceContext.getConjunctionExtension(dj.getData());
        Set<String> extIntersection = new HashSet<>(extLeft);
        extIntersection.retainAll(extRight);
        record.setExtensions(extLeft, extRight, extIntersection);

        double forAllSum = fForAllTraced(ci, dj, nextLevel, record);
        double existsSum = fExistsTraced(ci, dj, nextLevel, record);

        return fp + forAllSum + existsSum;
    }

    private double fForAllTraced(TreeNode<Set<String>> ci, TreeNode<Set<String>> dj, int nextLevel, ALCExplanationRecord record) {
        Map<String, TreeNode<Set<String>>> uCi = universalMap(ci);
        Map<String, TreeNode<Set<String>>> uDj = universalMap(dj);

        Set<String> roles = new HashSet<>();
        roles.addAll(uCi.keySet());
        roles.addAll(uDj.keySet());

        double sum = 0.0;
        for (String role : roles) {
            TreeNode<Set<String>> valueC = uCi.getOrDefault(role, syntheticTop());
            TreeNode<Set<String>> valueD = uDj.getOrDefault(role, syntheticTop());
            ALCExplanationRecord child = new ALCExplanationRecord();
            sum += fTraced(valueC, valueD, nextLevel, child);
            record.getUniversalChildren().put(role, child);
        }
        return sum;
    }

    private double fExistsTraced(TreeNode<Set<String>> ci, TreeNode<Set<String>> dj, int nextLevel, ALCExplanationRecord record) {
        Map<String, List<TreeNode<Set<String>>>> eCi = existentialMap(ci);
        Map<String, List<TreeNode<Set<String>>>> eDj = existentialMap(dj);

        Set<String> roles = new HashSet<>();
        roles.addAll(eCi.keySet());
        roles.addAll(eDj.keySet());

        double sum = 0.0;
        for (String role : roles) {
            List<TreeNode<Set<String>>> listC = eCi.getOrDefault(role, Collections.emptyList());
            List<TreeNode<Set<String>>> listD = eDj.getOrDefault(role, Collections.emptyList());

            List<ALCExplanationRecord> children = new ArrayList<>();
            double value;
            if (listC.size() >= listD.size()) {
                value = bestMatchSumTraced(listC, listD, nextLevel, children, "this->other", record.getMissed());
            } else {
                value = bestMatchSumTraced(listD, listC, nextLevel, children, "other->this", record.getMissed());
            }
            sum += value;
            record.getExistentialChildren().put(role, children);
        }
        return sum;
    }

    private double bestMatchSumTraced(List<TreeNode<Set<String>>> from, List<TreeNode<Set<String>>> to, int nextLevel,
                                       List<ALCExplanationRecord> outChildren, String directionLabel,
                                       List<String> missedOut) {
        boolean nothingOnOtherSide = to.isEmpty();
        List<TreeNode<Set<String>>> candidates = nothingOnOtherSide ? Collections.singletonList(syntheticTop()) : to;

        double sum = 0.0;
        for (TreeNode<Set<String>> item : from) {
            double max = 0.0;
            ALCExplanationRecord best = null;
            for (TreeNode<Set<String>> candidate : candidates) {
                ALCExplanationRecord candidateRecord = new ALCExplanationRecord();
                double val = fTraced(item, candidate, nextLevel, candidateRecord);
                if (best == null || val >= max) {
                    max = val;
                    best = candidateRecord;
                }
            }
            if (best != null) {
                best.setDirectionLabel(directionLabel);
                if (nothingOnOtherSide) {
                    missedOut.add(item.toString());
                } else {
                    outChildren.add(best);
                }
            }
            sum += max;
        }
        return sum;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** The overlap function f from Def. 4.1. */
    public double computeOverlap(TreeNode<Set<String>> c, TreeNode<Set<String>> d) {
        if (c == null || d == null) {
            throw new JSimPiException("Unable to compute overlap as c or d is null.", ErrorCode.OverlapReasoner_IllegalArguments);
        }

        double result = f(c, d, 0);

        if (logger.isDebugEnabled()) {
            logger.debug("f(C,D) = " + result);
        }

        return result;
    }

    /** The dissimilarity measure d from Def. 4.2, derived from f. */
    public double computeDissimilarity(TreeNode<Set<String>> c, TreeNode<Set<String>> d) {
        double overlap = computeOverlap(c, d);
        int domainSize = aboxServiceContext.getDomainSize();

        if (overlap == domainSize) {
            return 0.0;
        }
        if (overlap == 0.0) {
            return 1.0;
        }
        return 1.0 / overlap;
    }

    /** Same as {@link #computeDissimilarity}, but also returns the explanation trace. */
    public ALCExplanationResult computeDissimilarityWithExplanation(TreeNode<Set<String>> c, TreeNode<Set<String>> d) {
        if (c == null || d == null) {
            throw new JSimPiException("Unable to compute overlap as c or d is null.", ErrorCode.OverlapReasoner_IllegalArguments);
        }

        ALCExplanationRecord record = new ALCExplanationRecord();
        double overlap = fTraced(c, d, 0, record);
        int domainSize = aboxServiceContext.getDomainSize();

        double dissimilarity;
        if (overlap == 0.0) {
            dissimilarity = 1.0;
        } else if (overlap == domainSize) {
            dissimilarity = 0.0;
        } else {
            dissimilarity = 1.0 / overlap;
        }

        return new ALCExplanationResult(dissimilarity, overlap, record);
    }

    /** Builds the forward tree (c vs d) and backward tree (d vs c) independently,
     *  then averages the two final scores - never blends inside either tree. */
    public ALCBidirectionalExplanationResult computeDissimilarityWithExplanationBothDirections(TreeNode<Set<String>> c, TreeNode<Set<String>> d) {
        ALCExplanationResult forward = computeDissimilarityWithExplanation(c, d);
        ALCExplanationResult backward = computeDissimilarityWithExplanation(d, c);
        return new ALCBidirectionalExplanationResult(forward, backward);
    }
}