package sim.explainer.library.enumeration;

/**
 * Which pair of things the dissimilarity measure d (Def. 4.2 / Sect. 4.3 of the ALC
 * dissimilarity paper) is being applied to. Individual-based targets are lifted to the
 * concept level first via {@link sim.explainer.library.framework.reasoner.MSCApproximator}.
 */
public enum ALCReasoningTarget {
    CONCEPT_CONCEPT("Concept to Concept"),
    INDIVIDUAL_CONCEPT("Individual to Concept"),
    INDIVIDUAL_INDIVIDUAL("Individual to Individual");

    private final String description;

    ALCReasoningTarget(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
