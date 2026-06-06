// ─── domain/simulationreport/SimulationReportIdTest.java ──────────────────

package domain.simulationreport;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

class SimulationReportIdTest {

    @Test
    void ensureValidIdIsCreated() {
        final SimulationReportId id = SimulationReportId.of("RPT-001");
        assertEquals("RPT-001", id.value());
    }

    @Test
    void ensureEmptyIdIsRejected() {
        assertThrows(Exception.class, () -> SimulationReportId.of(""));
    }

    @Test
    void ensureNullIdIsRejected() {
        assertThrows(Exception.class, () -> SimulationReportId.of(null));
    }

    @Test
    void ensureTwoIdsWithSameValueAreEqual() {
        assertEquals(SimulationReportId.of("RPT-001"),
                SimulationReportId.of("RPT-001"));
    }

    @Test
    void ensureTwoIdsWithDifferentValuesAreNotEqual() {
        assertNotEquals(SimulationReportId.of("RPT-001"),
                SimulationReportId.of("RPT-002"));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final SimulationReportId id = SimulationReportId.of("RPT-001");
        assertEquals(id, id);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, SimulationReportId.of("RPT-001"));
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        assertNotEquals(SimulationReportId.of("RPT-001"), "RPT-001");
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        assertEquals(SimulationReportId.of("RPT-001").hashCode(),
                SimulationReportId.of("RPT-001").hashCode());
    }

    @Test
    void ensureCompareToReturnsZeroForEqual() {
        assertEquals(0, SimulationReportId.of("RPT-001")
                .compareTo(SimulationReportId.of("RPT-001")));
    }

    @Test
    void ensureCompareToReturnsNegativeForSmaller() {
        assertTrue(SimulationReportId.of("RPT-001")
                .compareTo(SimulationReportId.of("RPT-002")) < 0);
    }

    @Test
    void ensureCompareToReturnsPositiveForLarger() {
        assertTrue(SimulationReportId.of("RPT-002")
                .compareTo(SimulationReportId.of("RPT-001")) > 0);
    }

    @Test
    void ensureToStringReturnsValue() {
        assertEquals("RPT-001", SimulationReportId.of("RPT-001").toString());
    }

    @Test
    void ensureProtectedNoArgConstructorExistsForJpa() throws Exception {
        final Constructor<SimulationReportId> ctor =
                SimulationReportId.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}