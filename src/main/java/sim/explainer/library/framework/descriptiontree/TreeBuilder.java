package sim.explainer.library.framework.descriptiontree;

import org.springframework.stereotype.Component;
import sim.explainer.library.exception.ErrorCode;
import sim.explainer.library.exception.JSimPiException;
import sim.explainer.library.util.MyStringUtils;
import sim.explainer.library.util.syntaxanalyzer.ALCDisjunctionSplitterHandler;
import sim.explainer.library.util.syntaxanalyzer.ChainOfResponsibilityHandler;
import sim.explainer.library.util.syntaxanalyzer.HandlerContextImpl;
import sim.explainer.library.util.syntaxanalyzer.krss.KRSSConceptSetHandler;
import sim.explainer.library.util.syntaxanalyzer.krss.KRSSTopLevelParserHandler;
import sim.explainer.library.util.syntaxanalyzer.manchester.ManchesterConceptSetHandler;
import sim.explainer.library.util.syntaxanalyzer.manchester.ManchesterTopLevelParserHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TreeBuilder {

    private ChainOfResponsibilityHandler<HandlerContextImpl> krssHandlerChain;
    private ChainOfResponsibilityHandler<HandlerContextImpl> manchesterHandlerChain;
    private final ALCDisjunctionSplitterHandler disjunctionSplitterHandler = new ALCDisjunctionSplitterHandler();

    public TreeBuilder() {
        manchesterHandlerChain = new ManchesterTopLevelParserHandler()
                .setNextHandler(new ManchesterConceptSetHandler()
                );

        krssHandlerChain = new KRSSTopLevelParserHandler()
                .setNextHandler(new KRSSConceptSetHandler()
                );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * If conceptDescription is disjunctive at its top level ("A or B or ..."),
     * builds a synthetic OR-node (no primitives of its own) at edge/parentNode,
     * with one DISJUNCT child per disjunct (each built by the normal, possibly
     * recursive, single-conjunct construction), and returns true.
     * If it is NOT disjunctive, does nothing and returns false, so the caller
     * falls through to the original single-node construction path unchanged -
     * this keeps EL/FL0/ELH/ALEH behavior byte-for-byte identical, since those
     * concept languages never contain "or" and the splitter always returns a
     * singleton list for them.
     */
    private boolean tryBuildDisjunctiveNode(HandlerContextImpl context, Tree<Set<String>> tree, String edge,
                                             TreeNode<Set<String>> parentNode, String conceptDescription,
                                             HashMap<String, String> mapper, String edgeType, boolean isManchester) {

        List<String> disjuncts = isManchester
                ? disjunctionSplitterHandler.splitManchesterDisjunction(conceptDescription)
                : disjunctionSplitterHandler.splitKRSSDisjunction(conceptDescription);

        if (disjuncts.size() <= 1) {
            return false;
        }

        String orEdge = edge == null ? "root" : edge;
        TreeNode<Set<String>> orNode = tree.addNode(
                MyStringUtils.mapConcepts(conceptDescription, mapper),
                edge,
                parentNode,
                new HashSet<String>(),
                edgeType);

        for (String disjunct : disjuncts) {
            if (isManchester) {
                constructSubTreeWithManchesterSyntax(context, tree, orEdge, orNode, disjunct, mapper, "DISJUNCT");
            } else {
                constructSubTreeWithKrssSyntax(context, tree, orEdge, orNode, disjunct, mapper, "DISJUNCT");
            }
        }

        return true;
    }

    private void constructSubTreeWithKrssSyntax(HandlerContextImpl context, Tree<Set<String>> tree, String edge, TreeNode<Set<String>> parentNode, String nestedPrimitiveStr, HashMap<String, String> mapper, String edgeType) {

        if (tryBuildDisjunctiveNode(context, tree, edge, parentNode, nestedPrimitiveStr, mapper, edgeType, false)) {
            return;
        }

        context.clear();
        context.setConceptDescription(nestedPrimitiveStr);
        krssHandlerChain.invoke(context);

        Set<String> primitivesTop = new HashSet<String>(context.getPrimitiveConceptSet());
        Map<String, Set<String>> existentialEdges = new HashMap<String, Set<String>>(context.getEdgePrimitiveConceptExistentialMap());
        Map<String, Set<String>> universalEdges = new HashMap<String, Set<String>>(context.getEdgePrimitiveConceptUniversalMap());

        TreeNode<Set<String>> child = tree.addNode(
                MyStringUtils.mapConcepts(nestedPrimitiveStr, mapper),
                edge,
                parentNode,
                primitivesTop,
                edgeType);

        for (Map.Entry<String, Set<String>> entry : existentialEdges.entrySet()) {

            String nestedEdge = entry.getKey();
            for (String nestedConcept : entry.getValue()) {
                constructSubTreeWithKrssSyntax(context, tree, nestedEdge, child, nestedConcept, mapper, "EXISTENTIAL");
            }
        }
        
        for (Map.Entry<String, Set<String>> entry : universalEdges.entrySet()) {
            String nestedEdge = entry.getKey();
            for (String nestedConcept : entry.getValue()) {
                constructSubTreeWithKrssSyntax(context, tree, nestedEdge, child, nestedConcept, mapper, "UNIVERSAL");
            }
        }
    }

    private void constructSubTreeWithManchesterSyntax(HandlerContextImpl context, Tree<Set<String>> tree, String edge, TreeNode<Set<String>> parentNode, String nestedPrimitiveStr, HashMap<String, String> mapper, String edgeType) {

        if (tryBuildDisjunctiveNode(context, tree, edge, parentNode, nestedPrimitiveStr, mapper, edgeType, true)) {
            return;
        }

        context.clear();
        context.setConceptDescription(nestedPrimitiveStr);
        manchesterHandlerChain.invoke(context);

        Set<String> primitivesTop = new HashSet<String>(context.getPrimitiveConceptSet());
        Map<String, Set<String>> existentialEdges = new HashMap<String, Set<String>>(context.getEdgePrimitiveConceptExistentialMap());
        Map<String, Set<String>> universalEdges = new HashMap<String, Set<String>>(context.getEdgePrimitiveConceptUniversalMap());

        TreeNode<Set<String>> child = tree.addNode(
                MyStringUtils.mapConcepts(nestedPrimitiveStr, mapper),
                edge,
                parentNode,
                primitivesTop,
                edgeType);

        for (Map.Entry<String, Set<String>> entry : existentialEdges.entrySet()) {

            String nestedEdge = entry.getKey();
            for (String nestedConcept : entry.getValue()) {
                constructSubTreeWithManchesterSyntax(context, tree, nestedEdge, child, nestedConcept, mapper, "EXISTENTIAL");
            }
        }

        for (Map.Entry<String, Set<String>> entry : universalEdges.entrySet()) {
            String nestedEdge = entry.getKey();
            for (String nestedConcept : entry.getValue()) {
                constructSubTreeWithManchesterSyntax(context, tree, nestedEdge, child, nestedConcept, mapper, "UNIVERSAL");
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Tree<Set<String>> constructAccordingToKRSSSyntax(HashMap<String, String> mapper, String conceptName, String conceptDescription) {
        if (conceptName == null || conceptDescription == null) {
            throw new JSimPiException("Unable to construct according to krss syntax as conceptName[" + conceptName + "] and conceptDescription["
                    + conceptDescription + "] are null.", ErrorCode.TreeBuilder_IllegalArguments);
        }

        Tree<Set<String>> tree = new Tree<Set<String>>(MyStringUtils.generateTreeLabel(conceptName));
        HandlerContextImpl context = new HandlerContextImpl();

        List<String> rootDisjuncts = disjunctionSplitterHandler.splitKRSSDisjunction(conceptDescription);
        if (rootDisjuncts.size() > 1) {
            TreeNode<Set<String>> orRoot = tree.addNode(MyStringUtils.mapConcepts(conceptName, mapper), null, null, new HashSet<String>());
            for (String disjunct : rootDisjuncts) {
                constructSubTreeWithKrssSyntax(context, tree, "root", orRoot, disjunct, mapper, "DISJUNCT");
            }
            return tree;
        }

        // Invoke business logic
        context.setConceptDescription(conceptDescription);
        krssHandlerChain.invoke(context);

        Set<String> primitivesTop = new HashSet<String>(context.getPrimitiveConceptSet());
        Map<String, Set<String>> existentialEdges = new HashMap<String, Set<String>>(context.getEdgePrimitiveConceptExistentialMap());
        Map<String, Set<String>> universalEdges = new HashMap<String, Set<String>>(context.getEdgePrimitiveConceptUniversalMap());

        // Initiate the root
        TreeNode<Set<String>> parent = tree.addNode(MyStringUtils.mapConcepts(conceptName, mapper), null, null, primitivesTop);

        for (Map.Entry<String, Set<String>> entry : existentialEdges.entrySet()) {

            String edge = entry.getKey();
            for (String primitiveSet : entry.getValue()) {
                constructSubTreeWithKrssSyntax(context, tree, edge, parent, primitiveSet, mapper, "EXISTENTIAL");
            }
        }

        for (Map.Entry<String, Set<String>> entry : universalEdges.entrySet()) {
            String edge = entry.getKey();
            for (String primitiveSet : entry.getValue()) {
                constructSubTreeWithKrssSyntax(context, tree, edge, parent, primitiveSet, mapper, "UNIVERSAL");
            }
        }

        return tree;
    }

    public Tree<Set<String>> constructAccordingToManchesterSyntax(HashMap<String, String> mapper, String conceptName, String conceptDescription) {
        if (conceptName == null || conceptDescription == null) {
            throw new JSimPiException("Unable to construct according to manchester syntax as conceptName[" + conceptName + "] and conceptDescription["
                    + conceptDescription + "] are null.", ErrorCode.TreeBuilder_IllegalArguments);
        }

        Tree<Set<String>> tree = new Tree<Set<String>>(MyStringUtils.generateTreeLabel(conceptName));
        HandlerContextImpl context = new HandlerContextImpl();

        List<String> rootDisjuncts = disjunctionSplitterHandler.splitManchesterDisjunction(conceptDescription);
        if (rootDisjuncts.size() > 1) {
            TreeNode<Set<String>> orRoot = tree.addNode(MyStringUtils.mapConcepts(conceptName, mapper), null, null, new HashSet<String>());
            for (String disjunct : rootDisjuncts) {
                constructSubTreeWithManchesterSyntax(context, tree, "root", orRoot, disjunct, mapper, "DISJUNCT");
            }
            return tree;
        }

        // Invoke business logic
        context.setConceptDescription(conceptDescription);
        manchesterHandlerChain.invoke(context);

        Set<String> primitivesTop = new HashSet<String>(context.getPrimitiveConceptSet());
        Map<String, Set<String>> existentialEdges = new HashMap<String, Set<String>>(context.getEdgePrimitiveConceptExistentialMap());
        Map<String, Set<String>> universalEdges = new HashMap<String, Set<String>>(context.getEdgePrimitiveConceptUniversalMap());

        // Initiate the root
        TreeNode<Set<String>> parent = tree.addNode(MyStringUtils.mapConcepts(conceptName, mapper), null, null, primitivesTop);

        for (Map.Entry<String, Set<String>> entry : existentialEdges.entrySet()) {

            String edge = entry.getKey();
            for (String primitiveSet : entry.getValue()) {
                constructSubTreeWithManchesterSyntax(context, tree, edge, parent, primitiveSet, mapper, "EXISTENTIAL");
            }
        }

        for (Map.Entry<String, Set<String>> entry : universalEdges.entrySet()) {
            String edge = entry.getKey();
            for (String primitiveSet : entry.getValue()) {
                constructSubTreeWithManchesterSyntax(context, tree, edge, parent, primitiveSet, mapper, "UNIVERSAL");
            }
        }

        return tree;
    }
}