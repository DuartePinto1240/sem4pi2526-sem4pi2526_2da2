package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AirportName} value object (US052).
 */
class AirportNameTest {

    // =========================================================================
    // Valid construction
    // =========================================================================

    @Test
    void ensureValidNameIsStored() {
        assertEquals("Humberto Delgado Airport",
                new AirportName("Humberto Delgado Airport").value());
    }

    @Test
    void ensureNameIsTrimmed() {
        assertEquals("Humberto Delgado Airport",
                new AirportName("  Humberto Delgado Airport  ").value());
    }

    @Test
    void ensureSingleWordNameIsAccepted() {
        assertEquals("Faro", new AirportName("Faro").value());
    }

    // =========================================================================
    // Invalid inputs
    // =========================================================================

    @Test
    void ensureNullIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AirportName(null));
    }

    @Test
    void ensureBlankStringIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AirportName("   "));
    }

    @Test
    void ensureEmptyStringIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AirportName(""));
    }

    // =========================================================================
    // Equality and hashCode
    // =========================================================================

    @Test
    void ensureTwoNamesWithSameValueAreEqual() {
        assertEquals(new AirportName("Faro Airport"), new AirportName("Faro Airport"));
    }

    @Test
    void ensureTwoNamesWithSameValueHaveSameHashCode() {
        assertEquals(
                new AirportName("Faro Airport").hashCode(),
                new AirportName("Faro Airport").hashCode());
    }

    @Test
    void ensureTwoDifferentNamesAreNotEqual() {
        assertNotEquals(new AirportName("Faro Airport"), new AirportName("Lisbon Airport"));
    }

    @Test
    void ensureNameIsNotEqualToNull() {
        assertNotEquals(new AirportName("Faro Airport"), null);
    }

    @Test
    void ensureNameIsNotEqualToOtherType() {
        assertNotEquals(new AirportName("Faro Airport"), "Faro Airport");
    }

    @Test
    void ensureNameIsEqualToItself() {
        final AirportName n = new AirportName("Faro Airport");
        assertEquals(n, n);
    }

    // =========================================================================
    // toString
    // =========================================================================

    @Test
    void ensureToStringReturnsName() {
        assertEquals("Faro Airport", new AirportName("Faro Airport").toString());
    }
}
