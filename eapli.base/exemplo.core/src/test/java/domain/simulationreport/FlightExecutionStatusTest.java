// ─── domain/simulationreport/FlightExecutionStatusTest.java ───────────────

package domain.simulationreport;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

class FlightExecutionStatusTest {

    @Test
    void ensureOkStatusIsCreated() {
        final FlightExecutionStatus s =
                new FlightExecutionStatus(0, FlightExecutionStatus.Status.OK);
        assertEquals(0, s.flightIdentifier());
        assertEquals(FlightExecutionStatus.Status.OK, s.status());
    }

    @Test
    void ensureViolatedStatusIsCreated() {
        final FlightExecutionStatus s =
                new FlightExecutionStatus(1, FlightExecutionStatus.Status.VIOLATED);
        assertEquals(FlightExecutionStatus.Status.VIOLATED, s.status());
    }

    @Test
    void ensureAbortedStatusIsCreated() {
        final FlightExecutionStatus s =
                new FlightExecutionStatus(2, FlightExecutionStatus.Status.ABORTED);
        assertEquals(FlightExecutionStatus.Status.ABORTED, s.status());
    }

    @Test
    void ensureTwoStatusesWithSameValuesAreEqual() {
        assertEquals(
                new FlightExecutionStatus(0, FlightExecutionStatus.Status.OK),
                new FlightExecutionStatus(0, FlightExecutionStatus.Status.OK));
    }

    @Test
    void ensureTwoStatusesWithDifferentFlightAreNotEqual() {
        assertNotEquals(
                new FlightExecutionStatus(0, FlightExecutionStatus.Status.OK),
                new FlightExecutionStatus(1, FlightExecutionStatus.Status.OK));
    }

    @Test
    void ensureTwoStatusesWithDifferentStatusAreNotEqual() {
        assertNotEquals(
                new FlightExecutionStatus(0, FlightExecutionStatus.Status.OK),
                new FlightExecutionStatus(0, FlightExecutionStatus.Status.VIOLATED));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final FlightExecutionStatus s =
                new FlightExecutionStatus(0, FlightExecutionStatus.Status.OK);
        assertEquals(s, s);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null,
                new FlightExecutionStatus(0, FlightExecutionStatus.Status.OK));
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        assertNotEquals(
                new FlightExecutionStatus(0, FlightExecutionStatus.Status.OK),
                "string");
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        assertEquals(
                new FlightExecutionStatus(0, FlightExecutionStatus.Status.OK).hashCode(),
                new FlightExecutionStatus(0, FlightExecutionStatus.Status.OK).hashCode());
    }

    @Test
    void ensureToStringContainsFlightId() {
        final FlightExecutionStatus s =
                new FlightExecutionStatus(0, FlightExecutionStatus.Status.OK);
        assertTrue(s.toString().contains("0"));
    }

    @Test
    void ensureProtectedNoArgConstructorExistsForJpa() throws Exception {
        final Constructor<FlightExecutionStatus> ctor =
                FlightExecutionStatus.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}