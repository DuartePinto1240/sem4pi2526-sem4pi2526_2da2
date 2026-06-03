package controller;

import domain.AirTransportCompany;
import domain.aircraft.Aircraft;
import domain.aircraft.CabinConfiguration;
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
import lprog.FlightLeg;
import lprog.FlightPlan;
import lprog.FlightPlanImporter;
import lprog.FlightPlanStatus;
import lprog.FlightSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.impl.inmemory.InMemoryFlightPlanRepository;
import repositories.AirControlAreaRepository;
import repositories.AircraftRepository;
import repositories.AirportRepository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for US080 and US081.
 *
 * These tests exercise the full flow without mocking the repository or
 * the DSL generator/importer — only external repositories (airport, area,
 * route, aircraft, pilot) are mocked where needed.
 */
class FlightPlanIntegrationTest {

    private InMemoryFlightPlanRepository repo;
    private CreateFlightPlanController   createCtrl;
    private ImportFlightPlanController   importCtrl;

    // ── fixtures ──────────────────────────────────────────────────────────────

    private AirTransportCompany tapCompany() {
        return new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
    }

    private FlightRoute tapRoute() {
        return new FlightRoute("TP001", "TP", "OPO", "LIS",
                new RecurringSchedule(List.of(
                        new ScheduleEntry(DayOfWeek.MONDAY,
                                LocalTime.of(8, 0), LocalTime.of(9, 0)))));
    }

    private Aircraft tapAircraft() {
        final EngineModel engine = new EngineModel(
                "CFM56", "CFM", MotorizationType.TURBOFAN,
                new Thrust(100, 120), new TSFC(0.016));
        final FlightCharacteristics chars = new FlightCharacteristics(
                41000, 60000, 78000, 25000, 12000, 220, 5000, 124, 0.026, 1.4);
        final AircraftModel model = new AircraftModel(
                "A320", "Airbus", AircraftType.PASSENGER, chars, List.of(engine));
        return new Aircraft("CS-TTP", "Portugal", "TP", model,
                new CabinConfiguration(150, 12, 0, 6));
    }

    private Pilot tapPilot() {
        final EngineModel engine = new EngineModel(
                "CFM56", "CFM", MotorizationType.TURBOFAN,
                new Thrust(100, 120), new TSFC(0.016));
        final FlightCharacteristics chars = new FlightCharacteristics(
                41000, 60000, 78000, 25000, 12000, 220, 5000, 124, 0.026, 1.4);
        final AircraftModel model = new AircraftModel(
                "A320", "Airbus", AircraftType.PASSENGER, chars, List.of(engine));
        return new Pilot("João Silva", "joao@tap.pt", "912345678",
                tapCompany(), List.of(model));
    }

    private List<FlightLeg> legWithSegment() {
        final FlightLeg leg = new FlightLeg(
                "OPO", "2028-06-01", "08:00",
                "LIS", "09:00", 5000.0, "kg");
        leg.addSegment(new FlightSegment(
                41.2481, -8.6814, 38.7813, -9.1359,
                List.of(9000.0), 50.0, 270.0, 10.0));
        return List.of(leg);
    }

    private File writeTempFp(final String content) throws Exception {
        final Path p = Files.createTempFile("integration", ".fp");
        Files.writeString(p, content);
        p.toFile().deleteOnExit();
        return p.toFile();
    }

    @BeforeEach
    void setUp() {
        repo = new InMemoryFlightPlanRepository();

        // US080 controller — no airport/area validation needed for most integration tests
        final AirportRepository airportRepo = mock(AirportRepository.class);
        when(airportRepo.findByIataCode(any())).thenReturn(Optional.empty());
        final AirControlAreaRepository areaRepo = mock(AirControlAreaRepository.class);
        final AircraftRepository       aircraftRepo = mock(AircraftRepository.class);

        createCtrl = new CreateFlightPlanController(
                mock(repositories.FlightRouteRepository.class),
                aircraftRepo,
                mock(repositories.PilotRepository.class),
                repo, airportRepo, areaRepo);

        // US081 controller — real importer, shared repo
        importCtrl = new ImportFlightPlanController(new FlightPlanImporter(), repo);
    }

    // ── US080 full flow ───────────────────────────────────────────────────────

    @Test
    void us080_createPlan_persistedWithDraftAndDsl() {
        final FlightPlan plan = createCtrl.createFlightPlan(
                "TP-INT-001", tapRoute(), tapAircraft(), tapPilot(),
                LocalDateTime.now().plusDays(30), legWithSegment());

        // persisted
        assertTrue(repo.existsByFlightId("TP-INT-001"));
        // status
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
        // DSL generated
        assertNotNull(plan.dslContent());
        assertTrue(plan.dslContent().contains("TP-INT-001"));
        assertTrue(plan.dslContent().contains("segment"));
    }



    @Test
    void us080_twoPlansWithDifferentIds_bothPersisted() {
        createCtrl.createFlightPlan("TP-INT-A", tapRoute(), tapAircraft(), tapPilot(),
                LocalDateTime.now().plusDays(30), legWithSegment());
        createCtrl.createFlightPlan("TP-INT-B", tapRoute(), tapAircraft(), tapPilot(),
                LocalDateTime.now().plusDays(30), legWithSegment());

        assertEquals(2, repo.count());
        assertTrue(repo.existsByFlightId("TP-INT-A"));
        assertTrue(repo.existsByFlightId("TP-INT-B"));
    }

    // ── US081 full flow ───────────────────────────────────────────────────────

    private static final String SINGLE_LEG_DSL =
            "flight TP-IMP-001 {\n" +
            "    type: regular\n" +
            "    leg {\n" +
            "        departure: OPO 2026-06-01 08:00\n" +
            "        arrival:   LIS 09:00\n" +
            "        fuel:      5000 kg\n" +
            "        segment {\n" +
            "            from: (41.2481, -8.6814)\n" +
            "            to:   (38.7813, -9.1359)\n" +
            "            altitude_slots: [8000, 9000]\n" +
            "            width: 50 m\n" +
            "            wind:  270 deg 10 m/s\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private static final String MULTI_LEG_DSL =
            "flight TP-IMP-002 {\n" +
            "    type: regular\n" +
            "    leg {\n" +
            "        departure: OPO 2026-06-15 07:00\n" +
            "        arrival:   LIS 08:00\n" +
            "        fuel:      4500 kg\n" +
            "        segment {\n" +
            "            from: (41.2481, -8.6814)\n" +
            "            to:   (38.7813, -9.1359)\n" +
            "            altitude_slots: [8000]\n" +
            "            width: 50 m\n" +
            "            wind:  270 deg 12 m/s\n" +
            "        }\n" +
            "    }\n" +
            "    leg {\n" +
            "        departure: LIS 2026-06-15 09:00\n" +
            "        arrival:   FAO 09:45\n" +
            "        fuel:      3000 kg\n" +
            "        segment {\n" +
            "            from: (38.7813, -9.1359)\n" +
            "            to:   (37.0144, -7.9659)\n" +
            "            altitude_slots: [6000]\n" +
            "            width: 40 m\n" +
            "            wind:  200 deg 8 m/s\n" +
            "        }\n" +
            "    }\n" +
            "}";

    @Test
    void us081_importSingleLeg_persistedWithSegment() throws Exception {
        importCtrl.importFromFile(writeTempFp(SINGLE_LEG_DSL));

        final FlightPlan plan = repo.findByFlightId("TP-IMP-001").orElseThrow();
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
        assertEquals(1, plan.legs().size());
        assertEquals(1, plan.legs().get(0).segments().size());
        assertNotNull(plan.dslContent());
    }

    @Test
    void us081_importSingleLeg_segmentDataCorrect() throws Exception {
        importCtrl.importFromFile(writeTempFp(SINGLE_LEG_DSL));

        final FlightSegment seg = repo.findByFlightId("TP-IMP-001").orElseThrow()
                .legs().get(0).segments().get(0);
        assertEquals(41.2481, seg.fromLat(), 0.0001);
        assertEquals(-8.6814, seg.fromLon(), 0.0001);
        assertEquals(38.7813, seg.toLat(),   0.0001);
        assertEquals(-9.1359, seg.toLon(),   0.0001);
        assertEquals(List.of(8000.0, 9000.0), seg.altitudeSlots());
        assertEquals(50.0,  seg.widthMeters(),  0.001);
        assertEquals(270.0, seg.windAngleDeg(), 0.001);
        assertEquals(10.0,  seg.windSpeedMs(),  0.001);
    }

    @Test
    void us081_importMultiLeg_twoLegsWithSegments() throws Exception {
        importCtrl.importFromFile(writeTempFp(MULTI_LEG_DSL));

        final FlightPlan plan = repo.findByFlightId("TP-IMP-002").orElseThrow();
        assertEquals(2, plan.legs().size());
        assertEquals(1, plan.legs().get(0).segments().size());
        assertEquals(1, plan.legs().get(1).segments().size());
    }

    @Test
    void us081_importMultiLeg_secondLegSegmentCorrect() throws Exception {
        importCtrl.importFromFile(writeTempFp(MULTI_LEG_DSL));

        final FlightSegment seg = repo.findByFlightId("TP-IMP-002").orElseThrow()
                .legs().get(1).segments().get(0);
        assertEquals(38.7813, seg.fromLat(), 0.0001);
        assertEquals(37.0144, seg.toLat(),   0.0001);
        assertEquals(List.of(6000.0), seg.altitudeSlots());
        assertEquals(200.0, seg.windAngleDeg(), 0.001);
        assertEquals(8.0,   seg.windSpeedMs(),  0.001);
    }

    @Test
    void us081_importTwice_secondImportSkipped() throws Exception {
        final File file = writeTempFp(SINGLE_LEG_DSL);
        importCtrl.importFromFile(file);
        importCtrl.importFromFile(file);
        assertEquals(1, repo.count());
    }

    // ── US080 + US081 coexistence ─────────────────────────────────────────────

    @Test
    void us080AndUs081_canCoexistInSameRepository() throws Exception {
        // Create manually
        createCtrl.createFlightPlan("TP-MAN-001", tapRoute(), tapAircraft(), tapPilot(),
                LocalDateTime.now().plusDays(30), legWithSegment());

        // Import from file
        importCtrl.importFromFile(writeTempFp(SINGLE_LEG_DSL));

        assertEquals(2, repo.count());
        assertTrue(repo.existsByFlightId("TP-MAN-001"));
        assertTrue(repo.existsByFlightId("TP-IMP-001"));
    }

    @Test
    void us080AndUs081_bothPlansHaveDslContent() throws Exception {
        final FlightPlan manual = createCtrl.createFlightPlan(
                "TP-MAN-002", tapRoute(), tapAircraft(), tapPilot(),
                LocalDateTime.now().plusDays(30), legWithSegment());

        importCtrl.importFromFile(writeTempFp(SINGLE_LEG_DSL));
        final FlightPlan imported = repo.findByFlightId("TP-IMP-001").orElseThrow();

        assertNotNull(manual.dslContent(),   "Manually created plan must have DSL");
        assertNotNull(imported.dslContent(), "Imported plan must have DSL");
    }
}
