package domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link AirControlAreaCode} value object (US050).
 *
 * <p>Follows the AAA (Arrange-Act-Assert) convention as required by NFR03.</p>
 */
public class AirControlAreaCodeTest {

    // =========================================================================
    // Valid construction
    // =========================================================================

    @Test
    public void ensureValidCodeIsCreated() {
        // Arrange & Act
        final AirControlAreaCode code = new AirControlAreaCode("LPPC");

        // Assert
        assertEquals("LPPC", code.code());
    }

    @Test
    public void ensureLowercaseCodeIsNormalisedToUppercase() {
        // Arrange & Act
        final AirControlAreaCode code = new AirControlAreaCode("lppc");

        // Assert — stored in uppercase
        assertEquals("LPPC", code.code());
    }

    @Test
    public void ensureCodeWithLeadingTrailingWhitespaceIsTrimmed() {
        // Arrange & Act
        final AirControlAreaCode code = new AirControlAreaCode("  LPPC  ");

        // Assert
        assertEquals("LPPC", code.code());
    }

    @Test
    public void ensureSingleCharacterCodeIsAccepted() {
        // Arrange & Act — no format restriction imposed by the professor
        final AirControlAreaCode code = new AirControlAreaCode("A");

        // Assert
        assertEquals("A", code.code());
    }

    // =========================================================================
    // Invalid construction
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void ensureNullCodeIsRejected() {
        new AirControlAreaCode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureEmptyCodeIsRejected() {
        new AirControlAreaCode("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureBlankCodeIsRejected() {
        new AirControlAreaCode("   ");
    }

    // =========================================================================
    // Equality, hashCode, compareTo
    // =========================================================================

    @Test
    public void ensureTwoCodesWithSameValueAreEqual() {
        // Arrange
        final AirControlAreaCode c1 = new AirControlAreaCode("LPPC");
        final AirControlAreaCode c2 = new AirControlAreaCode("LPPC");

        // Assert
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void ensureEqualityIsCaseInsensitiveDueToNormalisation() {
        // Arrange — lowercase and uppercase both normalise to "LPPC"
        final AirControlAreaCode c1 = new AirControlAreaCode("LPPC");
        final AirControlAreaCode c2 = new AirControlAreaCode("lppc");

        // Assert
        assertEquals(c1, c2);
    }

    @Test
    public void ensureTwoCodesWithDifferentValuesAreNotEqual() {
        // Arrange
        final AirControlAreaCode c1 = new AirControlAreaCode("LPPC");
        final AirControlAreaCode c2 = new AirControlAreaCode("LECM");

        // Assert
        assertNotEquals(c1, c2);
    }

    @Test
    public void ensureCodeDoesNotEqualNull() {
        // Arrange
        final AirControlAreaCode code = new AirControlAreaCode("LPPC");

        // Assert
        assertNotEquals(null, code);
    }

    @Test
    public void ensureCodeEqualsItself() {
        // Arrange
        final AirControlAreaCode code = new AirControlAreaCode("LPPC");

        // Assert
        assertEquals(code, code);
    }

    @Test
    public void ensureCompareToReturnsZeroForEqualCodes() {
        // Arrange
        final AirControlAreaCode c1 = new AirControlAreaCode("LPPC");
        final AirControlAreaCode c2 = new AirControlAreaCode("LPPC");

        // Assert
        assertEquals(0, c1.compareTo(c2));
    }

    @Test
    public void ensureCompareToReturnsNonZeroForDifferentCodes() {
        // Arrange
        final AirControlAreaCode c1 = new AirControlAreaCode("LECM");
        final AirControlAreaCode c2 = new AirControlAreaCode("LPPC");

        // Assert
        assertNotEquals(0, c1.compareTo(c2));
    }

    // =========================================================================
    // toString
    // =========================================================================

    @Test
    public void ensureToStringReturnsTheCode() {
        // Arrange
        final AirControlAreaCode code = new AirControlAreaCode("LPPC");

        // Assert
        assertEquals("LPPC", code.toString());
    }
}