package controller;

import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.AircraftType;
import domain.aircraftmodel.FlightCharacteristics;
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

class AddEngineModelToAircraftControllerTest {

    private AddEngineModelToAircraftController ctrl;
    private InMemoryAircraftModelRepository    aircraftModelRepo;
    private InMemoryEngineModelRepository      engineModelRepo;

    private EngineModel turbofanEngine1;
    private EngineModel turbofanEngine2;
    private EngineModel turbopropEngine;
    private AircraftModel aircraftModel;

    @BeforeEach
    void setUp() {
        aircraftModelRepo = new InMemoryAircraftModelRepository();
        engineModelRepo   = new InMemoryEngineModelRepository();

        turbofanEngine1 = new EngineModel(
                "LEAP-1B", "CFM International",
                MotorizationType.TURBOFAN,
                new Thrust(101.8, 121.4), new TSFC(0.0157));

        turbofanEngine2 = new EngineModel(
                "Trent 900", "Rolls-Royce",
                MotorizationType.TURBOFAN,
                new Thrust(311.0, 340.0), new TSFC(0.0152));

        turbopropEngine = new EngineModel(
                "PW127", "Pratt & Whitney Canada",
                MotorizationType.TURBOPROP,
                new Thrust(15.0, 20.5), new TSFC(0.0300));

        engineModelRepo.save(turbofanEngine1);
        engineModelRepo.save(turbofanEngine2);
        engineModelRepo.save(turbopropEngine);

        aircraftModel = new AircraftModel(
                "737 MAX", "Boeing",
                AircraftType.PASSENGER,
                validCharacteristics(),
                List.of(turbofanEngine1));

        aircraftModelRepo.save(aircraftModel);

        ctrl = new AddEngineModelToAircraftController(aircraftModelRepo, engineModelRepo);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private FlightCharacteristics validCharacteristics() {
        return new FlightCharacteristics(
                41413.0, 65317.0, 79016.0, 26020.0,
                12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45);
    }

    // ── availableAircraftModels ───────────────────────────────────────────────

    @Test
    void ensureAvailableAircraftModelsReturnsAllModels() {
        // Arrange + Act
        final Iterable<AircraftModel> models = ctrl.availableAircraftModels();
        // Assert
        int count = 0;
        for (final AircraftModel ignored : models) count++;
        assertEquals(1, count);
    }

    // ── availableEngineModels ─────────────────────────────────────────────────

    @Test
    void ensureAvailableEngineModelsReturnsAllEngines() {
        // Arrange + Act
        final Iterable<EngineModel> engines = ctrl.availableEngineModels();
        // Assert
        int count = 0;
        for (final EngineModel ignored : engines) count++;
        assertEquals(3, count);
    }

    // ── addEngineModel — incompatible type (US057.2) ──────────────────────────

    @Test
    void ensureIncompatibleEngineTypeIsRejected() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.addEngineModel(aircraftModel, turbopropEngine));
    }

    // ── addEngineModel — duplicate (US057.3) ──────────────────────────────────

    @Test
    void ensureDuplicateEngineCannotBeAdded() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.addEngineModel(aircraftModel, turbofanEngine1));
    }

    // ── addEngineModel — null aircraft ────────────────────────────────────────

    @Test
    void ensureNullAircraftModelIsRejected() {
        // Arrange + Act + Assert
        assertThrows(Exception.class,
                () -> ctrl.addEngineModel(null, turbofanEngine2));
    }

    // ── addEngineModel — null engine ──────────────────────────────────────────

    @Test
    void ensureNullEngineModelIsRejected() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.addEngineModel(aircraftModel, null));
    }

    // ── addEngineModel — happy path (US057.1) ────────────────────────────────

    @Test
    void ensureCompatibleEngineIsAddedSuccessfully() {
        // Arrange + Act
        final AircraftModel updated = ctrl.addEngineModel(aircraftModel, turbofanEngine2);
        // Assert
        assertEquals(2, updated.certifiedEngines().size());
        assertTrue(updated.certifiedEngines().contains(turbofanEngine2));
    }

    @Test
    void ensureOriginalEngineIsStillPresentAfterAdding() {
        // Arrange + Act
        final AircraftModel updated = ctrl.addEngineModel(aircraftModel, turbofanEngine2);
        // Assert
        assertTrue(updated.certifiedEngines().contains(turbofanEngine1));
    }
}