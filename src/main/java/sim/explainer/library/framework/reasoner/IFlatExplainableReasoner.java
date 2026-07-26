package sim.explainer.library.framework.reasoner;

/**
 * Optional capability for reasoners that produce a flat, direction-keyed explanation
 * table (as opposed to ALEH's level/TreeNode-keyed BacktraceTable). Implement this
 * when a reasoner's explanation doesn't decompose via tree recursion — e.g. FL0 today,
 * and any future non-recursive DL like ALC's structural variant.
 */
public interface IFlatExplainableReasoner<T> {
    T getExplanationTable();
}