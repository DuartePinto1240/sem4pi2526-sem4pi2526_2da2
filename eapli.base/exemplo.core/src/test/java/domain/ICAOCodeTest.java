package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ICAOCode} value object (US052).
 *
 * <p>Covers: valid construction, normalisation, all invalid formats,
 * equality, hashCode, compareTo, and toString.</p>
 */
class ICAOCodeTest {

    // Valid construction and normalisation

    @Test
    void ensureValidCodeIsStored() {
        assertEquals("LPPT", new ICAOCode("LPPT").code());
    }

    @Test
    void ensureLowerCaseInputIsNormalisedToUpperCase() {
        assertEquals("LPPT", new ICAOCode("lppt").code());
    }

    @Test
    void ensureMixedCaseInputIsNormalisedToUpperCase() {
        assertEquals("LPPT", new ICAOCode("LpPt").code());
    }

    @Test
    void ensureLeadingAndTrailingWhitespaceIsTrimmed() {
        assertEquals("LPPT", new ICAOCode("  LPPT  ").code());
    }

    // Invalid formats

    @Test
    void ensureNullIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ICAOCode(null));
    }

    @Test
    void ensureBlankStringIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ICAOCode("   "));
    }

    @Test
    void ensureThreeLetterCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ICAOCode("LPP"));
    }

    @Test
    void ensureFiveLetterCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ICAOCode("LPPTP"));
    }

    @Test
    void ensureCodeWithDigitIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ICAOCode("LP1T"));
    }

    @Test
    void ensureCodeWithSpecialCharIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ICAOCode("LP-T"));
    }

    @Test
    void ensureEmptyStringIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ICAOCode(""));
    }

    // Equality and hashCode

    @Test
    void ensureTwoCodesWithSameValueAreEqual() {
        assertEquals(new ICAOCode("LPPT"), new ICAOCode("LPPT"));
    }

    @Test
    void ensureTwoCodesWithSameValueHaveSameHashCode() {
        assertEquals(new ICAOCode("LPPT").hashCode(), new ICAOCode("LPPT").hashCode());
    }

    @Test
    void ensureTwoDifferentCodesAreNotEqual() {
        assertNotEquals(new ICAOCode("LPPT"), new ICAOCode("LPPR"));
    }

    @Test
    void ensureCodeIsNotEqualToNull() {
        assertNotEquals(new ICAOCode("LPPT"), null);
    }

    @Test
    void ensureCodeIsNotEqualToOtherType() {
        assertNotEquals(new ICAOCode("LPPT"), "LPPT");
    }

    @Test
    void ensureCodeIsEqualToItself() {
        final ICAOCode code = new ICAOCode("LPPT");
        assertEquals(code, code);
    }

    // compareTo

    @Test
    void ensureCompareToReturnsZeroForEqualCodes() {
        assertEquals(0, new ICAOCode("LPPT").compareTo(new ICAOCode("LPPT")));
    }

    @Test
    void ensureCompareToReturnsNegativeWhenLessThan() {
        assertTrue(new ICAOCode("EHAM").compareTo(new ICAOCode("LPPT")) < 0);
    }

    @Test
    void ensureCompareToReturnsPositiveWhenGreaterThan() {
        assertTrue(new ICAOCode("LPPT").compareTo(new ICAOCode("EHAM")) > 0);
    }

    // toString

    @Test
    void ensureToStringReturnsCode() {
        assertEquals("LPPT", new ICAOCode("LPPT").toString());
    }
}
