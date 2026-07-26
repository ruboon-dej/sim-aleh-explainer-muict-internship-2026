package sim.explainer.library.framework.explainer;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Backtrace table for FL0-family reasoners. Unlike BacktraceTable (level/TreeNode-pair keyed,
 * built for ALEH's recursive tree comparison), this is keyed by direction only, since FL0's
 * comparison is a single flat pass over the primitive concept universe (no tree recursion).
 */
public class FL0BacktraceTable {

    private final Map<String, FL0Record> forwardRecords = new LinkedHashMap<>();
    private final Map<String, FL0Record> backwardRecords = new LinkedHashMap<>();

    private BigDecimal forwardDeg = BigDecimal.ZERO;
    private BigDecimal backwardDeg = BigDecimal.ZERO;

    private String forwardConcept1, forwardConcept2;
    private String backwardConcept1, backwardConcept2;

    public void addForwardRecord(String primitive, FL0Record record) {
        forwardRecords.put(primitive, record);
    }

    public void addBackwardRecord(String primitive, FL0Record record) {
        backwardRecords.put(primitive, record);
    }

    public void setForwardDeg(BigDecimal deg) { this.forwardDeg = deg; }
    public void setBackwardDeg(BigDecimal deg) { this.backwardDeg = deg; }

    public void setForwardConcepts(String c1, String c2) { forwardConcept1 = c1; forwardConcept2 = c2; }
    public void setBackwardConcepts(String c1, String c2) { backwardConcept1 = c1; backwardConcept2 = c2; }

    public Map<String, FL0Record> getForwardRecords() { return forwardRecords; }
    public Map<String, FL0Record> getBackwardRecords() { return backwardRecords; }

    public String printForward() {
        return print(forwardConcept1, forwardConcept2, forwardDeg, forwardRecords);
    }

    public String printBackward() {
        return print(backwardConcept1, backwardConcept2, backwardDeg, backwardRecords);
    }

    private String print(String c1, String c2, BigDecimal deg, Map<String, FL0Record> records) {
        StringBuilder matched = new StringBuilder();
        StringBuilder missed = new StringBuilder();

        for (FL0Record r : records.values()) {
            StringBuilder target = r.isMatched() ? matched : missed;
            if (target.length() > 0) target.append(", ");
            target.append(r.toString());
        }

        return "FL0Record{deg=" + deg + ", matched=[" + matched + "], missed=[" + missed + "]}";
    }
}