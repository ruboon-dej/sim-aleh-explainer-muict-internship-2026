package sim.explainer.library.enumeration;

public enum CombinationStrategy {
    AVERAGE("average"),
    MULTIPLICATION("multiplication"),
    RMS("root mean square");

    private final String description;

    CombinationStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}