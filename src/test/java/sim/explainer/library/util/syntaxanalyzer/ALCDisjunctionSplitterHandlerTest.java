package sim.explainer.library.util.syntaxanalyzer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ALCDisjunctionSplitterHandlerTest {

    private final ALCDisjunctionSplitterHandler handler = new ALCDisjunctionSplitterHandler();

    @Test
    void manchester_noTopLevelOr_returnsSingleton() {
        List<String> result = handler.splitManchesterDisjunction("MozzarellaTopping");
        assertEquals(1, result.size());
    }

    @Test
    void manchester_twoDisjuncts_splitsCorrectly() {
        List<String> result = handler.splitManchesterDisjunction("MozzarellaTopping or TomatoTopping");
        assertEquals(2, result.size());
    }

    @Test
    void manchester_fourDisjuncts_splitsCorrectly() {
        List<String> result = handler.splitManchesterDisjunction("A or B or C or D");
        assertEquals(4, result.size());
    }

    @Test
    void manchester_nestedParensNotMistakenForTopLevelSplit() {
        // The "or" inside the nested parens belongs to a deeper sub-expression, not
        // this level - only the outer "or" should count as a top-level split.
        List<String> result = handler.splitManchesterDisjunction("(A and (B or C)) or D");
        assertEquals(2, result.size());
    }

    @Test
    void krss_noTopLevelOr_returnsSingleton() {
        List<String> result = handler.splitKRSSDisjunction("MozzarellaTopping");
        assertEquals(1, result.size());
    }

    @Test
    void krss_prefixOrForm_splitsCorrectly() {
        List<String> result = handler.splitKRSSDisjunction("(or MozzarellaTopping TomatoTopping)");
        assertEquals(2, result.size());
    }
}