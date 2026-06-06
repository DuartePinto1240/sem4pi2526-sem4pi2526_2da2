// ─── domain/simulationreport/SimulationReportTest.java ────────────────────

package domain.simulationreport;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SimulationReportTest {

    private static final SimulationReportId    ID         =
            SimulationReportId.of("simulation_report_20260601_143022");
    private static final LocalDateTime         NOW        = LocalDateTime.now();
    private static final OverallValidationResult PASS     = OverallValidationResult.pass();
    private static final OverallValidationResult FAIL     = OverallValidationResult.fail();
    private static final FlightExecutionStatus STATUS_OK  =
            new FlightExecutionStatus(0, FlightExecutionStatus.Status.OK);
    private static final FlightExecutionStatus STATUS_VIO =
            new FlightExecutionStatus(1, FlightExecutionStatus.Status.VIOLATED);
    private static final FlightExecutionStatus STATUS_ABT =
            new FlightExecutionStatus(2, FlightExecutionStatus.Status.ABORTED);
    private static final SafetyViolationEvent  VIOLATION  =
            new SafetyViolationEvent(12.0, 0, 1,
                    41.25, -8.68, 10000.0, 0.1, 0.2, 0.3,
                    41.26, -8.69, 10000.0, 0.4, 0.5, 0.6);

    // ── Valid creation ─────────────────────────────────────────────────────

    @Test
    void ensureValidReportIsCreated() {
        final SimulationReport r = new SimulationReport(
                ID, NOW, 2, List.of(STATUS_OK, STATUS_VIO),
                List.of(VIOLATION), FAIL);
        assertNotNull(r);
        assertEquals(ID, r.identity());
        assertEquals(NOW, r.generatedAt());
        assertEquals(2, r.totalFlights());
        assertFalse(r.result().isPassed());
        assertEquals(2, r.flightStatuses().size());
        assertEquals(1, r.violationEvents().size());
    }

    @Test
    void ensureReportWithNoViolationsIsPass() {
        final SimulationReport r = new SimulationReport(
                ID, NOW, 1, List.of(STATUS_OK),
                List.of(), PASS);
        assertTrue(r.result().isPassed());
        assertEquals(0, r.violationEvents().size());
    }

    @Test
    void ensureNullViolationListIsAccepted() {
        final SimulationReport r = new SimulationReport(
                ID, NOW, 1, List.of(STATUS_OK),
                null, PASS);
        assertNotNull(r.violationEvents());
        assertTrue(r.violationEvents().isEmpty());
    }

    // ── Validation ────────────────────────────────────────────────────────

    @Test
    void ensureNullIdIsRejected() {
        assertThrows(Exception.class, () ->
                new SimulationReport(null, NOW, 1,
                        List.of(STATUS_OK), List.of(), PASS));
    }

    @Test
    void ensureNullGeneratedAtIsRejected() {
        assertThrows(Exception.class, () ->
                new SimulationReport(ID, null, 1,
                        List.of(STATUS_OK), List.of(), PASS));
    }

    @Test
    void ensureNullResultIsRejected() {
        assertThrows(Exception.class, () ->
                new SimulationReport(ID, NOW, 1,
                        List.of(STATUS_OK), List.of(), null));
    }

    @Test
    void ensureZeroTotalFlightsIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new SimulationReport(ID, NOW, 0,
                        List.of(STATUS_OK), List.of(), PASS));
    }

    @Test
    void ensureEmptyStatusListIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new SimulationReport(ID, NOW, 1,
                        List.of(), List.of(), PASS));
    }

    // ── Identity / sameAs ─────────────────────────────────────────────────

    @Test
    void ensureSameAsReturnsTrueForSameId() {
        final SimulationReport r1 = new SimulationReport(
                ID, NOW, 1, List.of(STATUS_OK), List.of(), PASS);
        final SimulationReport r2 = new SimulationReport(
                ID, NOW.plusDays(1), 2,
                List.of(STATUS_OK, STATUS_VIO), List.of(), FAIL);
        assertTrue(r1.sameAs(r2));
    }

    @Test
    void ensureSameAsReturnsFalseForDifferentId() {
        final SimulationReport r1 = new SimulationReport(
                SimulationReportId.of("RPT-001"), NOW, 1,
                List.of(STATUS_OK), List.of(), PASS);
        final SimulationReport r2 = new SimulationReport(
                SimulationReportId.of("RPT-002"), NOW, 1,
                List.of(STATUS_OK), List.of(), PASS);
        assertFalse(r1.sameAs(r2));
    }

    @Test
    void ensureSameAsReturnsFalseForNonReport() {
        final SimulationReport r = new SimulationReport(
                ID, NOW, 1, List.of(STATUS_OK), List.of(), PASS);
        assertFalse(r.sameAs("not a report"));
    }

    // ── Immutability ──────────────────────────────────────────────────────

    @Test
    void ensureFlightStatusesListIsUnmodifiable() {
        final SimulationReport r = new SimulationReport(
                ID, NOW, 1, List.of(STATUS_OK), List.of(), PASS);
        assertThrows(UnsupportedOperationException.class,
                () -> r.flightStatuses().add(STATUS_VIO));
    }

    @Test
    void ensureViolationEventsListIsUnmodifiable() {
        final SimulationReport r = new SimulationReport(
                ID, NOW, 1, List.of(STATUS_OK),
                List.of(VIOLATION), PASS);
        assertThrows(UnsupportedOperationException.class,
                () -> r.violationEvents().add(VIOLATION));
    }

    // ── toString ──────────────────────────────────────────────────────────

    @Test
    void ensureToStringContainsId() {
        final SimulationReport r = new SimulationReport(
                ID, NOW, 1, List.of(STATUS_OK), List.of(), PASS);
        assertTrue(r.toString().contains(ID.value()));
    }

    // ── JPA constructor ───────────────────────────────────────────────────

    @Test
    void ensureProtectedNoArgConstructorExistsForJpa() throws Exception {
        final Constructor<SimulationReport> ctor =
                SimulationReport.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }

    // ── All status types ──────────────────────────────────────────────────

    @Test
    void ensureAllFlightStatusTypesAreSupported() {
        final SimulationReport r = new SimulationReport(
                SimulationReportId.of("RPT-003"), NOW, 3,
                List.of(STATUS_OK, STATUS_VIO, STATUS_ABT),
                List.of(), FAIL);
        assertEquals(3, r.flightStatuses().size());
        assertEquals(FlightExecutionStatus.Status.OK,
                r.flightStatuses().get(0).status());
        assertEquals(FlightExecutionStatus.Status.VIOLATED,
                r.flightStatuses().get(1).status());
        assertEquals(FlightExecutionStatus.Status.ABORTED,
                r.flightStatuses().get(2).status());
    }
}