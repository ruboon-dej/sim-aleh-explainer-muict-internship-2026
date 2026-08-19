package sim.explainer.library.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import sim.explainer.library.exception.ErrorCode;
import sim.explainer.library.exception.JSimPiException;

/**
 * Holds the one tunable knob Def. 4.1 actually defines: lambda, the weighting factor
 * applied to the max-over-disjunct-pairs term, which the paper suggests "might be
 * defined as a function of the level where the sub-concepts occur" (e.g. lambda = 1/level).
 *
 * If no fixed value has been set, {@link #getLevelDiscountFactor(int)} auto-computes
 * 1/level so deeper sub-concepts matter less by default, matching the paper's own
 * example without requiring the caller to configure anything.
 */
@Component
public class ALCPreferenceProfile {

    private static final Logger logger = LoggerFactory.getLogger(ALCPreferenceProfile.class);

    private Double fixedLevelDiscountFactor = null;

    public ALCPreferenceProfile() {
        logger.info("ALCPreferenceProfile initialized - lambda defaults to auto (1/level) until configured");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Fixes lambda to a constant value for every level, overriding the auto 1/level
     * behavior. Pass {@code null} via {@link #resetToAutoLevelDiscount()} to go back
     * to auto-detect.
     */
    public void setFixedLevelDiscountFactor(double lambda) {
        if (lambda < 0 || lambda > 1) {
            throw new JSimPiException("Level discount factor must be between 0 and 1.", ErrorCode.ALCPreferenceProfile_IllegalArguments);
        }
        this.fixedLevelDiscountFactor = lambda;
    }

    public void resetToAutoLevelDiscount() {
        this.fixedLevelDiscountFactor = null;
    }

    public boolean isAutoLevelDiscount() {
        return fixedLevelDiscountFactor == null;
    }

    /**
     * @param level recursion depth at which the disjunction is being compared
     *              (0 = top-level call).
     * @return the configured fixed lambda if one was set, otherwise 1/level
     *         (with level 0 treated as no discount, i.e. lambda = 1).
     */
    public double getLevelDiscountFactor(int level) {
        if (level < 0) {
            throw new JSimPiException("Unable to get level discount factor as level[" + level + "] is negative.", ErrorCode.ALCPreferenceProfile_IllegalArguments);
        }

        if (fixedLevelDiscountFactor != null) {
            return fixedLevelDiscountFactor;
        }

        return level == 0 ? 1.0 : 1.0 / level;
    }
}
