package controller;

import domain.simulationreport.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.SimulationReportRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonthlyReportControllerTest {

    private SimulationReportRepository repo;
    private MonthlyReportController ctrl;

    // ── helpers ───────────────────────────────────────────────────────────────

    private SimulationReport reportFor(final int year, final int month,
                                       final String id, final boolean pass) {
        return new SimulationReport(
                SimulationReportId.of(id),
                LocalDateTime.of(year, month, 15, 10, 0),
                2,
                List.of(
                        new FlightExecutionStatus(1, FlightExecutionStatus.Status.OK),
                        new FlightExecutionStatus(2, pass
                                ? FlightExecutionStatus.Status.OK
                                : FlightExecutionStatus.Status.VIOLATED)
                ),
                List.of(),
                pass ? OverallValidationResult.pass() : OverallValidationResult.fail()
        );
    }

    @BeforeEach
    void setUp() {
        repo = mock(SimulationReportRepository.class);
        ctrl = new MonthlyReportController(repo);
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    /**
     * Test 1 — Reports from other months are excluded.
     * Refers to: US112.1, US112.2
     */
    @Test
    void ensureOnlyReportsFromRequestedMonthAreIncluded() {
        when(repo.findAll()).thenReturn(List.of(
                reportFor(2026, 5, "sim_may",  true),
                reportFor(2026, 6, "sim_june", false)
        ));

        final String preview = ctrl.previewMonthlyReport(2026, 5);

        assertTrue(preview.contains("sim_may"),  "May report should be included");
        assertFalse(preview.contains("sim_june"), "June report should NOT be included");
    }

    /**
     * Test 2 — Empty month produces a valid report with no-data message.
     * Refers to: US112.1, US112.2
     */
    @Test
    void ensureEmptyMonthProducesValidReport() {
        when(repo.findAll()).thenReturn(List.of());

        final String preview = ctrl.previewMonthlyReport(2026, 5);

        assertNotNull(preview);
        assertTrue(preview.contains("No simulation reports found"),
                "Should indicate no data for this period");
    }

    /**
     * Test 3 — Header always contains AISafe branding.
     * Refers to: US112.3
     */
    @Test
    void ensureReportHeaderContainsBranding() {
        when(repo.findAll()).thenReturn(List.of());

        final String preview = ctrl.previewMonthlyReport(2026, 5);

        assertTrue(preview.contains("AISafe"),
                "Header must contain AISafe branding");
        assertTrue(preview.contains("Monthly Statistics Report"),
                "Header must contain the report title");
    }

    /**
     * Test 4 — Report includes the correct period label.
     * Refers to: US112.1, US112.3
     */
    @Test
    void ensureReportPeriodIsCorrect() {
        when(repo.findAll()).thenReturn(List.of());

        final String preview = ctrl.previewMonthlyReport(2026, 5);

        assertTrue(preview.contains("May 2026"), "Report period must be 'May 2026'");
    }

    /**
     * Test 5 — Pass/fail counts are correctly aggregated.
     * Refers to: US112.2
     */
    @Test
    void ensurePassFailCountsAreCorrect() {
        when(repo.findAll()).thenReturn(List.of(
                reportFor(2026, 5, "sim_pass", true),
                reportFor(2026, 5, "sim_fail", false)
        ));

        final String preview = ctrl.previewMonthlyReport(2026, 5);

        assertTrue(preview.contains("Simulations passed       : 1"));
        assertTrue(preview.contains("Simulations failed       : 1"));
    }

    /**
     * Test 6 — Total flights are summed across all reports in the month.
     * Refers to: US112.2
     */
    @Test
    void ensureTotalFlightsAreSummed() {
        when(repo.findAll()).thenReturn(List.of(
                reportFor(2026, 5, "sim_a", true),
                reportFor(2026, 5, "sim_b", true)
        ));

        // Each report has 2 flights → total should be 4
        final String preview = ctrl.previewMonthlyReport(2026, 5);

        assertTrue(preview.contains("Total flights simulated  : 4"));
    }

    /**
     * Test 7 — generateMonthlyReport saves to a file (file name pattern check).
     * Refers to: US112.4
     */
    @Test
    void ensureGeneratedFilenameMatchesPattern() {
        when(repo.findAll()).thenReturn(List.of());

        final String filename = ctrl.generateMonthlyReport(2026, 5);

        assertTrue(filename.contains("report_monthly_2026_05_"),
                "Filename must contain 'report_monthly_2026_05_'");
        assertTrue(filename.endsWith(".txt"),
                "Filename must end with '.txt'");

        // clean up
        final java.io.File f = new java.io.File(filename);
        f.delete();
        if (f.getParentFile() != null) f.getParentFile().delete();
    }

    /**
     * Test 8 — Invalid month throws IllegalArgumentException.
     * Refers to: US112.1
     */
    @Test
    void ensureInvalidMonthIsRejected() {
        when(repo.findAll()).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> ctrl.previewMonthlyReport(2026, 13));
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.previewMonthlyReport(2026, 0));
    }
}
