package lprog;

import java.util.Collections;
import java.util.List;

/**
 * Value object representing the outcome of a US085 flight plan test.
 *
 * <p>On PASS, also carries post-simulation flight statistics computed from
 * the aircraft model characteristics: total distance, fuel burn, remaining
 * fuel, and estimated flight duration.</p>
 */
public class FlightPlanTestResult {

    private final boolean      passed;
    private final List<String> errors;

    /* Post-simulation stats (only meaningful when passed == true) */
    private final double totalDistanceKm;
    private final double totalDeclaredFuelKg;
    private final double estimatedFuelBurnKg;
    private final double remainingFuelKg;
    private final String estimatedDuration;   /* e.g. "2h 35min" */

    private FlightPlanTestResult(final boolean      passed,
                                 final List<String> errors,
                                 final double       totalDistanceKm,
                                 final double       totalDeclaredFuelKg,
                                 final double       estimatedFuelBurnKg,
                                 final double       remainingFuelKg,
                                 final String       estimatedDuration) {
        this.passed               = passed;
        this.errors               = Collections.unmodifiableList(errors);
        this.totalDistanceKm      = totalDistanceKm;
        this.totalDeclaredFuelKg  = totalDeclaredFuelKg;
        this.estimatedFuelBurnKg  = estimatedFuelBurnKg;
        this.remainingFuelKg      = remainingFuelKg;
        this.estimatedDuration    = estimatedDuration;
    }

    /** Creates a PASS result with full flight statistics. */
    public static FlightPlanTestResult pass(final double totalDistanceKm,
                                            final double totalDeclaredFuelKg,
                                            final double estimatedFuelBurnKg,
                                            final double cruiseSpeedKmh) {
        final double remaining = totalDeclaredFuelKg - estimatedFuelBurnKg;
        final String duration  = formatDuration(totalDistanceKm, cruiseSpeedKmh);
        return new FlightPlanTestResult(true, List.of(),
                totalDistanceKm, totalDeclaredFuelKg,
                estimatedFuelBurnKg, remaining, duration);
    }

    /** Creates a PASS result without stats (backwards-compatible). */
    public static FlightPlanTestResult pass() {
        return new FlightPlanTestResult(true, List.of(), 0, 0, 0, 0, "N/A");
    }

    /** Creates a FAIL result. */
    public static FlightPlanTestResult fail(final List<String> errors) {
        return new FlightPlanTestResult(false, errors, 0, 0, 0, 0, null);
    }

    public boolean      isPassed()              { return passed;              }
    public List<String> errors()                { return errors;              }
    public double       totalDistanceKm()       { return totalDistanceKm;     }
    public double       totalDeclaredFuelKg()   { return totalDeclaredFuelKg; }
    public double       estimatedFuelBurnKg()   { return estimatedFuelBurnKg; }
    public double       remainingFuelKg()       { return remainingFuelKg;     }
    public String       estimatedDuration()     { return estimatedDuration;   }

    public String summary() {
        if (passed) return "PASS — flight plan is valid and feasible.";
        final StringBuilder sb = new StringBuilder("FAIL\n");
        for (final String e : errors) sb.append("  • ").append(e).append('\n');
        return sb.toString().stripTrailing();
    }

    @Override
    public String toString() { return summary(); }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String formatDuration(final double distanceKm,
                                         final double cruiseSpeedKmh) {
        if (cruiseSpeedKmh <= 0) return "N/A";
        final double totalMinutes = (distanceKm / cruiseSpeedKmh) * 60.0;
        final int hours   = (int) (totalMinutes / 60);
        final int minutes = (int) (totalMinutes % 60);
        return hours > 0
                ? String.format("%dh %02dmin", hours, minutes)
                : String.format("%dmin", minutes);
    }
}
