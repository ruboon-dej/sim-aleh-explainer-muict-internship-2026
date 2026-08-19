package sim.explainer.library.framework.reasoner;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Set;

import sim.explainer.library.exception.ErrorCode;
import sim.explainer.library.exception.JSimPiException;
import sim.explainer.library.framework.descriptiontree.Tree;
import sim.explainer.library.framework.descriptiontree.TreeNode;
import sim.explainer.library.framework.explainer.ALCExplanationResult;
import sim.explainer.library.framework.explainer.ALCExplanationTable;
import sim.explainer.library.framework.explainer.BacktraceTable;
import sim.explainer.library.framework.explainer.SimRecord;
import sim.explainer.library.framework.unfolding.IRoleUnfolder;

public class TopDownALCSimReasonerImpl implements IReasoner, IFlatExplainableReasoner<ALCExplanationTable> {

    protected static final int SCALE = 5;

    private final OverlapReasoner overlapReasoner;
    protected BacktraceTable backtraceTable;
    protected ALCExplanationTable alcExplanationTable = new ALCExplanationTable();

    public TopDownALCSimReasonerImpl(OverlapReasoner overlapReasoner) {
        this.overlapReasoner = overlapReasoner;
        this.backtraceTable = new BacktraceTable();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public BigDecimal measureDirectedSimilarity(Tree<Set<String>> tree1, Tree<Set<String>> tree2) {
        if (tree1 == null || tree2 == null)
            throw new JSimPiException("Trees cannot be null.", ErrorCode.Application_IllegalArguments);

        this.backtraceTable = new BacktraceTable();

        TreeNode<Set<String>> root1 = tree1.getNodes().get(0);
        TreeNode<Set<String>> root2 = tree2.getNodes().get(0);

        boolean isForward = alcExplanationTable.getForward() == null;

        ALCExplanationResult explanationResult = overlapReasoner.computeDissimilarityWithExplanation(root1, root2);
        BigDecimal similarity = BigDecimal.ONE.subtract(new BigDecimal(explanationResult.getDissimilarity()))
                .round(new MathContext(SCALE));

        SimRecord record = new SimRecord();
        record.setDeg(similarity);
        backtraceTable.addRecord(0, root1, root2, record);

        if (isForward) {
            alcExplanationTable.setForward(explanationResult, root1.getConceptName(), root2.getConceptName());
        } else {
            alcExplanationTable.setBackward(explanationResult, root1.getConceptName(), root2.getConceptName());
        }

        return similarity;
    }

    @Override
    public ALCExplanationTable getExplanationTable() {
        return alcExplanationTable;
    }

    public void resetAlcExplanationTable() {
        this.alcExplanationTable = new ALCExplanationTable();
    }

    @Override
    public BacktraceTable getBacktraceTable() {
        return backtraceTable;
    }

    @Override
    public void setRoleUnfoldingStrategy(IRoleUnfolder iRoleUnfolder) {
        // ALC's dissimilarity measure doesn't use role-hierarchy unfolding.
    }

    @Override
    public List<String> getExecutionTimes() {
        return null;
    }
}