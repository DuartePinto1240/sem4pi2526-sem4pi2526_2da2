package lprog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private FlightLeg validLeg() {
        return new FlightLeg("OPO", "2026-05-17", "14:00",
                "LIS", "15:00", 5000.0, "kg");
    }

    private FlightPlan validFlightPlan() {
        return new FlightPlan("TP001", "regular", List.of(validLeg()));
    }

    // ── flightId ──────────────────────────────────────────────────────────────

    @Test
    void ensureFlightIdCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(null, "regular", List.of(validLeg())));
    }

    @Test
    void ensureFlightIdCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan("", "regular", List.of(validLeg())));
    }

    // ── flightType ────────────────────────────────────────────────────────────

    @Test
    void ensureFlightTypeCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan("TP001", null, List.of(validLeg())));
    }

    @Test
    void ensureFlightTypeCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan("TP001", "", List.of(validLeg())));
    }

    // ── legs ─────────────────────────────────────────────────────────────────

    @Test
    void ensureLegsCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan("TP001", "regular", null));
    }

    @Test
    void ensureLegsCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan("TP001", "regular", List.of()));
    }

    // ── valid creation ────────────────────────────────────────────────────────

    @Test
    void ensureValidFlightPlanIsCreatedWithDraftStatus() {
        // Arrange + Act
        final FlightPlan fp = validFlightPlan();
        // Assert
        assertEquals("TP001",              fp.flightId());
        assertEquals("regular",            fp.flightType());
        assertEquals(FlightPlanStatus.DRAFT, fp.status());
        assertEquals(1,                    fp.legs().size());
        assertNotNull(fp.importedAt());
    }

    @Test
    void ensureFlightTypeIsStoredLowerCase() {
        // Arrange + Act
        final FlightPlan fp = new FlightPlan("TP001", "REGULAR", List.of(validLeg()));
        // Assert
        assertEquals("regular", fp.flightType());
    }

    @Test
    void ensureMultipleLegsAreAccepted() {
        // Arrange
        final FlightLeg leg1 = new FlightLeg("OPO", "2026-05-17", "08:00",
                "LIS", "09:00", 5000.0, "kg");
        final FlightLeg leg2 = new FlightLeg("LIS", "2026-05-17", "10:00",
                "FAO", "11:00", 3000.0, "kg");
        // Act
        final FlightPlan fp = new FlightPlan("TP002", "charter", List.of(leg1, leg2));
        // Assert
        assertEquals(2, fp.legs().size());
    }

    @Test
    void ensureLegsListIsUnmodifiable() {
        // Arrange
        final FlightPlan fp = validFlightPlan();
        // Act + Assert
        assertThrows(UnsupportedOperationException.class,
                () -> fp.legs().add(validLeg()));
    }

    // ── identity ─────────────────────────────────────────────────────────────

    @Test
    void ensureIdentityIsNullBeforePersistence() {
        assertNull(validFlightPlan().identity());
    }

    // ── sameAs ───────────────────────────────────────────────────────────────

    @Test
    void ensureSameAsReturnsTrueForSameFlightId() {
        final FlightPlan fp1 = validFlightPlan();
        final FlightPlan fp2 = new FlightPlan("TP001", "charter", List.of(validLeg()));
        assertTrue(fp1.sameAs(fp2));
    }

    @Test
    void ensureSameAsIsCaseInsensitive() {
        final FlightPlan fp1 = new FlightPlan("TP001", "regular", List.of(validLeg()));
        final FlightPlan fp2 = new FlightPlan("tp001", "regular", List.of(validLeg()));
        assertTrue(fp1.sameAs(fp2));
    }

    @Test
    void ensureSameAsReturnsFalseForDifferentFlightId() {
        final FlightPlan fp1 = new FlightPlan("TP001", "regular", List.of(validLeg()));
        final FlightPlan fp2 = new FlightPlan("TP002", "regular", List.of(validLeg()));
        assertFalse(fp1.sameAs(fp2));
    }

    @Test
    void ensureSameAsReturnsFalseForNull() {
        assertFalse(validFlightPlan().sameAs(null));
    }

    @Test
    void ensureSameAsReturnsFalseForOtherType() {
        assertFalse(validFlightPlan().sameAs("not a flight plan"));
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    void ensureToStringContainsKeyFields() {
        final String result = validFlightPlan().toString();
        assertTrue(result.contains("TP001"));
        assertTrue(result.contains("regular"));
        assertTrue(result.contains("DRAFT"));
    }
}