package controller;

import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import persistence.impl.inmemory.InMemoryEngineModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreateEngineModelControllerTest {

    private CreateEngineModelController ctrl;

    @BeforeEach
    void setUp() {
        // Uses package-private constructor to inject in-memory repository
        ctrl = new CreateEngineModelController(new InMemoryEngineModelRepository());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private EngineModel registerLeap1B() {
        return ctrl.createEngineModel(
                "LEAP-1B", "CFM International",
                MotorizationType.TURBOFAN,
                101.8, 121.4, 0.0157);
    }

    // ── uniqueness ────────────────────────────────────────────────────────────

    @Test
    void ensureDuplicateNameAndManufacturerIsRejected() {
        // Arrange
        registerLeap1B();
        // Act + Assert
        assertThrows(IllegalArgumentException.class, this::registerLeap1B);
    }

    @Test
    void ensureSameNameDifferentManufacturerIsAllowed() {
        // Arrange
        registerLeap1B();
        // Act + Assert
        assertDoesNotThrow(() -> ctrl.createEngineModel(
                "LEAP-1B", "Other Manufacturer",
                MotorizationType.TURBOFAN,
                101.8, 121.4, 0.0157));
    }

    @Test
    void ensureSameManufacturerDifferentNameIsAllowed() {
        // Arrange
        registerLeap1B();
        // Act + Assert
        assertDoesNotThrow(() -> ctrl.createEngineModel(
                "LEAP-1A", "CFM International",
                MotorizationType.TURBOFAN,
                101.8, 121.4, 0.0157));
    }

    // ── motorization type ────────────────────────────────────────────────────

    @Test
    void ensureNullMotorizationTypeIsRejected() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.createEngineModel(
                        "LEAP-1B", "CFM International",
                        null,
                        101.8, 121.4, 0.0157));
    }

    // ── thrust ───────────────────────────────────────────────────────────────

    @Test
    void ensureNegativeStoppedThrustIsRejected() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.createEngineModel(
                        "LEAP-1B", "CFM International",
                        MotorizationType.TURBOFAN,
                        -101.8, 121.4, 0.0157));
    }

    @Test
    void ensureNegativeCruiseThrustIsRejected() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.createEngineModel(
                        "LEAP-1B", "CFM International",
                        MotorizationType.TURBOFAN,
                        101.8, -121.4, 0.0157));
    }

    @Test
    void ensureCruiseThrustLowerThanStoppedThrustIsRejected() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.createEngineModel(
                        "LEAP-1B", "CFM International",
                        MotorizationType.TURBOFAN,
                        121.4, 101.8, 0.0157));
    }

    // ── tsfc ─────────────────────────────────────────────────────────────────

    @Test
    void ensureNegativeTsfcIsRejected() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.createEngineModel(
                        "LEAP-1B", "CFM International",
                        MotorizationType.TURBOFAN,
                        101.8, 121.4, -0.0157));
    }

    @Test
    void ensureZeroTsfcIsRejected() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.createEngineModel(
                        "LEAP-1B", "CFM International",
                        MotorizationType.TURBOFAN,
                        101.8, 121.4, 0.0));
    }

    // ── happy path ───────────────────────────────────────────────────────────

    @Test
    void ensureValidEngineModelIsSavedAndReturned() {
        // Arrange + Act
        final EngineModel saved = registerLeap1B();
        // Assert
        assertNotNull(saved);
        assertEquals("LEAP-1B",                saved.name());
        assertEquals("CFM International",      saved.manufacturer());
        assertEquals(MotorizationType.TURBOFAN, saved.motorizationType());
        assertEquals(101.8, saved.thrust().seaLevelStopped());
        assertEquals(121.4, saved.thrust().seaLevelCruise());
        assertEquals(0.0157, saved.tsfc().value());
    }

    @Test
    void ensureMultipleDifferentEngineModelsCanBeRegistered() {
        // Arrange + Act + Assert
        assertDoesNotThrow(() -> {
            ctrl.createEngineModel("LEAP-1B",   "CFM International",
                    MotorizationType.TURBOFAN, 101.8, 121.4, 0.0157);
            ctrl.createEngineModel("Trent 900", "Rolls-Royce",
                    MotorizationType.TURBOFAN, 311.0, 340.0, 0.0152);
            ctrl.createEngineModel("PW1100G",   "Pratt & Whitney",
                    MotorizationType.TURBOFAN, 120.0, 150.0, 0.0149);
        });
    }

    @Test
    void ensureListAllEngineModelsReturnsRegisteredModels() {
        // Arrange
        registerLeap1B();
        ctrl.createEngineModel("Trent 900", "Rolls-Royce",
                MotorizationType.TURBOFAN, 311.0, 340.0, 0.0152);
        // Act
        final Iterable<EngineModel> all = ctrl.listAllEngineModels();
        // Assert
        int count = 0;
        for (final EngineModel ignored : all) count++;
        assertEquals(2, count);
    }
}