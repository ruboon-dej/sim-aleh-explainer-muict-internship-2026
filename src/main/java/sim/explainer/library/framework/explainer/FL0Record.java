package sim.explainer.library.framework.explainer;

import java.math.BigDecimal;

import sim.explainer.library.util.MyStringUtils;

public class FL0Record {
    private final String primitive;
    private final String matchedAgainst;
    private final BigDecimal weight;
    private final boolean matched;

    public FL0Record(String primitive, String matchedAgainst, BigDecimal weight, boolean matched) {
        this.primitive = primitive;
        this.matchedAgainst = matchedAgainst;
        this.weight = weight;
        this.matched = matched;
    }

    public String toString(boolean includeFreshConceptName) {
        String p = includeFreshConceptName ? primitive : MyStringUtils.stripFresh(primitive);
        String m = includeFreshConceptName ? matchedAgainst : MyStringUtils.stripFresh(matchedAgainst);

        boolean isBinary = weight.compareTo(BigDecimal.ZERO) == 0 || weight.compareTo(BigDecimal.ONE) == 0;
        if (!matched) return p;
        if (m == null || m.equals(p)) {
            return isBinary ? p : p + ":" + weight;
        }
        return isBinary ? p + "~" + m : p + "~" + m + ":" + weight;
    }

    public static String stripFresh(String str) {
        if (str == null) return null;
        return str.endsWith("'") ? str.substring(0, str.length() - 1) : str;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String getPrimitive() { return primitive; }
    public String getMatchedAgainst() { return matchedAgainst; }
    public BigDecimal getWeight() { return weight; }
    public boolean isMatched() { return matched; }
}