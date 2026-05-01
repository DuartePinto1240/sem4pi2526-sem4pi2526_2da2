package controller;

import domain.aircraft.Aircraft;
import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.AircraftType;
import domain.aircraftmodel.FlightCharacteristics;
import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import persistence.impl.inmemory.InMemoryAircraftModelRepository;
import persistence.impl.inmemory.InMemoryAircraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AddAircraftToFleetControllerTest {

    private AddAircraftToFleetController ctrl;
    private AircraftModel model;

    @BeforeEach
    void setUp() {
        final InMemoryAircraftRepository      aircraftRepo = new InMemoryAircraftRepository();
        final InMemoryAircraftModelRepository modelRepo    = new InMemoryAircraftModelRepository();

        final EngineModel engine = new EngineModel(
                "GE90", "GE", MotorizationType.TURBOFAN,
                new Thrust(300000, 400000),
                new TSFC(1.6e-4));

        final FlightCharacteristics chars = new FlightCharacteristics(
                150000, 250000, 300000, 100000,
                13000, 250, 12000, 440, 0.02, 0.5);

        model = new AircraftModel(
                "777-200ER", "Boeing", AircraftType.PASSENGER,
                chars, List.of(engine));
        modelRepo.save(model);

        ctrl = new AddAircraftToFleetController(aircraftRepo, modelRepo);
    }

    @Test
    void ensureValidAircraftIsAddedSuccessfully() {
        final Aircraft aircraft = ctrl.addAircraft(
                "CS-TUA", "Portugal", "TP",
                model.identity(), 200, 20, 0, 8);

        assertNotNull(aircraft);
        assertEquals("CS-TUA", aircraft.registration());
        assertTrue(aircraft.operationalStatus().isActive());
    }

    @Test
    void ensureDuplicateRegistrationIsRejected() {
        ctrl.addAircraft("CS-TUA", "Portugal", "TP",
                model.identity(), 200, 20, 0, 8);

        assertThrows(IllegalArgumentException.class, () ->
                ctrl.addAircraft("CS-TUA", "Spain", "FR",
                        model.identity(), 150, 0, 0, 4));
    }

    @Test
    void ensureInvalidModelIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.addAircraft("CS-TUB", "Portugal", "TP",
                        999L, 200, 0, 0, 4));
    }

    @Test
    void ensureAircraftIsActiveUponRegistration() {
        final Aircraft aircraft = ctrl.addAircraft(
                "CS-TUC", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        assertTrue(aircraft.operationalStatus().isActive());
    }

    @Test
    void ensureMultipleAircraftCanBeAddedWithDifferentRegistrations() {
        ctrl.addAircraft("CS-TUA", "Portugal", "TP",
                model.identity(), 200, 20, 0, 8);
        final Aircraft second = ctrl.addAircraft("CS-TUB", "Portugal", "TP",
                model.identity(), 200, 20, 0, 8);

        assertNotNull(second);
        assertEquals("CS-TUB", second.registration());
    }
}