package sim.explainer.library.framework.explainer;

/**
 * Pairs the numeric result of {@code d}/{@code f} (Def. 4.1/4.2) with the
 * {@link ALCExplanationRecord} trace explaining how it was reached.
 */
public class ALCExplanationResult {

    private final double dissimilarity;
    private final double overlap;
    private final ALCExplanationRecord trace;

    public ALCExplanationResult(double dissimilarity, double overlap, ALCExplanationRecord trace) {
        this.dissimilarity = dissimilarity;
        this.overlap = overlap;
        this.trace = trace;
    }

    public double getSimilarity() { return 1.0 - dissimilarity; }

    public double getDissimilarity() {
        return dissimilarity;
    }

    public double getOverlap() {
        return overlap;
    }

    public ALCExplanationRecord getTrace() {
        return trace;
    }
}
