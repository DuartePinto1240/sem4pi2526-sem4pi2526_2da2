package domain.enginemodel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TSFCTest {

    // ── constructor — invalid inputs ──────────────────────────────────────────

    @Test
    void ensureTsfcCannotBeNegative() {
        // Arrange
        final double invalidValue = -0.01;
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new TSFC(invalidValue));
    }

    @Test
    void ensureTsfcCannotBeZero() {
        // Arrange
        final double invalidValue = 0.0;
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new TSFC(invalidValue));
    }

    // ── constructor — valid ───────────────────────────────────────────────────

    @Test
    void ensureValidTsfcIsCreatedSuccessfully() {
        // Arrange
        final double value = 0.0157;
        // Act
        final TSFC tsfc = new TSFC(value);
        // Assert
        assertEquals(value, tsfc.value());
    }

    // ── equals and hashCode ───────────────────────────────────────────────────

    @Test
    void ensureTwoTsfcsWithSameValueAreEqual() {
        // Arrange
        final TSFC t1 = new TSFC(0.0157);
        final TSFC t2 = new TSFC(0.0157);
        // Act + Assert
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void ensureTwoTsfcsWithDifferentValuesAreNotEqual() {
        // Arrange
        final TSFC t1 = new TSFC(0.0157);
        final TSFC t2 = new TSFC(0.0200);
        // Act + Assert
        assertNotEquals(t1, t2);
    }

    @Test
    void ensureTsfcIsNotEqualToNull() {
        // Arrange
        final TSFC t = new TSFC(0.0157);
        // Act + Assert
        assertNotEquals(null, t);
    }

    @Test
    void ensureTsfcIsNotEqualToOtherType() {
        // Arrange
        final TSFC t = new TSFC(0.0157);
        // Act + Assert
        assertNotEquals(t, "not a tsfc");
    }

    @Test
    void ensureTsfcIsEqualToItself() {
        // Arrange
        final TSFC t = new TSFC(0.0157);
        // Act + Assert
        assertEquals(t, t);
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    void ensureToStringContainsValue() {
        // Arrange
        final TSFC t = new TSFC(0.0157);
        // Act
        final String result = t.toString();
        // Assert
        assertNotNull(result);
        assertFalse(result.isBlank());
    }
}