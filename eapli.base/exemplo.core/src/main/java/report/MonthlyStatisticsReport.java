package report;

import domain.simulationreport.FlightExecutionStatus;
import domain.simulationreport.SafetyViolationEvent;
import domain.simulationreport.SimulationReport;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Concrete monthly statistics report (US112).
 *
 * <p>Extends {@link AbstractReport} — only {@link #generateBody()} is
 * specific to this report type.  Future report types (compliance, incident,
 * etc.) follow the same pattern.</p>
 */
public class MonthlyStatisticsReport extends AbstractReport {

    private final int year;
    private final int month;
    private final List<SimulationReport> data;

    public MonthlyStatisticsReport(final int year, final int month,
                                   final List<SimulationReport> data) {
        if (month < 1 || month > 12)
            throw new IllegalArgumentException("Month must be between 1 and 12");
        if (data == null)
            throw new IllegalArgumentException("Data list cannot be null");
        this.year  = year;
        this.month = month;
        this.data  = data;
    }

    @Override
    protected String reportTitle() {
        return "Monthly Statistics Report";
    }

    @Override
    protected String reportPeriod() {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;
    }

    @Override
    protected String generateBody() {
        if (data.isEmpty()) {
            return "SUMMARY\n  No simulation reports found for this period.\n";
        }

        int totalFlights    = 0;
        int totalViolations = 0;
        int passed          = 0;
        int failed          = 0;

        for (final SimulationReport r : data) {
            totalFlights    += r.totalFlights();
            totalViolations += r.violationEvents().size();
            if (r.result().isPassed()) passed++;
            else                       failed++;
        }

        final double passRate = data.isEmpty() ? 0.0
                : (passed * 100.0) / data.size();

        final StringBuilder sb = new StringBuilder();

        sb.append("SUMMARY\n");
        sb.append(String.format("  Total simulations run    : %d%n", data.size()));
        sb.append(String.format("  Total flights simulated  : %d%n", totalFlights));
        sb.append(String.format("  Total violations         : %d%n", totalViolations));
        sb.append(String.format("  Simulations passed       : %d%n", passed));
        sb.append(String.format("  Simulations failed       : %d%n", failed));
        sb.append(String.format("  Pass rate                : %.1f%%%n", passRate));

        sb.append("\nDETAIL\n");
        for (final SimulationReport r : data) {
            sb.append(String.format("  Report %-30s | flights: %2d | violations: %2d | %s%n",
                    r.identity(),
                    r.totalFlights(),
                    r.violationEvents().size(),
                    r.result().isPassed() ? "PASS" : "FAIL"));

            for (final FlightExecutionStatus s : r.flightStatuses()) {
                sb.append(String.format("      %-10s → %s%n",
                        "Flight " + s.flightIdentifier(), s.status().name()));
            }

            if (!r.violationEvents().isEmpty()) {
                sb.append("    Violations:\n");
                int i = 1;
                for (final SafetyViolationEvent v : r.violationEvents()) {
                    sb.append(String.format("      #%d — %s%n", i++, v));
                }
            }
        }

        return sb.toString();
    }
}
