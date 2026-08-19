package sim.explainer.library.framework.explainer;

/**
 * Direction-keyed explanation table for ALC's OverlapReasoner-based similarity,
 * mirroring FL0BacktraceTable's role for FL0 (see IFlatExplainableReasoner) - ALC's
 * comparison isn't tree-recursion-shaped in the ALEH/BacktraceTable sense either, so
 * it gets its own flat, direction-keyed table rather than being force-fit into
 * BacktraceTable/SimRecord.
 */
public class ALCExplanationTable {

    private ALCExplanationResult forward;
    private ALCExplanationResult backward;
    private String forwardConcept1, forwardConcept2;
    private String backwardConcept1, backwardConcept2;

    public void setForward(ALCExplanationResult result, String concept1, String concept2) {
        this.forward = result;
        this.forwardConcept1 = concept1;
        this.forwardConcept2 = concept2;
    }

    public void setBackward(ALCExplanationResult result, String concept1, String concept2) {
        this.backward = result;
        this.backwardConcept1 = concept1;
        this.backwardConcept2 = concept2;
    }

    public ALCExplanationResult getForward() { return forward; }
    public ALCExplanationResult getBackward() { return backward; }

    public String printForward() {
        return print(forwardConcept1, forwardConcept2, forward);
    }

    public String printBackward() {
        return print(backwardConcept1, backwardConcept2, backward);
    }

    private String print(String c1, String c2, ALCExplanationResult result) {
        if (result == null) {
            return "SimRecord{deg=n/a}";
        }
        return "[" + c1 + "] : [" + c2 + "]\n" + result.getTrace().toSimRecordFormat();
    }
}