package controller;

import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.AircraftType;
import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import persistence.impl.inmemory.InMemoryAircraftModelRepository;
import persistence.impl.inmemory.InMemoryEngineModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreateAircraftModelControllerTest {

    private CreateAircraftModelController ctrl;
    private InMemoryEngineModelRepository engineRepo;
    private EngineModel turbofanEngine;

    @BeforeEach
    void setUp() {
        engineRepo = new InMemoryEngineModelRepository();
        turbofanEngine = new EngineModel(
                "LEAP-1B", "CFM International",
                MotorizationType.TURBOFAN,
                new Thrust(101.8, 121.4), new TSFC(0.0157));
        engineRepo.save(turbofanEngine);
        ctrl = new CreateAircraftModelController(
                new InMemoryAircraftModelRepository(), engineRepo);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private AircraftModel register737Max() {
        return ctrl.createAircraftModel(
                "737 MAX", "Boeing", AircraftType.PASSENGER,
                41413.0, 65317.0, 79016.0, 26020.0,
                12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45,
                List.of(turbofanEngine));
    }

    // ── uniqueness ────────────────────────────────────────────────────────────

    @Test
    void ensureDuplicateNameAndManufacturerIsRejected() {
        // Arrange
        register737Max();
        // Act + Assert
        assertThrows(IllegalArgumentException.class, this::register737Max);
    }

    @Test
    void ensureSameNameDifferentManufacturerIsAllowed() {
        // Arrange
        register737Max();
        // Act + Assert
        assertDoesNotThrow(() -> ctrl.createAircraftModel(
                "737 MAX", "Other Manufacturer", AircraftType.PASSENGER,
                41413.0, 65317.0, 79016.0, 26020.0,
                12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45,
                List.of(turbofanEngine)));
    }

    // ── engine validation ─────────────────────────────────────────────────────

    @Test
    void ensureEmptyEngineListIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.createAircraftModel(
                        "737 MAX", "Boeing", AircraftType.PASSENGER,
                        41413.0, 65317.0, 79016.0, 26020.0,
                        12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45,
                        List.of()));
    }

    @Test
    void ensureMixedEngineTypesAreRejected() {
        // Arrange
        final EngineModel turboprop = new EngineModel(
                "PW127", "P&WC", MotorizationType.TURBOPROP,
                new Thrust(15.0, 20.5), new TSFC(0.0300));
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.createAircraftModel(
                        "737 MAX", "Boeing", AircraftType.PASSENGER,
                        41413.0, 65317.0, 79016.0, 26020.0,
                        12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45,
                        List.of(turbofanEngine, turboprop)));
    }

    // ── flight characteristics validation ────────────────────────────────────

    @Test
    void ensureNegativeEmptyWeightIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.createAircraftModel(
                        "737 MAX", "Boeing", AircraftType.PASSENGER,
                        -1.0, 65317.0, 79016.0, 26020.0,
                        12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45,
                        List.of(turbofanEngine)));
    }

    @Test
    void ensureEmptyWeightGreaterThanMzfwIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.createAircraftModel(
                        "737 MAX", "Boeing", AircraftType.PASSENGER,
                        70000.0, 65317.0, 79016.0, 26020.0,
                        12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45,
                        List.of(turbofanEngine)));
    }

    @Test
    void ensureMzfwGreaterThanMtowIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.createAircraftModel(
                        "737 MAX", "Boeing", AircraftType.PASSENGER,
                        41413.0, 80000.0, 79016.0, 26020.0,
                        12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45,
                        List.of(turbofanEngine)));
    }

    // ── happy path ───────────────────────────────────────────────────────────

    @Test
    void ensureValidAircraftModelIsSavedAndReturned() {
        // Arrange + Act
        final AircraftModel saved = register737Max();
        // Assert
        assertNotNull(saved);
        assertEquals("737 MAX",              saved.modelName());
        assertEquals("Boeing",               saved.manufacturer());
        assertEquals(AircraftType.PASSENGER, saved.type());
        assertEquals(1, saved.certifiedEngines().size());
    }

    @Test
    void ensureMultipleDifferentModelsCanBeRegistered() {
        assertDoesNotThrow(() -> {
            register737Max();
            ctrl.createAircraftModel(
                    "A320", "Airbus", AircraftType.PASSENGER,
                    42600.0, 57000.0, 64300.0, 23860.0,
                    12100.0, 230.0, 6100.0, 122.6, 0.024, 1.40,
                    List.of(turbofanEngine));
        });
    }

    // ── availableEngineModels ─────────────────────────────────────────────────

    @Test
    void ensureAvailableEngineModelsReturnsEnginesFromRepository() {
        // Arrange + Act
        final Iterable<EngineModel> engines = ctrl.availableEngineModels();
        // Assert
        int count = 0;
        for (final EngineModel ignored : engines) count++;
        assertEquals(1, count);
    }
}