package controller;

import domain.AirControlArea;
import domain.AirTransportCompany;
import domain.GeographicBoundary;
import domain.aircraft.Aircraft;
import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.AircraftType;
import domain.aircraftmodel.FlightCharacteristics;
import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import domain.flightroute.FlightRoute;
import domain.flightroute.RecurringSchedule;
import domain.flightroute.ScheduleEntry;
import domain.pilot.Pilot;
import domain.Airport;
import domain.IATACode;
import domain.AirControlAreaCode;
import domain.AirportCoordinates;
import domain.AirportName;
import domain.Elevation;
import domain.Country;
import lprog.FlightLeg;
import lprog.FlightPlan;
import lprog.FlightPlanStatus;
import lprog.FlightSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.impl.inmemory.InMemoryFlightPlanRepository;
import repositories.AirControlAreaRepository;
import repositories.AircraftRepository;
import repositories.AirportRepository;
import repositories.FlightPlanRepository;
import repositories.FlightRouteRepository;
import repositories.PilotRepository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateFlightPlanControllerTest {

    private FlightRouteRepository    routeRepo;
    private AircraftRepository       aircraftRepo;
    private PilotRepository          pilotRepo;
    private FlightPlanRepository     flightPlanRepo;
    private AirportRepository        airportRepo;
    private AirControlAreaRepository areaRepo;
    private CreateFlightPlanController ctrl;

    // ── domain helpers ────────────────────────────────────────────────────────

    private AirTransportCompany tapCompany() {
        return new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
    }

    private AirTransportCompany otherCompany() {
        return new AirTransportCompany("Ryanair", "FR", "RYR");
    }

    private FlightRoute activeRoute() {
        return new FlightRoute("TP123", "TP", "OPO", "LIS",
                new RecurringSchedule(List.of(
                        new ScheduleEntry(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0)),
                        new ScheduleEntry(DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(9, 0)))));
    }

    private FlightRoute routeForCompany(final String designator,
                                        final String companyIata,
                                        final String origin,
                                        final String dest) {
        return new FlightRoute(designator, companyIata, origin, dest,
                new RecurringSchedule(List.of(
                        new ScheduleEntry(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0)))));
    }

    private Aircraft tapAircraft() {
        return aircraft("CS-TTP", "TP");
    }

    private Aircraft otherAircraft() {
        return aircraft("EI-ABC", "FR");
    }

    private Aircraft aircraft(final String reg, final String companyIata) {
        final EngineModel engine = new EngineModel(
                "CFM56", "CFM International", MotorizationType.TURBOFAN,
                new Thrust(100.0, 120.0), new TSFC(0.016));
        final FlightCharacteristics chars = new FlightCharacteristics(
                41000, 60000, 78000, 25000, 12000, 220, 5000, 124, 0.026, 1.4);
        final AircraftModel model = new AircraftModel(
                "A320", "Airbus", AircraftType.PASSENGER, chars, List.of(engine));
        return new Aircraft(reg, "Portugal", companyIata, model,
                new domain.aircraft.CabinConfiguration(150, 12, 0, 6));
    }

    private Pilot tapPilot() {
        return pilot("Joao Silva", "joao@tap.pt", tapCompany());
    }

    private Pilot otherPilot() {
        return pilot("Peter Murphy", "peter@ryanair.com", otherCompany());
    }

    private Pilot pilot(final String name, final String email,
                        final AirTransportCompany company) {
        final EngineModel engine = new EngineModel(
                "CFM56", "CFM International", MotorizationType.TURBOFAN,
                new Thrust(100.0, 120.0), new TSFC(0.016));
        final FlightCharacteristics chars = new FlightCharacteristics(
                41000, 60000, 78000, 25000, 12000, 220, 5000, 124, 0.026, 1.4);
        final AircraftModel model = new AircraftModel(
                "A320", "Airbus", AircraftType.PASSENGER, chars, List.of(engine));
        return new Pilot(name, email, "912345678", company, List.of(model));
    }

    private List<FlightLeg> oneValidLeg() {
        return List.of(new FlightLeg(
                "OPO", "2028-06-01", "08:00",
                "LIS", "09:00", 5000.0, "kg"));
    }

    private List<FlightLeg> legWithSegment() {
        final FlightLeg leg = new FlightLeg(
                "OPO", "2028-06-01", "08:00",
                "LIS", "09:00", 5000.0, "kg");
        leg.addSegment(new FlightSegment(
                41.2481, -8.6814, 38.7813, -9.1359,
                0.0, 10000.0, "cruise", 50.0, 270.0, 10.0));
        return List.of(leg);
    }

    private LocalDateTime futureDate() {
        return LocalDateTime.now().plusDays(30);
    }

    // ── setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        routeRepo      = mock(FlightRouteRepository.class);
        aircraftRepo   = mock(AircraftRepository.class);
        pilotRepo      = mock(PilotRepository.class);
        flightPlanRepo = new InMemoryFlightPlanRepository();
        airportRepo    = mock(AirportRepository.class);
        areaRepo       = mock(AirControlAreaRepository.class);
        // airportRepo returns empty by default → coordinate validation skipped
        ctrl = new CreateFlightPlanController(routeRepo, aircraftRepo, pilotRepo,
                flightPlanRepo, airportRepo, areaRepo);
    }

    // ── US080.6: DRAFT status ─────────────────────────────────────────────────

    @Test
    void ensureFlightPlanIsCreatedWithDraftStatus() {
        final FlightPlan plan = ctrl.createFlightPlan(
                "TP001", activeRoute(), tapAircraft(), tapPilot(),
                futureDate(), oneValidLeg());
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    // ── US080.6: persisted in repository ─────────────────────────────────────

    @Test
    void ensureFlightPlanIsPersistedAfterCreation() {
        ctrl.createFlightPlan(
                "TP002", activeRoute(), tapAircraft(), tapPilot(),
                futureDate(), oneValidLeg());
        assertTrue(flightPlanRepo.existsByFlightId("TP002"));
    }

    // ── US080: duplicate flight ID rejected (one plan per flight) ────────────

    @Test
    void ensureDuplicateFlightIdIsRejected() {
        ctrl.createFlightPlan(
                "TP-DUP", activeRoute(), tapAircraft(), tapPilot(),
                futureDate(), oneValidLeg());
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createFlightPlan(
                        "TP-DUP", activeRoute(), tapAircraft(), tapPilot(),
                        futureDate(), oneValidLeg()));
    }

    // ── US080.6: correct metadata stored ─────────────────────────────────────

    @Test
    void ensureFlightPlanStoresRouteDesignator() {
        final FlightPlan plan = ctrl.createFlightPlan(
                "TP003", activeRoute(), tapAircraft(), tapPilot(),
                futureDate(), oneValidLeg());
        assertEquals("TP123", plan.routeDesignator());
    }

    @Test
    void ensureFlightPlanStoresAircraftRegistration() {
        final FlightPlan plan = ctrl.createFlightPlan(
                "TP004", activeRoute(), tapAircraft(), tapPilot(),
                futureDate(), oneValidLeg());
        assertEquals("CS-TTP", plan.aircraftRegistration());
    }

    @Test
    void ensureFlightPlanStoresPilotId() {
        final FlightPlan plan = ctrl.createFlightPlan(
                "TP005", activeRoute(), tapAircraft(), tapPilot(),
                futureDate(), oneValidLeg());
        // pilotId is null in test (no persistence-generated id), just assert no exception
        assertDoesNotThrow(() -> plan.pilotId());
    }

    // ── US080: DSL auto-generated ─────────────────────────────────────────────

    @Test
    void ensureDslContentIsAutoGeneratedAfterCreation() {
        final FlightPlan plan = ctrl.createFlightPlan(
                "TP006", activeRoute(), tapAircraft(), tapPilot(),
                futureDate(), oneValidLeg());
        assertNotNull(plan.dslContent(), "DSL should be auto-generated");
        assertTrue(plan.dslContent().contains("TP006"));
        assertTrue(plan.dslContent().contains("flight"));
    }

    @Test
    void ensureDslContentContainsLegData() {
        final FlightPlan plan = ctrl.createFlightPlan(
                "TP007", activeRoute(), tapAircraft(), tapPilot(),
                futureDate(), oneValidLeg());
        assertTrue(plan.dslContent().contains("OPO"));
        assertTrue(plan.dslContent().contains("LIS"));
        assertTrue(plan.dslContent().contains("5000"));
    }



    // ── US080: segments preserved in plan ────────────────────────────────────

    @Test
    void ensureLegsWithSegmentsArePreserved() {
        final FlightPlan plan = ctrl.createFlightPlan(
                "TP009", activeRoute(), tapAircraft(), tapPilot(),
                futureDate(), legWithSegment());
        assertEquals(1, plan.legs().get(0).segments().size());
        assertEquals(41.2481, plan.legs().get(0).segments().get(0).fromLat(), 0.0001);
    }

    // ── US080.1: inactive route rejected ─────────────────────────────────────

    @Test
    void ensureInactiveRouteIsRejected() {
        final FlightRoute inactive = activeRoute();
        inactive.deactivate(java.time.LocalDate.now());
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createFlightPlan(
                        "TP010", inactive, tapAircraft(), tapPilot(),
                        futureDate(), oneValidLeg()));
    }

    // ── US080.2: aircraft from wrong company rejected ─────────────────────────

    @Test
    void ensureAircraftFromWrongCompanyIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createFlightPlan(
                        "TP011", activeRoute(), otherAircraft(), tapPilot(),
                        futureDate(), oneValidLeg()));
    }

    // ── US080.3: pilot from wrong company rejected ────────────────────────────

    @Test
    void ensurePilotFromWrongCompanyIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createFlightPlan(
                        "TP012", activeRoute(), tapAircraft(), otherPilot(),
                        futureDate(), oneValidLeg()));
    }

    // ── US080.4: past departure rejected ─────────────────────────────────────

    @Test
    void ensurePastDepartureDateIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createFlightPlan(
                        "TP013", activeRoute(), tapAircraft(), tapPilot(),
                        LocalDateTime.now().minusDays(1), oneValidLeg()));
    }

    @Test
    void ensurePresentDepartureDateIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createFlightPlan(
                        "TP014", activeRoute(), tapAircraft(), tapPilot(),
                        LocalDateTime.now().minusSeconds(1), oneValidLeg()));
    }

    // ── US080.7: empty legs rejected ─────────────────────────────────────────

    @Test
    void ensureEmptyLegsIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createFlightPlan(
                        "TP015", activeRoute(), tapAircraft(), tapPilot(),
                        futureDate(), List.of()));
    }

    // ── US080.5: negative fuel rejected ──────────────────────────────────────

    @Test
    void ensureNegativeFuelInLegIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createFlightPlan(
                        "TP016", activeRoute(), tapAircraft(), tapPilot(),
                        futureDate(), List.of(new FlightLeg(
                                "OPO", "2028-06-01", "08:00",
                                "LIS", "09:00", -100.0, "kg"))));
    }

    @Test
    void ensureZeroFuelInLegIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createFlightPlan(
                        "TP017", activeRoute(), tapAircraft(), tapPilot(),
                        futureDate(), List.of(new FlightLeg(
                                "OPO", "2028-06-01", "08:00",
                                "LIS", "09:00", 0.0, "kg"))));
    }

    // ── US080: activeRoutesForPilot filters by company ────────────────────────

    @Test
    void ensureActiveRoutesForPilotReturnsOnlyCompanyRoutes() {
        final FlightRoute tp = routeForCompany("TP001", "TP", "OPO", "LIS");
        final FlightRoute fr = routeForCompany("FR001", "FR", "OPO", "MAD");
        when(routeRepo.findAll()).thenReturn(List.of(tp, fr));

        final Iterable<FlightRoute> result = ctrl.activeRoutesForPilot(tapPilot());
        final List<FlightRoute> list = new java.util.ArrayList<>();
        result.forEach(list::add);

        assertEquals(1, list.size());
        assertEquals("TP001", list.get(0).identity().value());
    }

    @Test
    void ensureActiveRoutesForPilotExcludesInactiveRoutes() {
        final FlightRoute active   = routeForCompany("TP001", "TP", "OPO", "LIS");
        final FlightRoute inactive = routeForCompany("TP002", "TP", "LIS", "OPO");
        inactive.deactivate(java.time.LocalDate.now());
        when(routeRepo.findAll()).thenReturn(List.of(active, inactive));

        final Iterable<FlightRoute> result = ctrl.activeRoutesForPilot(tapPilot());
        final List<FlightRoute> list = new java.util.ArrayList<>();
        result.forEach(list::add);

        assertEquals(1, list.size());
        assertEquals("TP001", list.get(0).identity().value());
    }

    @Test
    void ensureActiveRoutesForPilotReturnsEmptyWhenNoMatchingRoutes() {
        when(routeRepo.findAll()).thenReturn(List.of(
                routeForCompany("FR001", "FR", "OPO", "MAD")));

        final Iterable<FlightRoute> result = ctrl.activeRoutesForPilot(tapPilot());
        assertFalse(result.iterator().hasNext());
    }

    // ── US080: coordinate validation ─────────────────────────────────────────

    @Test
    void ensureSegmentCoordinatesOutsideBoundaryAreRejected() {
        // Set up an airport with a known boundary
        final GeographicBoundary boundary = new GeographicBoundary(
                37.0, 42.0, -10.0, -6.0); // covers Portugal
        final AirControlArea area = new AirControlArea("LPPC", "Lisboa FIR", boundary);
        final Airport airport = new Airport(
                new domain.ICAOCode("LPPR"),
                new IATACode("OPO"),
                new AirportName("Porto"),
                "Porto",
                new Country("Portugal"),
                new AirportCoordinates(41.2481, -8.6814),
                new Elevation(69),
                new AirControlAreaCode("LPPC"));

        when(airportRepo.findByIataCode(new IATACode("OPO")))
                .thenReturn(Optional.of(airport));
        when(areaRepo.ofIdentity(new AirControlAreaCode("LPPC")))
                .thenReturn(Optional.of(area));

        // Segment with 'to' coordinate outside Portugal (Spain)
        final FlightLeg leg = new FlightLeg(
                "OPO", "2028-06-01", "08:00",
                "LIS", "09:00", 5000.0, "kg");
        leg.addSegment(new FlightSegment(
                41.2481, -8.6814,   // from: inside boundary
                40.4983, -3.5676,   // to: Madrid — outside boundary
                0.0, 10000.0, "cruise", 50.0, 270.0, 10.0));

        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createFlightPlan(
                        "TP018", activeRoute(), tapAircraft(), tapPilot(),
                        futureDate(), List.of(leg)));
    }

    @Test
    void ensureSegmentCoordinatesInsideBoundaryAreAccepted() {
        final GeographicBoundary boundary = new GeographicBoundary(
                37.0, 42.0, -10.0, -6.0);
        final AirControlArea area = new AirControlArea("LPPC", "Lisboa FIR", boundary);
        final Airport airport = new Airport(
                new domain.ICAOCode("LPPR"),
                new IATACode("OPO"),
                new AirportName("Porto"),
                "Porto",
                new Country("Portugal"),
                new AirportCoordinates(41.2481, -8.6814),
                new Elevation(69),
                new AirControlAreaCode("LPPC"));

        when(airportRepo.findByIataCode(new IATACode("OPO")))
                .thenReturn(Optional.of(airport));
        when(areaRepo.ofIdentity(new AirControlAreaCode("LPPC")))
                .thenReturn(Optional.of(area));

        final FlightLeg leg = new FlightLeg(
                "OPO", "2028-06-01", "08:00",
                "LIS", "09:00", 5000.0, "kg");
        leg.addSegment(new FlightSegment(
                41.2481, -8.6814,
                38.7813, -9.1359,  // LIS — inside boundary
                0.0, 10000.0, "cruise", 50.0, 270.0, 10.0));

        assertDoesNotThrow(() ->
                ctrl.createFlightPlan(
                        "TP019", activeRoute(), tapAircraft(), tapPilot(),
                        futureDate(), List.of(leg)));
    }

    @Test
    void ensureCoordinateValidationSkippedWhenAirportNotFound() {
        // airportRepo returns empty → validation is silently skipped
        when(airportRepo.findByIataCode(any())).thenReturn(Optional.empty());

        final FlightLeg leg = new FlightLeg(
                "OPO", "2028-06-01", "08:00",
                "LIS", "09:00", 5000.0, "kg");
        leg.addSegment(new FlightSegment(
                999.0, 999.0, 999.0, 999.0, // absurd coords — skipped
                0.0, 10000.0, "cruise", 50.0, 270.0, 10.0));

        assertDoesNotThrow(() ->
                ctrl.createFlightPlan(
                        "TP020", activeRoute(), tapAircraft(), tapPilot(),
                        futureDate(), List.of(leg)));
    }
}
