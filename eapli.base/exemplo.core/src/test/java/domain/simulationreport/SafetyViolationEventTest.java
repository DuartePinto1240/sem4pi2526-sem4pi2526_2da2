// ─── domain/simulationreport/SafetyViolationEventTest.java ────────────────

package domain.simulationreport;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

class SafetyViolationEventTest {

    private static final SafetyViolationEvent EVENT = new SafetyViolationEvent(
            12.0, 0, 1,
            41.25, -8.68, 10000.0, 0.1, 0.2, 0.3,
            41.26, -8.69, 10000.0, 0.4, 0.5, 0.6);

    @Test
    void ensureValidEventIsCreated() {
        assertEquals(12.0, EVENT.timestamp());
        assertEquals(0,    EVENT.flightI());
        assertEquals(1,    EVENT.flightJ());
        assertEquals(41.25, EVENT.latI());
        assertEquals(-8.68, EVENT.lonI());
        assertEquals(10000.0, EVENT.altI());
        assertEquals(0.1,  EVENT.vxI());
        assertEquals(0.2,  EVENT.vyI());
        assertEquals(0.3,  EVENT.vzI());
        assertEquals(41.26, EVENT.latJ());
        assertEquals(-8.69, EVENT.lonJ());
        assertEquals(10000.0, EVENT.altJ());
        assertEquals(0.4,  EVENT.vxJ());
        assertEquals(0.5,  EVENT.vyJ());
        assertEquals(0.6,  EVENT.vzJ());
    }

    @Test
    void ensureNegativeTimestampIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new SafetyViolationEvent(-1.0, 0, 1,
                        0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0));
    }

    @Test
    void ensureTwoEventsWithSameValuesAreEqual() {
        final SafetyViolationEvent other = new SafetyViolationEvent(
                12.0, 0, 1,
                41.25, -8.68, 10000.0, 0.1, 0.2, 0.3,
                41.26, -8.69, 10000.0, 0.4, 0.5, 0.6);
        assertEquals(EVENT, other);
    }

    @Test
    void ensureTwoEventsWithDifferentTimestampAreNotEqual() {
        final SafetyViolationEvent other = new SafetyViolationEvent(
                5.0, 0, 1,
                41.25, -8.68, 10000.0, 0.1, 0.2, 0.3,
                41.26, -8.69, 10000.0, 0.4, 0.5, 0.6);
        assertNotEquals(EVENT, other);
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        assertEquals(EVENT, EVENT);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, EVENT);
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        assertNotEquals(EVENT, "string");
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final SafetyViolationEvent other = new SafetyViolationEvent(
                12.0, 0, 1,
                41.25, -8.68, 10000.0, 0.1, 0.2, 0.3,
                41.26, -8.69, 10000.0, 0.4, 0.5, 0.6);
        assertEquals(EVENT.hashCode(), other.hashCode());
    }

    @Test
    void ensureToStringIsNotBlank() {
        assertFalse(EVENT.toString().isBlank());
    }

    @Test
    void ensureProtectedNoArgConstructorExistsForJpa() throws Exception {
        final Constructor<SafetyViolationEvent> ctor =
                SafetyViolationEvent.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}