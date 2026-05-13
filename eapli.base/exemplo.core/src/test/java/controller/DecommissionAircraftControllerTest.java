package controller;

import domain.aircraft.Aircraft;
import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.AircraftType;
import domain.aircraftmodel.FlightCharacteristics;
import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import persistence.impl.inmemory.InMemoryAircraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecommissionAircraftControllerTest {

    private DecommissionAircraftController ctrl;
    private InMemoryAircraftRepository aircraftRepo;
    private AircraftModel model;

    @BeforeEach
    void setUp() {
        aircraftRepo = new InMemoryAircraftRepository();

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

        ctrl = new DecommissionAircraftController(aircraftRepo);
    }

    private Aircraft createAndSaveAircraft(final String reg, final String companyIata) {
        final domain.aircraft.CabinConfiguration cabin =
                new domain.aircraft.CabinConfiguration(200, 20, 0, 8);
        final Aircraft a = new Aircraft(reg, "Portugal", companyIata, model, cabin);
        aircraftRepo.save(a);
        return a;
    }

    // activeAircraftOfCompany

    @Test
    void ensureActiveAircraftOfCompanyReturnsOnlyActiveOnes() {
        createAndSaveAircraft("CS-TUA", "TP");
        final Aircraft toDecommission = createAndSaveAircraft("CS-TUB", "TP");
        toDecommission.decommission();
        aircraftRepo.save(toDecommission);

        final List<Aircraft> result = new java.util.ArrayList<>();
        ctrl.activeAircraftOfCompany("TP").forEach(result::add);

        assertEquals(1, result.size());
        assertEquals("CS-TUA", result.get(0).registration());
    }

    @Test
    void ensureActiveAircraftOfCompanyReturnsEmptyWhenNoneActive() {
        final Aircraft a = createAndSaveAircraft("CS-TUA", "TP");
        a.decommission();
        aircraftRepo.save(a);

        final List<Aircraft> result = new java.util.ArrayList<>();
        ctrl.activeAircraftOfCompany("TP").forEach(result::add);

        assertTrue(result.isEmpty());
    }

    @Test
    void ensureActiveAircraftOfCompanyFiltersOtherCompanies() {
        createAndSaveAircraft("CS-TUA", "TP");
        createAndSaveAircraft("EI-ABC", "FR");

        final List<Aircraft> result = new java.util.ArrayList<>();
        ctrl.activeAircraftOfCompany("TP").forEach(result::add);

        assertEquals(1, result.size());
        assertEquals("CS-TUA", result.get(0).registration());
    }

    // decommissionAircraft

    @Test
    void ensureValidDecommissionSucceeds() {
        createAndSaveAircraft("CS-TUA", "TP");

        final Aircraft result = ctrl.decommissionAircraft("CS-TUA", "TP");

        assertTrue(result.operationalStatus().isDecommissioned());
        assertFalse(result.operationalStatus().isActive());
    }

    @Test
    void ensureDecommissionedAircraftIsPersistedWithNewStatus() {
        createAndSaveAircraft("CS-TUA", "TP");
        ctrl.decommissionAircraft("CS-TUA", "TP");

        final Aircraft persisted = aircraftRepo.ofIdentity("CS-TUA").orElseThrow();
        assertTrue(persisted.operationalStatus().isDecommissioned());
    }

    @Test
    void ensureDecommissionNonExistentAircraftThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.decommissionAircraft("XX-NONE", "TP"));
    }

    @Test
    void ensureDecommissionAircraftFromWrongCompanyThrows() {
        createAndSaveAircraft("CS-TUA", "TP");

        assertThrows(IllegalArgumentException.class,
                () -> ctrl.decommissionAircraft("CS-TUA", "FR"));
    }

    @Test
    void ensureDecommissionAlreadyDecommissionedAircraftThrows() {
        createAndSaveAircraft("CS-TUA", "TP");
        ctrl.decommissionAircraft("CS-TUA", "TP");

        assertThrows(IllegalArgumentException.class,
                () -> ctrl.decommissionAircraft("CS-TUA", "TP"));
    }

    @Test
    void ensureDecommissionDoesNotAffectOtherAircraft() {
        createAndSaveAircraft("CS-TUA", "TP");
        createAndSaveAircraft("CS-TUB", "TP");

        ctrl.decommissionAircraft("CS-TUA", "TP");

        final Aircraft other = aircraftRepo.ofIdentity("CS-TUB").orElseThrow();
        assertTrue(other.operationalStatus().isActive());
    }
}