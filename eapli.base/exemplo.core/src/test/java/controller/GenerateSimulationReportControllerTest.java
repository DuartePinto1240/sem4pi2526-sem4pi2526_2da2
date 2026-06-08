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
    /**
     * Test 10 — availableReportFiles returns empty when directory does not exist.
     * Covers: files == null branch in availableReportFiles().
     */
    @Test
    void ensureNonExistentDirectoryReturnsEmptyList() {
        final GenerateSimulationReportController ctrlBadDir =
                new GenerateSimulationReportController(repo, "/nonexistent/path/xyz");
        assertTrue(ctrlBadDir.availableReportFiles().isEmpty());
    }

    /**
     * Test 11 — Report with 0 total flights falls back to Math.max(0, 1) = 1.
     * Covers: Math.max(totalFlights, 1) branch.
     */
    @Test
    void ensureReportWithZeroFlightsDefaultsToOne() throws IOException {
        final String content =
                "========================================\n"
                        + "  AISafe Simulation Report\n"
                        + "  Generated: Sat Jun  6 21:00:17 2026\n"
                        + "========================================\n\n"
                        + "--- Summary ---\n"
                        + "Total flights    : 0\n"
                        + "Total violations : 0\n"
                        + "Result           : PASS\n\n"
                        + "--- Flight Statuses ---\n"
                        + "Flight 0 : OK\n\n"
                        + "--- Safety Violation Events ---\n"
                        + "No violations recorded.\n\n"
                        + "========================================\n"
                        + "  End of Report\n"
                        + "========================================\n";
        writeReport("simulation_report_20260606_220000.txt", content);

        final SimulationReport r =
                ctrl.generateReport("simulation_report_20260606_220000.txt");

        assertEquals(1, r.totalFlights(),
                "totalFlights=0 in file must default to 1 via Math.max");
    }

    /**
     * Test 12 — Report with no Flight Statuses section triggers fallback.
     * Covers: if (statuses.isEmpty()) branch.
     */
    @Test
    void ensureReportWithNoStatusSectionCreatesDefaultStatuses() throws IOException {
        final String content =
                "========================================\n"
                        + "  AISafe Simulation Report\n"
                        + "  Generated: Sat Jun  6 21:00:17 2026\n"
                        + "========================================\n\n"
                        + "--- Summary ---\n"
                        + "Total flights    : 2\n"
                        + "Total violations : 0\n"
                        + "Result           : PASS\n\n"
                        + "--- Safety Violation Events ---\n"
                        + "No violations recorded.\n\n"
                        + "========================================\n"
                        + "  End of Report\n"
                        + "========================================\n";
        writeReport("simulation_report_20260606_220001.txt", content);

        final SimulationReport r =
                ctrl.generateReport("simulation_report_20260606_220001.txt");

        assertEquals(2, r.flightStatuses().size(),
                "Fallback must create one OK status per flight");
        r.flightStatuses().forEach(s ->
                assertEquals(FlightExecutionStatus.Status.OK, s.status()));
    }

    /**
     * Test 13 — Report with ABORTED flight status is correctly parsed.
     * Covers: case "ABORTED" branch in status switch.
     */
    @Test
    void ensureAbortedFlightStatusIsParsed() throws IOException {
        final String content =
                "========================================\n"
                        + "  AISafe Simulation Report\n"
                        + "  Generated: Sat Jun  6 21:00:17 2026\n"
                        + "========================================\n\n"
                        + "--- Summary ---\n"
                        + "Total flights    : 1\n"
                        + "Total violations : 0\n"
                        + "Result           : FAIL\n\n"
                        + "--- Flight Statuses ---\n"
                        + "Flight 0 : ABORTED\n\n"
                        + "--- Safety Violation Events ---\n"
                        + "No violations recorded.\n\n"
                        + "========================================\n"
                        + "  End of Report\n"
                        + "========================================\n";
        writeReport("simulation_report_20260606_220002.txt", content);

        final SimulationReport r =
                ctrl.generateReport("simulation_report_20260606_220002.txt");

        assertEquals(FlightExecutionStatus.Status.ABORTED,
                r.flightStatuses().get(0).status());
    }

    /**
     * Test 14 — Report with unparseable date falls back to now().
     * Covers: catch block in Generated: parsing.
     */
    @Test
    void ensureUnparseableDateFallsBackToNow() throws IOException {
        final String content =
                "========================================\n"
                        + "  AISafe Simulation Report\n"
                        + "  Generated: INVALID DATE FORMAT\n"
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
        writeReport("simulation_report_20260606_220003.txt", content);

        final SimulationReport r =
                ctrl.generateReport("simulation_report_20260606_220003.txt");

        assertNotNull(r.generatedAt(),
                "generatedAt must not be null even with invalid date");
    }

    /**
     * Test 15 — Violation with malformed Lat/Lon/Alt line is silently ignored.
     * Covers: catch block in parseLatLonAlt.
     */
    @Test
    void ensureMalformedLatLonAltIsSilentlyIgnored() throws IOException {
        final String content =
                "========================================\n"
                        + "  AISafe Simulation Report\n"
                        + "  Generated: Sat Jun  6 21:00:17 2026\n"
                        + "========================================\n\n"
                        + "--- Summary ---\n"
                        + "Total flights    : 2\n"
                        + "Total violations : 1\n"
                        + "Result           : FAIL\n\n"
                        + "--- Flight Statuses ---\n"
                        + "Flight 0 : VIOLATED\n"
                        + "Flight 1 : VIOLATED\n\n"
                        + "--- Safety Violation Events ---\n\n"
                        + "Violation #1\n"
                        + "  Timestamp : 1.00 s\n"
                        + "  Flight 1  : 0\n"
                        + "    Lat=MALFORMED\n"
                        + "    Velocity : vx=0.0000 vy=0.0000 vz=0.0000\n"
                        + "  Flight 2  : 1\n"
                        + "    Lat=41.2598 Lon=-8.6887 Alt=81.0\n"
                        + "    Velocity : vx=MALFORMED\n\n"
                        + "========================================\n"
                        + "  End of Report\n"
                        + "========================================\n";
        writeReport("simulation_report_20260606_220004.txt", content);

        assertDoesNotThrow(() ->
                        ctrl.generateReport("simulation_report_20260606_220004.txt"),
                "Malformed position/velocity lines must not throw");
    }

    /**
     * Test 16 — Package-private constructor with repo only uses default dir.
     * Covers: GenerateSimulationReportController(repo) constructor.
     */
    @Test
    void ensureRepoOnlyConstructorCreatesController() {
        final GenerateSimulationReportController c =
                new GenerateSimulationReportController(repo);
        assertNotNull(c);
        // availableReportFiles uses default dir — may return empty or not,
        // but must not throw
        assertDoesNotThrow(c::availableReportFiles);
    }

    /**
     * Test 17 — availableReportFiles returns files in sorted order.
     * Covers: .sorted() in stream pipeline.
     */
    @Test
    void ensureAvailableFilesAreSorted() throws IOException {
        writeReport("simulation_report_20260606_210055.txt", passReport1Flight());
        writeReport("simulation_report_20260606_210017.txt", passReport1Flight());
        writeReport("simulation_report_20260606_210033.txt", passReport1Flight());

        final List<String> files = ctrl.availableReportFiles();

        assertEquals(3, files.size());
        for (int i = 0; i < files.size() - 1; i++) {
            assertTrue(files.get(i).compareTo(files.get(i + 1)) < 0,
                    "Files must be in sorted order");
        }
    }
}
