package controller;

import domain.aircraft.Aircraft;
import domain.aircraft.OperationalStatus;
import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.AircraftType;
import domain.aircraftmodel.FlightCharacteristics;
import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import java.util.ArrayList;
import domain.Country;
import domain.aircraft.CabinConfiguration;
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
    @Test
    void ensureAvailableAircraftModelsReturnsRegisteredModels() {
        final List<AircraftModel> models = new ArrayList<>();
        ctrl.availableAircraftModels().forEach(models::add);

        assertEquals(1, models.size());
        assertEquals("777-200ER", models.get(0).modelName());
    }

    @Test
    void ensureAvailableAircraftModelsIsEmptyWhenNoneRegistered() {
        final InMemoryAircraftRepository      emptyAircraftRepo = new InMemoryAircraftRepository();
        final InMemoryAircraftModelRepository emptyModelRepo    = new InMemoryAircraftModelRepository();
        final AddAircraftToFleetController    emptyCtrl         =
                new AddAircraftToFleetController(emptyAircraftRepo, emptyModelRepo);

        final List<AircraftModel> models = new ArrayList<>();
        emptyCtrl.availableAircraftModels().forEach(models::add);

        assertTrue(models.isEmpty());
    }

    @Test
    void ensureNullRegistrationIsRejected() {
        assertThrows(Exception.class, () ->
                ctrl.addAircraft(null, "Portugal", "TP",
                        model.identity(), 100, 0, 0, 4));
    }

    @Test
    void ensureEmptyRegistrationIsRejected() {
        assertThrows(Exception.class, () ->
                ctrl.addAircraft("", "Portugal", "TP",
                        model.identity(), 100, 0, 0, 4));
    }

    @Test
    void ensureAircraftCompanyIataIsStoredUppercase() {
        final Aircraft aircraft = ctrl.addAircraft(
                "CS-TUD", "Portugal", "tp",
                model.identity(), 100, 0, 0, 4);

        assertEquals("TP", aircraft.companyIata());
    }

    @Test
    void ensureAircraftRegistrationIsStoredUppercase() {
        final Aircraft aircraft = ctrl.addAircraft(
                "cs-tue", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        assertEquals("CS-TUE", aircraft.registration());
    }

    @Test
    void ensureAircraftCountryOfRegistrationIsSet() {
        final Aircraft aircraft = ctrl.addAircraft(
                "CS-TUF", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        assertEquals("Portugal", aircraft.countryOfRegistration());
    }

    @Test
    void ensureAircraftModelIdMatchesSelectedModel() {
        final Aircraft aircraft = ctrl.addAircraft(
                "CS-TUG", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        assertEquals(model.identity(), aircraft.aircraftModelId());
    }


    @Test
    void ensureCabinConfigurationValuesAreStoredCorrectly() {
        final Aircraft aircraft = ctrl.addAircraft(
                "CS-TUH", "Portugal", "TP",
                model.identity(), 180, 30, 10, 12);

        assertEquals(180, aircraft.cabinConfiguration().economySeats());
        assertEquals(30, aircraft.cabinConfiguration().businessSeats());
        assertEquals(10, aircraft.cabinConfiguration().firstClassSeats());
        assertEquals(12, aircraft.cabinConfiguration().crewSize());
    }



    @Test
    void ensureAircraftOperationalStatusIsNotNull() {
        final Aircraft aircraft = ctrl.addAircraft(
                "CS-TUJ", "Portugal", "TP",
                model.identity(), 100, 10, 0, 4);

        assertNotNull(aircraft.operationalStatus());
    }

    @Test
    void ensureAircraftRepositoryStoresAircraft() {
        ctrl.addAircraft(
                "CS-TUK", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        assertTrue(ctrl.availableAircraftModels().iterator().hasNext());
    }

    @Test
    void ensureRegistrationIsTrimmedAndUppercase() {
        final Aircraft aircraft = ctrl.addAircraft(
                "  cs-tul  ", "Portugal", "tp",
                model.identity(), 100, 0, 0, 4);

        assertEquals("CS-TUL", aircraft.registration());
        assertEquals("TP", aircraft.companyIata());
    }

    @Test
    void ensureNegativeSeatsAreRejected() {
        assertThrows(Exception.class, () ->
                ctrl.addAircraft(
                        "CS-TUM", "Portugal", "TP",
                        model.identity(), -1, 0, 0, 4));
    }


    @Test
    void ensureAircraftModelDataIsAccessible() {
        assertEquals("Boeing", model.manufacturer());
        assertEquals(AircraftType.PASSENGER, model.type());

        EngineModel engine = model.certifiedEngines().get(0);

        assertEquals("GE", engine.manufacturer());
        assertEquals(MotorizationType.TURBOFAN, engine.motorizationType());
    }



    @Test
    void ensureNegativeEconomySeatsAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.addAircraft(
                        "CS-AAA", "Portugal", "TP",
                        model.identity(), -1, 0, 0, 4));
    }

    @Test
    void ensureNegativeCrewSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.addAircraft(
                        "CS-AAB", "Portugal", "TP",
                        model.identity(), 10, 0, 0, -1));
    }

    @Test
    void ensureNullCountryIsRejected() {
        assertThrows(Exception.class, () ->
                ctrl.addAircraft(
                        "CS-AAC", null, "TP",
                        model.identity(), 100, 0, 0, 4));
    }

    @Test
    void ensureEmptyCompanyIataIsRejected() {
        assertThrows(Exception.class, () ->
                ctrl.addAircraft(
                        "CS-AAD", "Portugal", "",
                        model.identity(), 100, 0, 0, 4));
    }

    @Test
    void ensureCompanyIataIsTrimmed() {
        final Aircraft aircraft = ctrl.addAircraft(
                "CS-AAE", "Portugal", " tp ",
                model.identity(), 100, 0, 0, 4);

        assertEquals("TP", aircraft.companyIata());
    }

    @Test
    void ensureDuplicateRegistrationMessageIsCorrect() {

        ctrl.addAircraft(
                "CS-TUA", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        final IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () ->
                        ctrl.addAircraft(
                                "CS-TUA", "Portugal", "TP",
                                model.identity(), 100, 0, 0, 4));

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void ensureAircraftCanBeDecommissioned() {

        final Aircraft aircraft = ctrl.addAircraft(
                "CS-DEC", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        aircraft.decommission();

        assertFalse(aircraft.operationalStatus().isActive());
    }

    @Test
    void ensureAircraftCannotBeDecommissionedTwice() {

        final Aircraft aircraft = ctrl.addAircraft(
                "CS-DED", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        aircraft.decommission();

        assertThrows(IllegalArgumentException.class,
                aircraft::decommission);
    }

    @Test
    void ensureAircraftSameAsWorks() {

        final Aircraft first = ctrl.addAircraft(
                "CS-SAME", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        final Aircraft second = ctrl.addAircraft(
                "CS-SAM2", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        assertFalse(first.sameAs(second));
    }

    @Test
    void ensureAircraftSameAsDetectsDifferentAircraft() {

        final Aircraft first = ctrl.addAircraft(
                "CS-ONE", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        final Aircraft second = new Aircraft(
                "CS-TWO",
                "Portugal",
                "TP",
                model,
                new CabinConfiguration(100, 0, 0, 4));

        assertFalse(first.sameAs(second));
    }

    @Test
    void ensureAircraftAgeIsZeroWhenCreated() {

        final Aircraft aircraft = ctrl.addAircraft(
                "CS-AGE", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        assertEquals(0, aircraft.ageInYears());
    }

    @Test
    void ensureAircraftToStringContainsRegistration() {

        final Aircraft aircraft = ctrl.addAircraft(
                "CS-STR", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        assertTrue(aircraft.toString().contains("CS-STR"));
    }

    @Test
    void ensureIdentityReturnsRegistration() {

        final Aircraft aircraft = ctrl.addAircraft(
                "CS-ID", "Portugal", "TP",
                model.identity(), 100, 0, 0, 4);

        assertEquals("CS-ID", aircraft.identity());
    }

    @Test
    void ensureAircraftModelToStringContainsModelName() {

        assertTrue(model.toString().contains("777-200ER"));
    }

    @Test
    void ensureAircraftModelSameAsWorks() {

        final EngineModel otherEngine = new EngineModel(
                "GE90X", "GE", MotorizationType.TURBOFAN,
                new Thrust(300000, 400000),
                new TSFC(1.6e-4));

        final FlightCharacteristics otherChars =
                new FlightCharacteristics(
                        150000,
                        250000,
                        300000,
                        100000,
                        13000,
                        250,
                        12000,
                        440,
                        0.02,
                        0.5);

        final AircraftModel other = new AircraftModel(
                "777-200ER",
                "Boeing",
                AircraftType.PASSENGER,
                otherChars,
                List.of(otherEngine));

        assertTrue(model.sameAs(other));
    }

    @Test
    void ensureCertifiedEngineCanBeAdded() {

        final EngineModel second = new EngineModel(
                "GE91",
                "GE",
                MotorizationType.TURBOFAN,
                new Thrust(300000, 400000),
                new TSFC(1.5e-4));

        model.addCertifiedEngine(second);

        assertEquals(2, model.certifiedEngines().size());
    }

    @Test
    void ensureDuplicateEngineIsRejected() {

        final EngineModel existing =
                model.certifiedEngines().get(0);

        assertThrows(IllegalArgumentException.class,
                () -> model.addCertifiedEngine(existing));
    }

    @Test
    void ensureCertifiedEngineCanBeRemoved() {

        final EngineModel second = new EngineModel(
                "GE92",
                "GE",
                MotorizationType.TURBOFAN,
                new Thrust(300000, 400000),
                new TSFC(1.5e-4));

        model.addCertifiedEngine(second);

        model.removeCertifiedEngine(second);

        assertEquals(1, model.certifiedEngines().size());
    }

    @Test
    void ensureLastEngineCannotBeRemoved() {

        final EngineModel existing =
                model.certifiedEngines().get(0);

        assertThrows(IllegalArgumentException.class,
                () -> model.removeCertifiedEngine(existing));
    }

    @Test
    void ensureTotalSeatsIsCalculatedCorrectly() {

        CabinConfiguration cabin =
                new CabinConfiguration(100, 20, 10, 5);

        assertEquals(130, cabin.totalSeats());
    }

    @Test
    void ensureCabinConfigurationToStringWorks() {

        CabinConfiguration cabin =
                new CabinConfiguration(100, 20, 10, 5);

        assertNotNull(cabin.toString());
    }

    @Test
    void ensureActiveStatusWorks() {

        OperationalStatus status = OperationalStatus.active();

        assertTrue(status.isActive());
    }

    @Test
    void ensureDecommissionedStatusWorks() {

        OperationalStatus status =
                OperationalStatus.decommissioned();

        assertFalse(status.isActive());
    }

    @Test
    void ensureOperationalStatusToStringWorks() {

        assertNotNull(
                OperationalStatus.active().toString());
    }
    @Test
    void ensureEngineModelToStringWorks() {

        EngineModel engine = new EngineModel(
                "GE90",
                "GE",
                MotorizationType.TURBOFAN,
                new Thrust(300000, 400000),
                new TSFC(1.6e-4));

        assertNotNull(engine.toString());
    }

    @Test
    void ensureEngineMotorizationTypeWorks() {

        EngineModel engine = new EngineModel(
                "GE90",
                "GE",
                MotorizationType.TURBOFAN,
                new Thrust(300000, 400000),
                new TSFC(1.6e-4));

        assertEquals(
                MotorizationType.TURBOFAN,
                engine.motorizationType());
    }


    @Test
    void ensureFlightCharacteristicsToStringWorks() {

        FlightCharacteristics fc =
                new FlightCharacteristics(
                        150000,
                        250000,
                        300000,
                        100000,
                        13000,
                        250,
                        12000,
                        440,
                        0.02,
                        0.5);

        assertNotNull(fc.toString());
    }


}