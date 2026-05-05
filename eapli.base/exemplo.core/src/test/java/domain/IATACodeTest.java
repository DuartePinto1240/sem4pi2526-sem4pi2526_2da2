package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IATACode} value object (US052).
 *
 * <p>Covers: valid construction, normalisation, all invalid formats,
 * equality, hashCode, and toString.</p>
 */
class IATACodeTest {

    // =========================================================================
    // Valid construction and normalisation
    // =========================================================================

    @Test
    void ensureValidCodeIsStored() {
        assertEquals("LIS", new IATACode("LIS").code());
    }

    @Test
    void ensureLowerCaseInputIsNormalisedToUpperCase() {
        assertEquals("LIS", new IATACode("lis").code());
    }

    @Test
    void ensureMixedCaseInputIsNormalisedToUpperCase() {
        assertEquals("LIS", new IATACode("LiS").code());
    }

    @Test
    void ensureLeadingAndTrailingWhitespaceIsTrimmed() {
        assertEquals("LIS", new IATACode("  LIS  ").code());
    }

    // =========================================================================
    // Invalid formats
    // =========================================================================

    @Test
    void ensureNullIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new IATACode(null));
    }

    @Test
    void ensureBlankStringIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new IATACode("   "));
    }

    @Test
    void ensureTwoLetterCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new IATACode("LI"));
    }

    @Test
    void ensureFourLetterCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new IATACode("LISB"));
    }

    @Test
    void ensureCodeWithDigitIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new IATACode("L1S"));
    }

    @Test
    void ensureCodeWithSpecialCharIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new IATACode("L-S"));
    }

    @Test
    void ensureEmptyStringIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new IATACode(""));
    }

    // =========================================================================
    // Equality and hashCode
    // =========================================================================

    @Test
    void ensureTwoCodesWithSameValueAreEqual() {
        assertEquals(new IATACode("LIS"), new IATACode("LIS"));
    }

    @Test
    void ensureTwoCodesWithSameValueHaveSameHashCode() {
        assertEquals(new IATACode("LIS").hashCode(), new IATACode("LIS").hashCode());
    }

    @Test
    void ensureTwoDifferentCodesAreNotEqual() {
        assertNotEquals(new IATACode("LIS"), new IATACode("OPO"));
    }

    @Test
    void ensureCodeIsNotEqualToNull() {
        assertNotEquals(new IATACode("LIS"), null);
    }

    @Test
    void ensureCodeIsNotEqualToOtherType() {
        assertNotEquals(new IATACode("LIS"), "LIS");
    }

    @Test
    void ensureCodeIsEqualToItself() {
        final IATACode code = new IATACode("LIS");
        assertEquals(code, code);
    }

    // =========================================================================
    // toString
    // =========================================================================

    @Test
    void ensureToStringReturnsCode() {
        assertEquals("LIS", new IATACode("LIS").toString());
    }
}
