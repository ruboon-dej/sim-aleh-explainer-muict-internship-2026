package sim.explainer.library.enumeration;

public enum ImplementationMethod {
    DYNAMIC_SIM("dynamic programming Sim"),
    DYNAMIC_SIMPI("dynamic programming SimPi"),
    TOPDOWN_SIM("top down Sim"),
    TOPDOWN_SIMPI("top down SimPi"),
    DYNAMIC_ALEH_SIMPI("dynamic programming ALEH SimPi"),
    TOPDOWN_ALEH_SIMPI("top down ALEH SimPi"),
    DYNAMIC_ALEH_SIM("dynamic programming ALEH Sim"),
    TOPDOWN_ALEH_SIM("top down ALEH Sim"),
    DYNAMIC_FL0_SIM("dynamic programming FL0 Sim"),
    DYNAMIC_FL0_SIMPI("dynamic programming FL0 SimPi"),
    TOPDOWN_FL0_SIM("top down FL0 Sim"),
    TOPDOWN_FL0_SIMPI("top down FL0 SimPi"),
    TOPDOWN_ALC_SIM("top down ALC Sim");

    private final String description;

    ImplementationMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}