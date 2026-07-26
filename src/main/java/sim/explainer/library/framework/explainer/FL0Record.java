package sim.explainer.library.framework.explainer;

import java.math.BigDecimal;

/**
 * Records how a single primitive concept P was resolved during an FL0 comparison.
 * One instance per primitive in the CNpri universe, per direction (forward/backward).
 */
public class FL0Record {
    private final String primitive;       // P, from D's side (the concept being checked)
    private final String matchedAgainst;  // Q in C that satisfied inclusion; null if none found
    private final BigDecimal weight;      // contribution: 1.0 (Sim, exact) / sHat(P,Q) (SimPi) / 0.0 (miss)
    private final boolean matched;        // true if inclusion held for some Q

    public FL0Record(String primitive, String matchedAgainst, BigDecimal weight, boolean matched) {
        this.primitive = primitive;
        this.matchedAgainst = matchedAgainst;
        this.weight = weight;
        this.matched = matched;
    }

    public String getPrimitive() { return primitive; }
    public String getMatchedAgainst() { return matchedAgainst; }
    public BigDecimal getWeight() { return weight; }
    public boolean isMatched() { return matched; }

    @Override
    public String toString() {
        boolean isBinary = weight.compareTo(BigDecimal.ZERO) == 0 || weight.compareTo(BigDecimal.ONE) == 0;
        if (!matched) return primitive;
        if (matchedAgainst == null || matchedAgainst.equals(primitive)) {
            return isBinary ? primitive : primitive + ":" + weight;
        }
        return isBinary ? primitive + "~" + matchedAgainst : primitive + "~" + matchedAgainst + ":" + weight;
    }
}