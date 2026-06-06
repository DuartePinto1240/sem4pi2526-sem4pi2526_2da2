package controller;

import domain.simulationreport.FlightExecutionStatus;
import domain.simulationreport.SimulationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.SimulationReportRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for US111 — GenerateSimulationReportController.
 *
 * <p>Uses a {@code @TempDir} for report files and a mocked
 * {@link SimulationReportRepository}, so no real database is needed.</p>
 */
class GenerateSimulationReportControllerTest {

    @TempDir
    Path tempDir;

    private SimulationReportRepository repo;
    private GenerateSimulationReportController ctrl;

    // ── report-file content helpers ───────────────────────────────────────

    /** Minimal PASS report — 1 flight, 0 violations. */
    private static String passReport1Flight() {
        return "========================================\n"
             + "  AISafe Simulation Report\n"
             + "  Generated: Sat Jun  6 21:00:17 2026\n"
             + "========================================\n\n"
             + "--- Summary ---\n"
             + "Total flights    : 1\n"
             + "Total violations : 0\n"
             + "Result           : PASS\n\n"
             + "--- Flight Statuses ---\n"
             + "Flight 0 : OK\n\n"
             + "--- Safety Violation Events ---\n"
             + "No violations recorded.\n\n"
             + "========================================\n"
             + "  End of Report\n"
             + "========================================\n";
    }

    /** FAIL report — 2 flights, 3 violations. */
    private static String failReport2Flights3Violations() {
        return "========================================\n"
             + "  AISafe Simulation Report\n"
             + "  Generated: Sat Jun  6 21:02:23 2026\n"
             + "========================================\n\n"
             + "--- Summary ---\n"
             + "Total flights    : 2\n"
             + "Total violations : 3\n"
             + "Result           : FAIL\n\n"
             + "--- Flight Statuses ---\n"
             + "Flight 0 : VIOLATED\n"
             + "Flight 1 : VIOLATED\n\n"
             + "--- Safety Violation Events ---\n\n"
             + "Violation #1\n"
             + "  Timestamp : 1.00 s\n"
             + "  Flight 1  : 0\n"
             + "    Lat=41.2598 Lon=-8.6887 Alt=81.0\n"
             + "    Velocity : vx=0.0000 vy=0.0000 vz=0.0000\n"
             + "  Flight 2  : 1\n"
             + "    Lat=41.2598 Lon=-8.6887 Alt=81.0\n"
             + "    Velocity : vx=0.0000 vy=0.0000 vz=0.0000\n\n"
             + "Violation #2\n"
             + "  Timestamp : 2.00 s\n"
             + "  Flight 1  : 0\n"
             + "    Lat=41.2597 Lon=-8.6875 Alt=93.0\n"
             + "    Velocity : vx=0.0001 vy=0.0001 vz=0.0001\n"
             + "  Flight 2  : 1\n"
             + "    Lat=41.2597 Lon=-8.6875 Alt=93.0\n"
             + "    Velocity : vx=0.0001 vy=0.0001 vz=0.0001\n\n"
             + "Violation #3\n"
             + "  Timestamp : 3.00 s\n"
             + "  Flight 1  : 0\n"
             + "    Lat=41.2595 Lon=-8.6862 Alt=105.0\n"
             + "    Velocity : vx=0.0001 vy=0.0001 vz=0.0001\n"
             + "  Flight 2  : 1\n"
             + "    Lat=41.2595 Lon=-8.6862 Alt=105.0\n"
             + "    Velocity : vx=0.0001 vy=0.0001 vz=0.0001\n\n"
             + "========================================\n"
             + "  End of Report\n"
             + "========================================\n";
    }

    /** Writes content to a file named {@code filename} inside tempDir. */
    private void writeReport(final String filename, final String content)
            throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }

    @BeforeEach
    void setUp() {
        repo = mock(SimulationReportRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ctrl = new GenerateSimulationReportController(repo, tempDir.toString());
    }

    // ── availableReportFiles ───────────────────────────────────────────────

    /**
     * Test 1 — No files in the directory → empty list.
     * Refers to: US111.1
     */
    @Test
    void ensureEmptyDirectoryReturnsEmptyList() {
        assertTrue(ctrl.availableReportFiles().isEmpty(),
                "Empty directory must return empty list");
    }

    /**
     * Test 2 — Only files matching the naming pattern are listed.
     * Refers to: US111.1
     */
    @Test
    void ensureOnlyMatchingFilesAreListed() throws IOException {
        writeReport("simulation_report_20260606_210017.txt", passReport1Flight());
        writeReport("simulation_report_20260606_210055.txt", passReport1Flight());
        writeReport("other_file.txt", "irrelevant");

        final List<String> files = ctrl.availableReportFiles();

        assertEquals(2, files.size(), "Only simulation_report_* files should be listed");
        assertTrue(files.stream().allMatch(f -> f.startsWith("simulation_report_")));
    }

    // ── generateReport ─────────────────────────────────────────────────────

    /**
     * Test 3 — Non-existent file throws IllegalArgumentException.
     * Refers to: US111.2
     */
    @Test
    void ensureMissingFileThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.generateReport("simulation_report_nonexistent.txt"),
                "Missing file must throw IllegalArgumentException");
    }

    /**
     * Test 4 — PASS report is parsed with correct flight count.
     * Refers to: US111.2
     */
    @Test
    void ensurePassReportFlightCountIsCorrect() throws IOException {
        writeReport("simulation_report_20260606_210017.txt", passReport1Flight());

        final SimulationReport report =
                ctrl.generateReport("simulation_report_20260606_210017.txt");

        assertEquals(1, report.totalFlights(),
                "Report must have 1 flight");
    }

    /**
     * Test 5 — PASS report result is true.
     * Refers to: US111.2
     */
    @Test
    void ensurePassReportResultIsPass() throws IOException {
        writeReport("simulation_report_20260606_210017.txt", passReport1Flight());

        final SimulationReport report =
                ctrl.generateReport("simulation_report_20260606_210017.txt");

        assertTrue(report.result().isPassed(), "Result must be PASS");
        assertEquals(0, report.violationEvents().size(),
                "PASS report must have 0 violations");
    }

    /**
     * Test 6 — FAIL report result is false and violation count matches.
     * Refers to: US111.2
     */
    @Test
    void ensureFailReportResultIsFailWithCorrectViolationCount() throws IOException {
        writeReport("simulation_report_20260606_210223.txt",
                failReport2Flights3Violations());

        final SimulationReport report =
                ctrl.generateReport("simulation_report_20260606_210223.txt");

        assertFalse(report.result().isPassed(), "Result must be FAIL");
        assertEquals(3, report.violationEvents().size(),
                "FAIL report must have 3 violations");
    }

    /**
     * Test 7 — Flight statuses are correctly parsed (OK vs VIOLATED).
     * Refers to: US111.2
     */
    @Test
    void ensureFlightStatusesAreParsedCorrectly() throws IOException {
        writeReport("simulation_report_20260606_210223.txt",
                failReport2Flights3Violations());

        final SimulationReport report =
                ctrl.generateReport("simulation_report_20260606_210223.txt");

        final List<FlightExecutionStatus> statuses = report.flightStatuses();
        assertEquals(2, statuses.size());
        assertTrue(statuses.stream().anyMatch(
                s -> s.status() == FlightExecutionStatus.Status.VIOLATED),
                "At least one flight must be VIOLATED");
    }

    /**
     * Test 8 — generateReport saves the parsed report to the repository.
     * Refers to: US111.3
     */
    @Test
    void ensureReportIsSavedToRepository() throws IOException {
        writeReport("simulation_report_20260606_210017.txt", passReport1Flight());

        ctrl.generateReport("simulation_report_20260606_210017.txt");

        verify(repo, times(1)).save(any(SimulationReport.class));
    }

    /**
     * Test 9 — The identity of the saved report matches the filename (without .txt).
     * Refers to: US111.3
     */
    @Test
    void ensureReportIdMatchesFilename() throws IOException {
        final String filename = "simulation_report_20260606_210017.txt";
        writeReport(filename, passReport1Flight());

        final SimulationReport report = ctrl.generateReport(filename);

        assertEquals("simulation_report_20260606_210017",
                report.identity().value(),
                "Report ID must match filename without extension");
    }
}
