// ─── domain/flightroute/FlightTypeTest.java ────────────────────────────────

package domain.flightroute;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightTypeTest {

    @Test
    void ensureRegularIsRegular() {
        assertTrue(FlightType.regular().isRegular());
        assertFalse(FlightType.regular().isCharter());
    }

    @Test
    void ensureCharterIsCharter() {
        assertTrue(FlightType.charter().isCharter());
        assertFalse(FlightType.charter().isRegular());
    }

    @Test
    void ensureTwoRegularsAreEqual() {
        assertEquals(FlightType.regular(), FlightType.regular());
    }

    @Test
    void ensureTwoChartersAreEqual() {
        assertEquals(FlightType.charter(), FlightType.charter());
    }

    @Test
    void ensureRegularAndCharterAreNotEqual() {
        assertNotEquals(FlightType.regular(), FlightType.charter());
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final FlightType ft = FlightType.regular();
        assertEquals(ft, ft);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, FlightType.regular());
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        assertNotEquals(FlightType.regular(), "REGULAR");
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        assertEquals(FlightType.regular().hashCode(), FlightType.regular().hashCode());
    }

    @Test
    void ensureToStringReturnsRegular() {
        assertEquals("REGULAR", FlightType.regular().toString());
    }

    @Test
    void ensureToStringReturnsCharter() {
        assertEquals("CHARTER", FlightType.charter().toString());
    }
}