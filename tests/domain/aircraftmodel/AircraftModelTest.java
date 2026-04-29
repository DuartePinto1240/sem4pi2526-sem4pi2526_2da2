package domain.aircraftmodel;

import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AircraftModelTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private EngineModel turbofanEngine() {
        return new EngineModel("LEAP-1B", "CFM International",
                MotorizationType.TURBOFAN,
                new Thrust(101.8, 121.4), new TSFC(0.0157));
    }

    private EngineModel anotherTurbofanEngine() {
        return new EngineModel("Trent 900", "Rolls-Royce",
                MotorizationType.TURBOFAN,
                new Thrust(311.0, 340.0), new TSFC(0.0152));
    }

    private EngineModel turbopropEngine() {
        return new EngineModel("PW127", "Pratt & Whitney Canada",
                MotorizationType.TURBOPROP,
                new Thrust(15.0, 20.5), new TSFC(0.0300));
    }

    private FlightCharacteristics validCharacteristics() {
        return new FlightCharacteristics(
                41413.0, 65317.0, 79016.0, 26020.0,
                12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45);
    }

    private AircraftModel validAircraftModel() {
        return new AircraftModel(
                "737 MAX", "Boeing",
                AircraftType.PASSENGER,
                validCharacteristics(),
                List.of(turbofanEngine()));
    }

    // ── modelName ─────────────────────────────────────────────────────────────

    @Test
    void ensureModelNameCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftModel(null, "Boeing",
                        AircraftType.PASSENGER, validCharacteristics(),
                        List.of(turbofanEngine())));
    }

    @Test
    void ensureModelNameCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftModel("", "Boeing",
                        AircraftType.PASSENGER, validCharacteristics(),
                        List.of(turbofanEngine())));
    }

    // ── manufacturer ─────────────────────────────────────────────────────────

    @Test
    void ensureManufacturerCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftModel("737 MAX", null,
                        AircraftType.PASSENGER, validCharacteristics(),
                        List.of(turbofanEngine())));
    }

    @Test
    void ensureManufacturerCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftModel("737 MAX", "",
                        AircraftType.PASSENGER, validCharacteristics(),
                        List.of(turbofanEngine())));
    }

    // ── type ─────────────────────────────────────────────────────────────────

    @Test
    void ensureTypeCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftModel("737 MAX", "Boeing",
                        null, validCharacteristics(),
                        List.of(turbofanEngine())));
    }

    // ── characteristics ───────────────────────────────────────────────────────

    @Test
    void ensureCharacteristicsCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftModel("737 MAX", "Boeing",
                        AircraftType.PASSENGER, null,
                        List.of(turbofanEngine())));
    }

    // ── engines ───────────────────────────────────────────────────────────────

    @Test
    void ensureEngineListCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftModel("737 MAX", "Boeing",
                        AircraftType.PASSENGER, validCharacteristics(), null));
    }

    @Test
    void ensureEngineListCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftModel("737 MAX", "Boeing",
                        AircraftType.PASSENGER, validCharacteristics(),
                        List.of()));
    }

    @Test
    void ensureMixedEngineTypesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftModel("737 MAX", "Boeing",
                        AircraftType.PASSENGER, validCharacteristics(),
                        List.of(turbofanEngine(), turbopropEngine())));
    }

    // ── valid creation ────────────────────────────────────────────────────────

    @Test
    void ensureValidAircraftModelIsCreatedSuccessfully() {
        // Arrange + Act
        final AircraftModel model = validAircraftModel();
        // Assert
        assertEquals("737 MAX",              model.modelName());
        assertEquals("Boeing",               model.manufacturer());
        assertEquals(AircraftType.PASSENGER, model.type());
        assertNotNull(model.characteristics());
        assertEquals(1, model.certifiedEngines().size());
    }

    @Test
    void ensureAllAircraftTypesAreAccepted() {
        for (final AircraftType type : AircraftType.values()) {
            assertDoesNotThrow(() -> new AircraftModel(
                    "Model-" + type, "Boeing",
                    type, validCharacteristics(), List.of(turbofanEngine())));
        }
    }

    // ── addCertifiedEngine ────────────────────────────────────────────────────

    @Test
    void ensureCompatibleEngineCanBeAdded() {
        // Arrange
        final AircraftModel model = validAircraftModel();
        final EngineModel engine2 = anotherTurbofanEngine();
        // Act
        model.addCertifiedEngine(engine2);
        // Assert
        assertEquals(2, model.certifiedEngines().size());
        assertTrue(model.certifiedEngines().contains(engine2));
    }

    @Test
    void ensureIncompatibleEngineTypeIsRejected() {
        // Arrange
        final AircraftModel model = validAircraftModel();
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> model.addCertifiedEngine(turbopropEngine()));
    }

    @Test
    void ensureDuplicateEngineCannotBeAdded() {
        // Arrange
        final EngineModel engine = turbofanEngine();
        final AircraftModel model = new AircraftModel(
                "737 MAX", "Boeing", AircraftType.PASSENGER,
                validCharacteristics(), List.of(engine));
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> model.addCertifiedEngine(engine));
    }

    @Test
    void ensureNullEngineCannotBeAdded() {
        assertThrows(IllegalArgumentException.class,
                () -> validAircraftModel().addCertifiedEngine(null));
    }

    // ── removeCertifiedEngine ─────────────────────────────────────────────────

    @Test
    void ensureEngineCanBeRemovedWhenMoreThanOneExists() {
        // Arrange
        final EngineModel engine1 = turbofanEngine();
        final EngineModel engine2 = anotherTurbofanEngine();
        final AircraftModel model = new AircraftModel(
                "737 MAX", "Boeing", AircraftType.PASSENGER,
                validCharacteristics(), List.of(engine1, engine2));
        // Act
        model.removeCertifiedEngine(engine1);
        // Assert
        assertEquals(1, model.certifiedEngines().size());
        assertFalse(model.certifiedEngines().contains(engine1));
    }

    @Test
    void ensureLastEngineCannotBeRemoved() {
        // Arrange
        final EngineModel engine = turbofanEngine();
        final AircraftModel model = new AircraftModel(
                "737 MAX", "Boeing", AircraftType.PASSENGER,
                validCharacteristics(), List.of(engine));
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> model.removeCertifiedEngine(engine));
    }

    @Test
    void ensureNonCertifiedEngineCannotBeRemoved() {
        // Arrange
        final AircraftModel model = validAircraftModel();
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> model.removeCertifiedEngine(anotherTurbofanEngine()));
    }

    @Test
    void ensureNullEngineCannotBeRemoved() {
        assertThrows(IllegalArgumentException.class,
                () -> validAircraftModel().removeCertifiedEngine(null));
    }

    // ── identity ─────────────────────────────────────────────────────────────

    @Test
    void ensureIdentityIsNullBeforePersistence() {
        assertNull(validAircraftModel().identity());
    }

    // ── sameAs ───────────────────────────────────────────────────────────────

    @Test
    void ensureSameAsReturnsTrueForSameNameAndManufacturer() {
        final AircraftModel m1 = validAircraftModel();
        final AircraftModel m2 = validAircraftModel();
        assertTrue(m1.sameAs(m2));
    }

    @Test
    void ensureSameAsIsCaseInsensitive() {
        final AircraftModel m1 = new AircraftModel("737 MAX", "Boeing",
                AircraftType.PASSENGER, validCharacteristics(), List.of(turbofanEngine()));
        final AircraftModel m2 = new AircraftModel("737 max", "boeing",
                AircraftType.PASSENGER, validCharacteristics(), List.of(turbofanEngine()));
        assertTrue(m1.sameAs(m2));
    }

    @Test
    void ensureSameAsReturnsFalseForDifferentName() {
        final AircraftModel m1 = validAircraftModel();
        final AircraftModel m2 = new AircraftModel("A320", "Boeing",
                AircraftType.PASSENGER, validCharacteristics(), List.of(turbofanEngine()));
        assertFalse(m1.sameAs(m2));
    }

    @Test
    void ensureSameAsReturnsFalseForDifferentManufacturer() {
        final AircraftModel m1 = validAircraftModel();
        final AircraftModel m2 = new AircraftModel("737 MAX", "Airbus",
                AircraftType.PASSENGER, validCharacteristics(), List.of(turbofanEngine()));
        assertFalse(m1.sameAs(m2));
    }

    @Test
    void ensureSameAsReturnsFalseForNull() {
        assertFalse(validAircraftModel().sameAs(null));
    }

    @Test
    void ensureSameAsReturnsFalseForOtherType() {
        assertFalse(validAircraftModel().sameAs("not an aircraft model"));
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    void ensureToStringContainsNameManufacturerAndType() {
        final String result = validAircraftModel().toString();
        assertTrue(result.contains("737 MAX"));
        assertTrue(result.contains("Boeing"));
        assertTrue(result.contains("PASSENGER"));
    }
}