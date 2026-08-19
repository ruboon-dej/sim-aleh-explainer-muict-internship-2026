package sim.explainer.library.framework.explainer;

/** Forward tree (C compared against D) + backward tree (D compared against C),
 *  plus their scores averaged only at this top level — never inside either tree. */
public class ALCBidirectionalExplanationResult {

    private final ALCExplanationResult forward;
    private final ALCExplanationResult backward;
    private final double averagedDissimilarity;

    public ALCBidirectionalExplanationResult(ALCExplanationResult forward, ALCExplanationResult backward) {
        this.forward = forward;
        this.backward = backward;
        this.averagedDissimilarity = (forward.getDissimilarity() + backward.getDissimilarity()) / 2.0;
    }

    public ALCExplanationResult getForward() { return forward; }
    public ALCExplanationResult getBackward() { return backward; }
    public double getAveragedDissimilarity() { return averagedDissimilarity; }
    public double getAveragedSimilarity() { return 1.0 - averagedDissimilarity; }
}