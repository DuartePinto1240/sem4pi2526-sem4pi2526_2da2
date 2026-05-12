package domain.aircraft;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OperationalStatusTest {

    @Test
    void ensureActiveStatusIsActive() {
        final OperationalStatus status = OperationalStatus.active();
        assertTrue(status.isActive());
        assertFalse(status.isDecommissioned());
    }

    @Test
    void ensureDecommissionedStatusIsDecommissioned() {
        final OperationalStatus status = OperationalStatus.decommissioned();
        assertFalse(status.isActive());
        assertTrue(status.isDecommissioned());
    }

    @Test
    void ensureStatusReturnsCorrectEnum() {
        assertEquals(OperationalStatus.Status.ACTIVE,
                OperationalStatus.active().status());
        assertEquals(OperationalStatus.Status.DECOMMISSIONED,
                OperationalStatus.decommissioned().status());
    }

    @Test
    void ensureToStringReturnsStatusName() {
        assertEquals("ACTIVE", OperationalStatus.active().toString());
        assertEquals("DECOMMISSIONED", OperationalStatus.decommissioned().toString());
    }

    @Test
    void ensureTwoActiveStatusesAreEqual() {
        assertEquals(OperationalStatus.active(), OperationalStatus.active());
        assertEquals(OperationalStatus.active().hashCode(),
                OperationalStatus.active().hashCode());
    }

    @Test
    void ensureActiveAndDecommissionedAreNotEqual() {
        assertNotEquals(OperationalStatus.active(),
                OperationalStatus.decommissioned());
    }
}
