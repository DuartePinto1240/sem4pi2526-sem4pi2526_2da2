package controller;

import domain.aircraft.Aircraft;
import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.AircraftType;
import domain.aircraftmodel.FlightCharacteristics;
import domain.aircraft.CabinConfiguration;
import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import persistence.impl.inmemory.InMemoryAircraftModelRepository;
import persistence.impl.inmemory.InMemoryAircraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListCompanyFleetControllerTest {

    private ListCompanyFleetController ctrl;
    private AircraftModel model;
    private InMemoryAircraftRepository aircraftRepo;

    @BeforeEach
    void setUp() {
        aircraftRepo = new InMemoryAircraftRepository();
        final InMemoryAircraftModelRepository modelRepo =
                new InMemoryAircraftModelRepository();

        final EngineModel engine = new EngineModel(
                "GE90", "Boeing", MotorizationType.TURBOFAN,
                new Thrust(300000, 400000),
                new TSFC(1.6e-4));

        final FlightCharacteristics chars = new FlightCharacteristics(
                150000, 250000, 300000, 100000,
                13000, 250, 12000, 440, 0.02, 0.5);

        model = new AircraftModel(
                "777-200ER", "Boeing", AircraftType.PASSENGER,
                chars, List.of(engine));
        modelRepo.save(model);

        // add two aircraft for TP and one for FR
        final CabinConfiguration cabinTP1 =
                new CabinConfiguration(200, 20, 0, 8);
        final CabinConfiguration cabinTP2 =
                new CabinConfiguration(100, 0, 0, 4);
        final CabinConfiguration cabinFR =
                new CabinConfiguration(180, 0, 0, 4);

        aircraftRepo.save(new Aircraft("CS-TUA", "Portugal", "TP", model, cabinTP1));
        aircraftRepo.save(new Aircraft("CS-TUB", "Portugal", "TP", model, cabinTP2));
        aircraftRepo.save(new Aircraft("EI-ABC", "Ireland",  "FR", model, cabinFR));

        ctrl = new ListCompanyFleetController(aircraftRepo, modelRepo);
    }

    // ─────────────────────────────────────────
    // US072 — list all fleet
    // ─────────────────────────────────────────

    @Test
    void ensureListFleetReturnsOnlyCompanyAircraft() {
        final List<Aircraft> result = toList(ctrl.listFleet("TP"));
        assertEquals(2, result.size());
        assertTrue(result.stream()
                .allMatch(a -> a.companyIata().equals("TP")));
    }

    @Test
    void ensureListFleetReturnsEmptyForUnknownCompany() {
        final List<Aircraft> result = toList(ctrl.listFleet("XX"));
        assertTrue(result.isEmpty());
    }

    @Test
    void ensureListFleetDoesNotReturnOtherCompanyAircraft() {
        final List<Aircraft> result = toList(ctrl.listFleet("TP"));
        assertTrue(result.stream()
                .noneMatch(a -> a.companyIata().equals("FR")));
    }

    // ─────────────────────────────────────────
    // US072a — list by model
    // ─────────────────────────────────────────

    @Test
    void ensureListByModelReturnsCorrectAircraft() {
        final List<Aircraft> result =
                toList(ctrl.listByModel("TP", model.identity()));
        assertEquals(2, result.size());
    }

    @Test
    void ensureListByModelReturnsEmptyForUnknownModel() {
        final List<Aircraft> result =
                toList(ctrl.listByModel("TP", 999L));
        assertTrue(result.isEmpty());
    }

    // ─────────────────────────────────────────
    // US072b — list by maker
    // ─────────────────────────────────────────

    @Test
    void ensureListByMakerReturnsCorrectAircraft() {
        final List<Aircraft> result =
                toList(ctrl.listByMaker("TP", "Boeing"));
        assertEquals(2, result.size());
    }

    @Test
    void ensureListByMakerIsCaseInsensitive() {
        final List<Aircraft> result =
                toList(ctrl.listByMaker("TP", "boeing"));
        assertEquals(2, result.size());
    }

    @Test
    void ensureListByMakerReturnsEmptyForUnknownMaker() {
        final List<Aircraft> result =
                toList(ctrl.listByMaker("TP", "Airbus"));
        assertTrue(result.isEmpty());
    }

    // ─────────────────────────────────────────
    // US072c — list by minimum capacity
    // ─────────────────────────────────────────

    @Test
    void ensureListByCapacityReturnsAircraftAboveMinimum() {
        // CS-TUA has 220 seats, CS-TUB has 100
        final List<Aircraft> result =
                toList(ctrl.listByCapacity("TP", 150));
        assertEquals(1, result.size());
        assertEquals("CS-TUA", result.get(0).registration());
    }

    @Test
    void ensureListByCapacityReturnsAllWhenMinimumIsZero() {
        final List<Aircraft> result =
                toList(ctrl.listByCapacity("TP", 0));
        assertEquals(2, result.size());
    }

    @Test
    void ensureListByCapacityReturnsEmptyWhenMinimumExceedsAll() {
        final List<Aircraft> result =
                toList(ctrl.listByCapacity("TP", 999));
        assertTrue(result.isEmpty());
    }

    // ─────────────────────────────────────────
    // US072d — list by age
    // ─────────────────────────────────────────

    @Test
    void ensureListByAgeReturnsSortedByRegistrationDate() {
        final List<Aircraft> result =
                toList(ctrl.listByAge("TP"));
        assertEquals(2, result.size());
        // both registered today so dates are equal
        // just verify both are returned and sorted
        assertNotNull(result.get(0).registrationDate());
        assertNotNull(result.get(1).registrationDate());
        assertTrue(
                result.get(0).registrationDate()
                        .compareTo(result.get(1).registrationDate()) <= 0);
    }

    @Test
    void ensureListByAgeReturnsEmptyForUnknownCompany() {
        final List<Aircraft> result =
                toList(ctrl.listByAge("XX"));
        assertTrue(result.isEmpty());
    }
    @Test
    void ensureUsedManufacturersReturnsManufacturer() {

        final List<String> result = new ArrayList<>();

        ctrl.usedManufacturers("TP").forEach(result::add);

        assertEquals(1, result.size());
        assertEquals("Boeing", result.get(0));
    }

    @Test
    void ensureUsedManufacturersReturnsEmptyForUnknownCompany() {

        final List<String> result = new ArrayList<>();

        ctrl.usedManufacturers("XX").forEach(result::add);

        assertTrue(result.isEmpty());
    }

    @Test
    void ensureAvailableModelsReturnsRegisteredModels() {

        final List<AircraftModel> result = new ArrayList<>();

        ctrl.availableModels().forEach(result::add);

        assertEquals(1, result.size());
        assertEquals("777-200ER",
                result.get(0).modelName());
    }

    @Test
    void ensureListByModelNameReturnsAlphabeticallySortedAircraft() {

        final List<Aircraft> result =
                toList(ctrl.listByModelName("TP"));

        assertEquals(2, result.size());

        assertEquals("CS-TUA",
                result.get(0).registration());

        assertEquals("CS-TUB",
                result.get(1).registration());
    }

    @Test
    void ensureListByModelNameReturnsEmptyForUnknownCompany() {

        final List<Aircraft> result =
                toList(ctrl.listByModelName("XX"));

        assertTrue(result.isEmpty());
    }

    @Test
    void ensureListByMakerTrimsManufacturer() {

        final List<Aircraft> result =
                toList(ctrl.listByMaker("TP", " Boeing "));

        assertEquals(2, result.size());
    }

    @Test
    void ensureListByCapacityIncludesExactMinimum() {

        final List<Aircraft> result =
                toList(ctrl.listByCapacity("TP", 100));

        assertEquals(2, result.size());
    }

    @Test
    void ensureAvailableModelsIsNotEmpty() {

        assertTrue(ctrl.availableModels().iterator().hasNext());
    }

    // ─────────────────────────────────────────
    // helper
    // ─────────────────────────────────────────

    private List<Aircraft> toList(final Iterable<Aircraft> iterable) {
        final List<Aircraft> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }
}
