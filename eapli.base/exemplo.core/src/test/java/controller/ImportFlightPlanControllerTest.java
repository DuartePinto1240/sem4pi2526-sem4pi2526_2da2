package controller;

import lprog.FlightLeg;
import lprog.FlightPlan;
import lprog.FlightPlanImporter;
import lprog.FlightPlanImporter.FlightPlanImportResult;
import lprog.FlightSegment;
import lprog.FlightPlanStatus;
import persistence.impl.inmemory.InMemoryFlightPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImportFlightPlanControllerTest {

    private ImportFlightPlanController ctrl;
    private InMemoryFlightPlanRepository repo;

    // ── DSL constants ─────────────────────────────────────────────────────────

    private static final String VALID_CONTENT =
            "flight TP001 {\n" +
            "    type: regular\n" +
            "    leg {\n" +
            "        departure: OPO 2026-05-17 14:00\n" +
            "        arrival:   LIS 15:00\n" +
            "        fuel:      5000 kg\n" +
            "        segment {\n" +
            "            phase: climb\n" +
            "            from: (41.2481, -8.6814)\n" +
            "            to:   (40.5000, -8.9000)\n" +
            "            altitude_range: [0, 10000]\n" +
            "            width: 50 m\n" +
            "            wind:  270 deg 10 m/s\n" +
            "        }\n" +
            "        segment {\n" +
            "            phase: cruise\n" +
            "            from: (40.5000, -8.9000)\n" +
            "            to:   (39.5000, -9.1000)\n" +
            "            altitude_range: [8000, 10000]\n" +
            "            width: 50 m\n" +
            "            wind:  270 deg 10 m/s\n" +
            "        }\n" +
            "        segment {\n" +
            "            phase: descent\n" +
            "            from: (39.5000, -9.1000)\n" +
            "            to:   (38.7813, -9.1359)\n" +
            "            altitude_range: [0, 10000]\n" +
            "            width: 50 m\n" +
            "            wind:  270 deg 10 m/s\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private static final String INVALID_CONTENT =
            "flight TP002 {\n" +
            "    type: regular\n" +
            "    leg {\n" +
            "        departure: OPO 2026-05-17 14:00\n" +
            "        arrival:   LIS 15:00\n" +
            "        fuel:      -500 kg\n" +
            "        segment {\n" +
            "            phase: climb\n" +
            "            from: (41.2481, -8.6814)\n" +
            "            to:   (40.5000, -8.9000)\n" +
            "            altitude_range: [0, 10000]\n" +
            "            width: 50 m\n" +
            "            wind:  270 deg 10 m/s\n" +
            "        }\n" +
            "        segment {\n" +
            "            phase: cruise\n" +
            "            from: (40.5000, -8.9000)\n" +
            "            to:   (39.5000, -9.1000)\n" +
            "            altitude_range: [8000, 10000]\n" +
            "            width: 50 m\n" +
            "            wind:  270 deg 10 m/s\n" +
            "        }\n" +
            "        segment {\n" +
            "            phase: descent\n" +
            "            from: (39.5000, -9.1000)\n" +
            "            to:   (38.7813, -9.1359)\n" +
            "            altitude_range: [0, 10000]\n" +
            "            width: 50 m\n" +
            "            wind:  270 deg 10 m/s\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private static final String MULTI_LEG_CONTENT =
            "flight TP003 {\n" +
            "    type: regular\n" +
            "    leg {\n" +
            "        departure: OPO 2026-06-15 07:00\n" +
            "        arrival:   LIS 08:00\n" +
            "        fuel:      4500 kg\n" +
            "        segment {\n" +
            "            phase: climb\n" +
            "            from: (41.2481, -8.6814)\n" +
            "            to:   (40.5000, -8.9000)\n" +
            "            altitude_range: [0, 10000]\n" +
            "            width: 50 m\n" +
            "            wind:  270 deg 12 m/s\n" +
            "        }\n" +
            "        segment {\n" +
            "            phase: cruise\n" +
            "            from: (40.5000, -8.9000)\n" +
            "            to:   (39.5000, -9.1000)\n" +
            "            altitude_range: [8000, 10000]\n" +
            "            width: 50 m\n" +
            "            wind:  270 deg 12 m/s\n" +
            "        }\n" +
            "        segment {\n" +
            "            phase: descent\n" +
            "            from: (39.5000, -9.1000)\n" +
            "            to:   (38.7813, -9.1359)\n" +
            "            altitude_range: [0, 10000]\n" +
            "            width: 50 m\n" +
            "            wind:  270 deg 10 m/s\n" +
            "        }\n" +
            "    }\n" +
            "    leg {\n" +
            "        departure: LIS 2026-06-15 09:00\n" +
            "        arrival:   FAO 09:45\n" +
            "        fuel:      3000 kg\n" +
            "        segment {\n" +
            "            phase: climb\n" +
            "            from: (38.7813, -9.1359)\n" +
            "            to:   (38.2000, -8.8000)\n" +
            "            altitude_range: [0, 8000]\n" +
            "            width: 40 m\n" +
            "            wind:  200 deg 8 m/s\n" +
            "        }\n" +
            "        segment {\n" +
            "            phase: cruise\n" +
            "            from: (38.2000, -8.8000)\n" +
            "            to:   (37.5000, -8.3000)\n" +
            "            altitude_range: [6000, 8000]\n" +
            "            width: 40 m\n" +
            "            wind:  200 deg 8 m/s\n" +
            "        }\n" +
            "        segment {\n" +
            "            phase: descent\n" +
            "            from: (37.5000, -8.3000)\n" +
            "            to:   (37.0144, -7.9659)\n" +
            "            altitude_range: [0, 8000]\n" +
            "            width: 40 m\n" +
            "            wind:  200 deg 6 m/s\n" +
            "        }\n" +
            "    }\n" +
            "}";

    @BeforeEach
    void setUp() {
        repo = new InMemoryFlightPlanRepository();
        ctrl = new ImportFlightPlanController(new FlightPlanImporter(), repo);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private File writeTempFile(final String content, final String extension) throws Exception {
        final Path path = Files.createTempFile("fplan", extension);
        Files.writeString(path, content);
        path.toFile().deleteOnExit();
        return path.toFile();
    }

    // ── format validation ─────────────────────────────────────────────────────

    @Test
    void ensureUnsupportedFileFormatIsRejected() throws Exception {
        final File file = writeTempFile(VALID_CONTENT, ".txt");
        final FlightPlanImportResult result = ctrl.importFromFile(file);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getMessage().contains("Unsupported file format")));
    }

    @Test
    void ensureUnsupportedFormatDoesNotPersist() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".csv"));
        assertEquals(0, repo.count());
    }

    // ── valid import ──────────────────────────────────────────────────────────

    @Test
    void ensureValidFpFileReturnsSuccessResult() throws Exception {
        assertTrue(ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp")).isSuccess());
    }

    @Test
    void ensureValidFpFilePersistsFlightPlanInRepository() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        assertTrue(repo.existsByFlightId("TP001"));
        assertEquals(1, repo.count());
    }

    @Test
    void ensureValidFpFileCreatesFlightPlanWithDraftStatus() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        assertEquals(FlightPlanStatus.DRAFT,
                repo.findByFlightId("TP001").orElseThrow().status());
    }

    @Test
    void ensureValidFpFileCreatesFlightPlanWithCorrectData() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        final FlightPlan saved = repo.findByFlightId("TP001").orElseThrow();
        assertEquals("TP001",   saved.flightId());
        assertEquals("regular", saved.flightType());
        assertEquals(1,         saved.legs().size());
    }

    // ── invalid import ────────────────────────────────────────────────────────

    @Test
    void ensureInvalidFpFileReturnsFailureResult() throws Exception {
        final FlightPlanImportResult result =
                ctrl.importFromFile(writeTempFile(INVALID_CONTENT, ".fp"));
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void ensureInvalidFpFileDoesNotPersistFlightPlan() throws Exception {
        ctrl.importFromFile(writeTempFile(INVALID_CONTENT, ".fp"));
        assertEquals(0, repo.count());
    }

    // ── non-existent file ─────────────────────────────────────────────────────

    @Test
    void ensureNonExistentFileReturnsFailureResult() {
        assertFalse(ctrl.importFromFile(new File("/nonexistent/path/file.fp")).isSuccess());
    }

    @Test
    void ensureNonExistentFileDoesNotPersistAnything() {
        ctrl.importFromFile(new File("/nonexistent/path/file.fp"));
        assertEquals(0, repo.count());
    }

    // ── duplicate flight ID ───────────────────────────────────────────────────

    @Test
    void ensureDuplicateFlightIdIsSkippedOnSecondImport() throws Exception {
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        ctrl.importFromFile(file);
        ctrl.importFromFile(file);
        assertEquals(1, repo.count());
    }

    // ── available files ───────────────────────────────────────────────────────

    @Test
    void ensureAvailableFlightPlanFilesReturnsEmptyWhenFolderMissing() {
        assertNotNull(ctrl.availableFlightPlanFiles());
    }

    // ── DSL content storage (US081) ───────────────────────────────────────────

    @Test
    void ensureValidFpFileStoresDslContentInFlightPlan() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        final FlightPlan saved = repo.findByFlightId("TP001").orElseThrow();
        assertNotNull(saved.dslContent());
        assertTrue(saved.dslContent().contains("TP001"));
    }

    @Test
    void ensureInvalidFpFileDoesNotStoreDslContent() throws Exception {
        ctrl.importFromFile(writeTempFile(INVALID_CONTENT, ".fp"));
        assertFalse(repo.existsByFlightId("TP002"));
    }

    // ── segment mapping ───────────────────────────────────────────────────────

    @Test
    void ensureImportedLegHasCorrectNumberOfSegments() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        assertEquals(3, repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).segments().size(),
                "Leg should have 3 segments (climb/cruise/descent)");
    }

    @Test
    void ensureImportedClimbSegmentHasCorrectFromCoordinates() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        final FlightSegment seg = repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).segments().get(0); // climb
        assertEquals(41.2481, seg.fromLat(), 0.0001);
        assertEquals(-8.6814, seg.fromLon(), 0.0001);
    }

    @Test
    void ensureImportedDescentSegmentHasCorrectToCoordinates() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        final FlightSegment seg = repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).segments().get(2); // descent
        assertEquals(38.7813, seg.toLat(), 0.0001);
        assertEquals(-9.1359, seg.toLon(), 0.0001);
    }

    @Test
    void ensureImportedCruiseSegmentHasCorrectAltitudeRange() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        final FlightSegment seg = repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).segments().get(1); // cruise
        assertEquals(8000.0,  seg.altitudeMin(), 0.001);
        assertEquals(10000.0, seg.altitudeMax(), 0.001);
    }

    @Test
    void ensureImportedSegmentHasCorrectPhase() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        final var segs = repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).segments();
        assertEquals("climb",   segs.get(0).phase());
        assertEquals("cruise",  segs.get(1).phase());
        assertEquals("descent", segs.get(2).phase());
    }

    @Test
    void ensureImportedSegmentHasCorrectWidth() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        final FlightSegment seg = repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).segments().get(1); // cruise
        assertEquals(50.0, seg.widthMeters(), 0.001);
    }

    @Test
    void ensureImportedSegmentHasCorrectWind() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        final FlightSegment seg = repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).segments().get(1); // cruise
        assertEquals(270.0, seg.windAngleDeg(), 0.001);
        assertEquals(10.0,  seg.windSpeedMs(),  0.001);
    }

    // ── multi-leg import ──────────────────────────────────────────────────────

    @Test
    void ensureMultiLegFlightImportsAllLegs() throws Exception {
        ctrl.importFromFile(writeTempFile(MULTI_LEG_CONTENT, ".fp"));
        assertEquals(2, repo.findByFlightId("TP003").orElseThrow().legs().size());
    }

    @Test
    void ensureMultiLegFlightImportsThreeSegmentsForEachLeg() throws Exception {
        ctrl.importFromFile(writeTempFile(MULTI_LEG_CONTENT, ".fp"));
        final FlightPlan saved = repo.findByFlightId("TP003").orElseThrow();
        assertEquals(3, saved.legs().get(0).segments().size());
        assertEquals(3, saved.legs().get(1).segments().size());
    }

    @Test
    void ensureMultiLegCruiseSegmentAltitudeRangeImportedCorrectly() throws Exception {
        ctrl.importFromFile(writeTempFile(MULTI_LEG_CONTENT, ".fp"));
        final FlightSegment cruise = repo.findByFlightId("TP003").orElseThrow()
                .legs().get(0).segments().get(1); // first leg, cruise
        assertEquals(8000.0,  cruise.altitudeMin(), 0.001);
        assertEquals(10000.0, cruise.altitudeMax(), 0.001);
    }

    // ── leg data integrity ────────────────────────────────────────────────────

    @Test
    void ensureImportedLegHasCorrectDepartureAirport() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        assertEquals("OPO", repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).departureAirport());
    }

    @Test
    void ensureImportedLegHasCorrectArrivalAirport() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        assertEquals("LIS", repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).arrivalAirport());
    }

    @Test
    void ensureImportedLegHasCorrectFuel() throws Exception {
        ctrl.importFromFile(writeTempFile(VALID_CONTENT, ".fp"));
        assertEquals(5000.0, repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).fuelQuantity(), 0.001);
    }
}
