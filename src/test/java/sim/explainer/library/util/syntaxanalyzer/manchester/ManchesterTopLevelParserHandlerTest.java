package sim.explainer.library.util.syntaxanalyzer.manchester;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import sim.explainer.library.util.syntaxanalyzer.HandlerContextImpl;

class ManchesterTopLevelParserHandlerTest {

    private final ManchesterTopLevelParserHandler handler = new ManchesterTopLevelParserHandler();

    @Test
    void disjunctiveOnlyFiller_isExtractedAsOneUniversalFiller() {
        HandlerContextImpl context = new HandlerContextImpl();
        String result = handler.returnTopLevelConceptStringIfAvailable(
                context, "hasTopping only (MozzarellaTopping or TomatoTopping)");

        assertNotNull(result);
        // the whole "or"-expression should be stored as ONE filler for hasTopping,
        // not flattened into two separate top-level primitives
        String stored = context.getEdgePrimitiveConceptUniversalMap().get("hasTopping").iterator().next();
        org.junit.jupiter.api.Assertions.assertTrue(stored.contains("or"));
    }

    @Test
    void singleNonDisjunctiveOnlyFiller_stillWorks_regressionCheck() {
        HandlerContextImpl context = new HandlerContextImpl();
        String result = handler.returnTopLevelConceptStringIfAvailable(
                context, "hasTopping only MozzarellaTopping");

        assertNotNull(result);
        String stored = context.getEdgePrimitiveConceptUniversalMap().get("hasTopping").iterator().next();
        org.junit.jupiter.api.Assertions.assertEquals("MozzarellaTopping", stored);
    }

    @Test
    void someRestriction_unaffectedByOnlyDispatchFix() {
        HandlerContextImpl context = new HandlerContextImpl();
        String result = handler.returnTopLevelConceptStringIfAvailable(
                context, "hasTopping some MozzarellaTopping");

        assertNotNull(result);
        String stored = context.getEdgePrimitiveConceptExistentialMap().get("hasTopping").iterator().next();
        org.junit.jupiter.api.Assertions.assertEquals("MozzarellaTopping", stored);
    }
}