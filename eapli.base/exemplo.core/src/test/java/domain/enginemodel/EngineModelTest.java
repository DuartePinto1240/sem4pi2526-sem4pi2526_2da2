package domain.enginemodel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EngineModelTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private Thrust validThrust() {
        return new Thrust(101.8, 121.4);
    }

    private TSFC validTsfc() {
        return new TSFC(0.0157);
    }

    private EngineModel validEngineModel() {
        return new EngineModel(
                "LEAP-1B", "CFM International",
                MotorizationType.TURBOFAN,
                validThrust(), validTsfc());
    }

    // ── name ─────────────────────────────────────────────────────────────────

    @Test
    void ensureNameCannotBeNull() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(
                        null, "CFM International",
                        MotorizationType.TURBOFAN,
                        validThrust(), validTsfc()));
    }

    @Test
    void ensureNameCannotBeEmpty() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(
                        "", "CFM International",
                        MotorizationType.TURBOFAN,
                        validThrust(), validTsfc()));
    }

    // ── manufacturer ─────────────────────────────────────────────────────────

    @Test
    void ensureManufacturerCannotBeNull() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(
                        "LEAP-1B", null,
                        MotorizationType.TURBOFAN,
                        validThrust(), validTsfc()));
    }

    @Test
    void ensureManufacturerCannotBeEmpty() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(
                        "LEAP-1B", "",
                        MotorizationType.TURBOFAN,
                        validThrust(), validTsfc()));
    }

    // ── motorization type ────────────────────────────────────────────────────

    @Test
    void ensureMotorizationTypeCannotBeNull() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(
                        "LEAP-1B", "CFM International",
                        null,
                        validThrust(), validTsfc()));
    }

    // ── thrust ───────────────────────────────────────────────────────────────

    @Test
    void ensureThrustCannotBeNull() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(
                        "LEAP-1B", "CFM International",
                        MotorizationType.TURBOFAN,
                        null, validTsfc()));
    }

    // ── tsfc ─────────────────────────────────────────────────────────────────

    @Test
    void ensureTsfcCannotBeNull() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(
                        "LEAP-1B", "CFM International",
                        MotorizationType.TURBOFAN,
                        validThrust(), null));
    }

    // ── successful creation ───────────────────────────────────────────────────

    @Test
    void ensureValidEngineModelIsCreatedSuccessfully() {
        // Arrange
        final String name           = "LEAP-1B";
        final String manufacturer   = "CFM International";
        final MotorizationType type = MotorizationType.TURBOFAN;
        final Thrust thrust         = validThrust();
        final TSFC tsfc             = validTsfc();
        // Act
        final EngineModel model = new EngineModel(
                name, manufacturer, type, thrust, tsfc);
        // Assert
        assertEquals(name,         model.name());
        assertEquals(manufacturer, model.manufacturer());
        assertEquals(type,         model.motorizationType());
        assertEquals(thrust,       model.thrust());
        assertEquals(tsfc,         model.tsfc());
    }

    @Test
    void ensureAllMotorizationTypesAreAccepted() {
        // Arrange + Act + Assert — one model per type
        for (final MotorizationType type : MotorizationType.values()) {
            assertDoesNotThrow(() -> new EngineModel(
                    "Engine-" + type, "Manufacturer",
                    type, validThrust(), validTsfc()));
        }
    }

    // ── identity ─────────────────────────────────────────────────────────────

    @Test
    void ensureIdentityIsNullBeforePersistence() {
        // Arrange
        final EngineModel model = validEngineModel();
        // Act + Assert — id is assigned by DB, null in memory
        assertNull(model.identity());
    }

    // ── sameAs ───────────────────────────────────────────────────────────────

    @Test
    void ensureSameAsReturnsTrueForSameNameAndManufacturer() {
        // Arrange
        final EngineModel m1 = validEngineModel();
        final EngineModel m2 = validEngineModel();
        // Act + Assert
        assertTrue(m1.sameAs(m2));
    }

    @Test
    void ensureSameAsIsCaseInsensitive() {
        // Arrange
        final EngineModel m1 = new EngineModel(
                "LEAP-1B", "CFM International",
                MotorizationType.TURBOFAN, validThrust(), validTsfc());
        final EngineModel m2 = new EngineModel(
                "leap-1b", "cfm international",
                MotorizationType.TURBOFAN, validThrust(), validTsfc());
        // Act + Assert
        assertTrue(m1.sameAs(m2));
    }

    @Test
    void ensureSameAsReturnsFalseForDifferentName() {
        // Arrange
        final EngineModel m1 = new EngineModel(
                "LEAP-1B", "CFM International",
                MotorizationType.TURBOFAN, validThrust(), validTsfc());
        final EngineModel m2 = new EngineModel(
                "Trent 900", "CFM International",
                MotorizationType.TURBOFAN, validThrust(), validTsfc());
        // Act + Assert
        assertFalse(m1.sameAs(m2));
    }

    @Test
    void ensureSameAsReturnsFalseForDifferentManufacturer() {
        // Arrange
        final EngineModel m1 = new EngineModel(
                "LEAP-1B", "CFM International",
                MotorizationType.TURBOFAN, validThrust(), validTsfc());
        final EngineModel m2 = new EngineModel(
                "LEAP-1B", "Other Manufacturer",
                MotorizationType.TURBOFAN, validThrust(), validTsfc());
        // Act + Assert
        assertFalse(m1.sameAs(m2));
    }

    @Test
    void ensureSameAsReturnsFalseForNonEngineModelObject() {
        // Arrange
        final EngineModel m = validEngineModel();
        // Act + Assert
        assertFalse(m.sameAs("not an engine model"));
    }

    @Test
    void ensureSameAsReturnsFalseForNull() {
        // Arrange
        final EngineModel m = validEngineModel();
        // Act + Assert
        assertFalse(m.sameAs(null));
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    void ensureToStringContainsNameManufacturerAndType() {
        // Arrange
        final EngineModel model = validEngineModel();
        // Act
        final String result = model.toString();
        // Assert
        assertTrue(result.contains("LEAP-1B"));
        assertTrue(result.contains("CFM International"));
        assertTrue(result.contains("TURBOFAN"));
    }
}