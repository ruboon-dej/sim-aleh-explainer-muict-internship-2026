package sim.explainer.library.util.syntaxanalyzer;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import sim.explainer.library.exception.JSimPiException;
import sim.explainer.library.exception.ErrorCode;
import sim.explainer.library.util.MyStringUtils;
import sim.explainer.library.util.ParserUtils;

/**
 * Splits a concept description into its top-level disjuncts D_1, ..., D_n
 * (Def. 3.3's D = D_1 (or) ... (or) D_n) BEFORE the per-conjunct KRSS/Manchester
 * handler chains run on each D_i individually. This has to be parenthesis-depth
 * aware: "A and (B or C)" must not be split at the "or" nested inside the
 * existential/universal restriction's argument, only at "or"s appearing at the
 * outermost level of the whole description.
 */
@Component
public class ALCDisjunctionSplitterHandler {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Manchester syntax writes disjunction infix: {@code "A and B or C and D"}.
     * Splits on every top-level (depth-0, outside any parenthesis) " or ".
     */
    public List<String> splitManchesterDisjunction(String conceptDescription) {
        if (conceptDescription == null) {
            throw new JSimPiException("Unable to split null concept description.", ErrorCode.ALCDisjunctionSplitterHandler_IllegalArguments);
        }

        String compact = ParserUtils.compactConceptDescriptionString(conceptDescription);
        compact = stripRedundantOuterWrap(compact);
        List<Integer> splitPoints = ParserUtils.findTopLevelKeywordIndices(compact, "or");

        List<String> disjuncts = new ArrayList<>();
        int start = 0;
        for (int splitPoint : splitPoints) {
            disjuncts.add(compact.substring(start, splitPoint).trim());
            start = splitPoint + "or".length();
        }
        disjuncts.add(compact.substring(start).trim());

        return stripOuterParenthesisIfWholeString(disjuncts);
    }

    /**
     * KRSS syntax writes disjunction prefix: {@code "(or (and A B) (and C D))"}.
     * Finds the {@code (or ...)} wrapper (if any) and splits its arguments at
     * top-level parenthesis boundaries.
     */
    public List<String> splitKRSSDisjunction(String conceptDescription) {
        if (conceptDescription == null) {
            throw new JSimPiException("Unable to split null concept description.", ErrorCode.ALCDisjunctionSplitterHandler_IllegalArguments);
        }

        String compact = ParserUtils.compactConceptDescriptionString(conceptDescription).trim();

        if (!compact.startsWith("(or ")) {
            List<String> single = new ArrayList<>();
            single.add(compact);
            return single;
        }

        String inner = compact.substring("(or ".length(), compact.length() - 1).trim();

        List<String> disjuncts = new ArrayList<>();
        int depth = 0;
        int tokenStart = 0;

        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == ParserUtils.OPEN_PARENTHESIS_CHAR) {
                depth++;
            } else if (c == ParserUtils.CLOSE_PARENTHESIS_CHAR) {
                depth--;
                if (depth == 0) {
                    disjuncts.add(inner.substring(tokenStart, i + 1).trim());
                    tokenStart = i + 1;
                }
            }
        }

        if (disjuncts.isEmpty()) {
            // Arguments were bare primitive names, not parenthesized sub-expressions.
            for (String token : inner.split("\\s+")) {
                if (!token.isEmpty()) {
                    disjuncts.add(token.trim());
                }
            }
        }

        return disjuncts;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private List<String> stripOuterParenthesisIfWholeString(List<String> disjuncts) {
        List<String> result = new ArrayList<>();
        for (String disjunct : disjuncts) {
            String trimmed = disjunct.trim();
            if (trimmed.startsWith(ParserUtils.OPEN_PARENTHESIS_STR) && trimmed.endsWith(ParserUtils.CLOSE_PARENTHESIS_STR)
                    && ParserUtils.getLastMatchedCloseParenthesis(trimmed) == trimmed.length() - 1) {
                trimmed = MyStringUtils.removeCharactersFrom(trimmed, 0, trimmed.length() - 2);
            }
            result.add(trimmed.trim());
        }
        return result;
    }

    private String stripRedundantOuterWrap(String str) {
        String trimmed = str.trim();
        while (trimmed.startsWith(ParserUtils.OPEN_PARENTHESIS_STR) && trimmed.endsWith(ParserUtils.CLOSE_PARENTHESIS_STR)
                && ParserUtils.getLastMatchedCloseParenthesis(trimmed) == trimmed.length() - 1) {
            trimmed = MyStringUtils.removeCharactersFrom(trimmed, 0, trimmed.length() - 2).trim();
        }
        return trimmed;
    }
}