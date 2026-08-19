package sim.explainer.library.framework.reasoner;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import sim.explainer.library.exception.ErrorCode;
import sim.explainer.library.exception.JSimPiException;
import sim.explainer.library.framework.ABoxServiceContext;
import sim.explainer.library.framework.descriptiontree.Tree;
import sim.explainer.library.framework.descriptiontree.TreeNode;

/**
 * Approximates the most specific concept of an individual w.r.t. the A-Box, up to a
 * bounded depth k (Def. 3.2 / MSC^k), now building a Tree<Set<String>> (shared with
 * ELH/ALEH/FL0) instead of the old standalone ALCConceptTree.
 *
 * Asserted concepts on an individual become the node's primitive data; each role
 * successor R(a,b) becomes one EXISTENTIAL child (universal restrictions are not
 * inferable from ground A-Box facts alone).
 */
@Component
public class MSCApproximator {

    private static final Logger logger = LoggerFactory.getLogger(MSCApproximator.class);

    private final ABoxServiceContext aboxServiceContext;

    @Autowired
    public MSCApproximator(ABoxServiceContext aboxServiceContext) {
        this.aboxServiceContext = aboxServiceContext;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private TreeNode<Set<String>> approximate(Tree<Set<String>> tree, TreeNode<Set<String>> parent, String role,
                                                String individual, int depthRemaining, Set<String> pathVisited) {
        Set<String> primitives = new HashSet<>(aboxServiceContext.getAssertedConcepts(individual));
        TreeNode<Set<String>> node = tree.addNode(individual, role, parent, primitives,
                parent == null ? null : "EXISTENTIAL");

        if (depthRemaining > 0 && !pathVisited.contains(individual)) {
            Set<String> nextPathVisited = new HashSet<>(pathVisited);
            nextPathVisited.add(individual);

            for (Map.Entry<String, Set<String>> roleEntry : aboxServiceContext.getAssertedRoles(individual).entrySet()) {
                String nextRole = roleEntry.getKey();
                for (String successor : roleEntry.getValue()) {
                    approximate(tree, node, nextRole, successor, depthRemaining - 1, nextPathVisited);
                }
            }
        }

        return node;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @param individualName the individual to approximate MSC for
     * @param maxDepth       how many role-hops to follow before stopping; cycles in
     *                       the A-Box are also broken automatically regardless of
     *                       maxDepth (an individual is never expanded twice along
     *                       the same path)
     */
    public Tree<Set<String>> approximateMostSpecificConcept(String individualName, int maxDepth) {
        if (individualName == null) {
            throw new JSimPiException("Unable to approximate MSC as individualName is null.", ErrorCode.MSCApproximator_IllegalArguments);
        }
        if (maxDepth < 0) {
            throw new JSimPiException("Unable to approximate MSC as maxDepth[" + maxDepth + "] is negative.", ErrorCode.MSCApproximator_IllegalArguments);
        }
        if (!aboxServiceContext.containsIndividual(individualName)) {
            throw new JSimPiException("Unable to approximate MSC as individual[" + individualName + "] is unknown to the A-Box.", ErrorCode.MSCApproximator_IllegalArguments);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Approximating MSC^" + maxDepth + " for individual[" + individualName + "].");
        }

        Tree<Set<String>> tree = new Tree<>(individualName);
        approximate(tree, null, null, individualName, maxDepth, new HashSet<>());
        return tree;
    }

    /**
     * Depth-auto-detect overload: uses the number of individuals in the A-Box as a
     * safe upper bound on how deep any role chain could need to go, since cycles
     * are broken independently of the depth counter.
     */
    public Tree<Set<String>> approximateMostSpecificConcept(String individualName) {
        return approximateMostSpecificConcept(individualName, aboxServiceContext.getDomainSize());
    }
}