package domain.enginemodel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThrustTest {

    // ── constructor — invalid inputs ──────────────────────────────────────────

    @Test
    void ensureSeaLevelStoppedThrustCannotBeNegative() {
        // Arrange
        final double invalidStopped = -100.0;
        final double validCruise    = 121.4;
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new Thrust(invalidStopped, validCruise));
    }

    @Test
    void ensureSeaLevelStoppedThrustCannotBeZero() {
        // Arrange
        final double invalidStopped = 0.0;
        final double validCruise    = 121.4;
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new Thrust(invalidStopped, validCruise));
    }

    @Test
    void ensureSeaLevelCruiseThrustCannotBeNegative() {
        // Arrange
        final double validStopped  = 101.8;
        final double invalidCruise = -50.0;
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new Thrust(validStopped, invalidCruise));
    }

    @Test
    void ensureSeaLevelCruiseThrustCannotBeZero() {
        // Arrange
        final double validStopped  = 101.8;
        final double invalidCruise = 0.0;
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new Thrust(validStopped, invalidCruise));
    }

    @Test
    void ensureCruiseThrustMustBeGreaterThanStoppedThrust() {
        // Arrange — cruise < stopped, invalid
        final double stopped = 121.4;
        final double cruise  = 101.8;
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new Thrust(stopped, cruise));
    }

    @Test
    void ensureCruiseThrustEqualToStoppedThrustIsRejected() {
        // Arrange — cruise == stopped, invalid
        final double stopped = 101.8;
        final double cruise  = 101.8;
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new Thrust(stopped, cruise));
    }

    // ── constructor — valid ───────────────────────────────────────────────────

    @Test
    void ensureValidThrustIsCreatedSuccessfully() {
        // Arrange
        final double stopped = 101.8;
        final double cruise  = 121.4;
        // Act
        final Thrust thrust = new Thrust(stopped, cruise);
        // Assert
        assertEquals(stopped, thrust.seaLevelStopped());
        assertEquals(cruise,  thrust.seaLevelCruise());
    }

    // ── equals and hashCode ───────────────────────────────────────────────────

    @Test
    void ensureTwoThrustsWithSameValuesAreEqual() {
        // Arrange
        final Thrust t1 = new Thrust(101.8, 121.4);
        final Thrust t2 = new Thrust(101.8, 121.4);
        // Act + Assert
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void ensureTwoThrustsWithDifferentStoppedAreNotEqual() {
        // Arrange
        final Thrust t1 = new Thrust(101.8, 121.4);
        final Thrust t2 = new Thrust(110.0, 121.4);
        // Act + Assert
        assertNotEquals(t1, t2);
    }

    @Test
    void ensureTwoThrustsWithDifferentCruiseAreNotEqual() {
        // Arrange
        final Thrust t1 = new Thrust(101.8, 121.4);
        final Thrust t2 = new Thrust(101.8, 130.0);
        // Act + Assert
        assertNotEquals(t1, t2);
    }

    @Test
    void ensureThrustIsNotEqualToNull() {
        // Arrange
        final Thrust t = new Thrust(101.8, 121.4);
        // Act + Assert
        assertNotEquals(null, t);
    }

    @Test
    void ensureThrustIsNotEqualToOtherType() {
        // Arrange
        final Thrust t = new Thrust(101.8, 121.4);
        // Act + Assert
        assertNotEquals(t, "not a thrust");
    }

    @Test
    void ensureThrustIsEqualToItself() {
        // Arrange
        final Thrust t = new Thrust(101.8, 121.4);
        // Act + Assert
        assertEquals(t, t);
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    void ensureToStringContainsBothValues() {
        // Arrange
        final Thrust t = new Thrust(101.8, 121.4);
        // Act
        final String result = t.toString();
        // Assert
        assertTrue(result.contains("101.8") || result.contains("101,8"));
        assertTrue(result.contains("121.4") || result.contains("121,4"));
    }
}