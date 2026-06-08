package lprog;

import java.util.Collections;
import java.util.List;

/**
 * Value object representing the outcome of a US085 flight plan test.
 */
public class FlightPlanTestResult {

    private final boolean      passed;
    private final List<String> errors;

    private FlightPlanTestResult(final boolean passed, final List<String> errors) {
        this.passed = passed;
        this.errors = Collections.unmodifiableList(errors);
    }

    public static FlightPlanTestResult pass() {
        return new FlightPlanTestResult(true, List.of());
    }

    public static FlightPlanTestResult fail(final List<String> errors) {
        return new FlightPlanTestResult(false, errors);
    }

    public boolean isPassed()          { return passed; }
    public List<String> errors()       { return errors; }

    public String summary() {
        if (passed) return "PASS — flight plan is valid and feasible.";
        final StringBuilder sb = new StringBuilder("FAIL\n");
        for (final String e : errors) sb.append("  • ").append(e).append('\n');
        return sb.toString().stripTrailing();
    }

    @Override
    public String toString() { return summary(); }
}
