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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImportFlightPlanControllerTest {

    private ImportFlightPlanController ctrl;
    private InMemoryFlightPlanRepository repo;

    private static final String VALID_CONTENT =
            "flight TP001 {\n" +
                    "    type: regular\n" +
                    "    leg {\n" +
                    "        departure: OPO 2026-05-17 14:00\n" +
                    "        arrival:   LIS 15:00\n" +
                    "        fuel:      5000 kg\n" +
                    "        segment {\n" +
                    "            from: (41.2481, -8.6814)\n" +
                    "            to:   (38.7813, -9.1359)\n" +
                    "            altitude_slots: [8000]\n" +
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
                    "            from: (41.2481, -8.6814)\n" +
                    "            to:   (38.7813, -9.1359)\n" +
                    "            altitude_slots: [8000]\n" +
                    "            width: 50 m\n" +
                    "            wind:  270 deg 10 m/s\n" +
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
        // Arrange — .txt is not supported
        final File file = writeTempFile(VALID_CONTENT, ".txt");
        // Act
        final FlightPlanImportResult result = ctrl.importFromFile(file);
        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getMessage().contains("Unsupported file format")));
    }

    @Test
    void ensureUnsupportedFormatDoesNotPersist() throws Exception {
        // Arrange
        final File file = writeTempFile(VALID_CONTENT, ".csv");
        // Act
        ctrl.importFromFile(file);
        // Assert
        assertEquals(0, repo.count());
    }

    // ── valid import ──────────────────────────────────────────────────────────

    @Test
    void ensureValidFpFileReturnsSuccessResult() throws Exception {
        // Arrange
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        // Act
        final FlightPlanImportResult result = ctrl.importFromFile(file);
        // Assert
        assertTrue(result.isSuccess());
    }

    @Test
    void ensureValidFpFilePersistsFlightPlanInRepository() throws Exception {
        // Arrange
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        // Act
        ctrl.importFromFile(file);
        // Assert
        assertTrue(repo.existsByFlightId("TP001"));
        assertEquals(1, repo.count());
    }

    @Test
    void ensureValidFpFileCreatesFlightPlanWithDraftStatus() throws Exception {
        // Arrange
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        // Act
        ctrl.importFromFile(file);
        // Assert
        final FlightPlan saved = repo.findByFlightId("TP001").orElseThrow();
        assertEquals(FlightPlanStatus.DRAFT, saved.status());
    }

    @Test
    void ensureValidFpFileCreatesFlightPlanWithCorrectData() throws Exception {
        // Arrange
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        // Act
        ctrl.importFromFile(file);
        // Assert
        final FlightPlan saved = repo.findByFlightId("TP001").orElseThrow();
        assertEquals("TP001",   saved.flightId());
        assertEquals("regular", saved.flightType());
        assertEquals(1,         saved.legs().size());
    }

    // ── invalid import ────────────────────────────────────────────────────────

    @Test
    void ensureInvalidFpFileReturnsFailureResult() throws Exception {
        // Arrange
        final File file = writeTempFile(INVALID_CONTENT, ".fp");
        // Act
        final FlightPlanImportResult result = ctrl.importFromFile(file);
        // Assert
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void ensureInvalidFpFileDoesNotPersistFlightPlan() throws Exception {
        // Arrange
        final File file = writeTempFile(INVALID_CONTENT, ".fp");
        // Act
        ctrl.importFromFile(file);
        // Assert
        assertEquals(0, repo.count());
    }

    // ── non-existent file ─────────────────────────────────────────────────────

    @Test
    void ensureNonExistentFileReturnsFailureResult() {
        // Arrange
        final File file = new File("/nonexistent/path/file.fp");
        // Act
        final FlightPlanImportResult result = ctrl.importFromFile(file);
        // Assert
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void ensureNonExistentFileDoesNotPersistAnything() {
        // Arrange + Act
        ctrl.importFromFile(new File("/nonexistent/path/file.fp"));
        // Assert
        assertEquals(0, repo.count());
    }

    // ── duplicate flight ID ───────────────────────────────────────────────────

    @Test
    void ensureDuplicateFlightIdIsSkippedOnSecondImport() throws Exception {
        // Arrange
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        ctrl.importFromFile(file);
        assertEquals(1, repo.count());
        // Act — import same file again
        ctrl.importFromFile(file);
        // Assert — still only one entry
        assertEquals(1, repo.count());
    }

    // ── available files ───────────────────────────────────────────────────────

    @Test
    void ensureAvailableFlightPlanFilesReturnsEmptyWhenFolderMissing() {
        // Arrange + Act — flightplans/ folder likely doesn't exist in test environment
        final List<File> files = ctrl.availableFlightPlanFiles();
        // Assert — should return empty list, not throw
        assertNotNull(files);
    }

    // ── dsl content storage (US081) ───────────────────────────────────────────

    @Test
    void ensureValidFpFileStoresDslContentInFlightPlan() throws Exception {
        // Arrange
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        // Act
        ctrl.importFromFile(file);
        // Assert
        final FlightPlan saved = repo.findByFlightId("TP001").orElseThrow();
        assertNotNull(saved.dslContent(),
                "dslContent should be stored after import");
        assertTrue(saved.dslContent().contains("TP001"));
    }

    @Test
    void ensureInvalidFpFileDoesNotStoreDslContent() throws Exception {
        final File file = writeTempFile(INVALID_CONTENT, ".fp");
        ctrl.importFromFile(file);
        assertFalse(repo.existsByFlightId("TP002"));
    }

    // ── segment mapping (US081 core fix) ──────────────────────────────────────

    @Test
    void ensureImportedLegHasCorrectNumberOfSegments() throws Exception {
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        ctrl.importFromFile(file);
        final FlightPlan saved = repo.findByFlightId("TP001").orElseThrow();
        final FlightLeg leg = saved.legs().get(0);
        assertEquals(1, leg.segments().size(),
                "Leg should have exactly 1 segment imported from DSL");
    }

    @Test
    void ensureImportedSegmentHasCorrectFromCoordinates() throws Exception {
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        ctrl.importFromFile(file);
        final FlightSegment seg = repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).segments().get(0);
        assertEquals(41.2481, seg.fromLat(), 0.0001);
        assertEquals(-8.6814, seg.fromLon(), 0.0001);
    }

    @Test
    void ensureImportedSegmentHasCorrectToCoordinates() throws Exception {
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        ctrl.importFromFile(file);
        final FlightSegment seg = repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).segments().get(0);
        assertEquals(38.7813, seg.toLat(), 0.0001);
        assertEquals(-9.1359, seg.toLon(), 0.0001);
    }

    @Test
    void ensureImportedSegmentHasCorrectAltitudeSlots() throws Exception {
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        ctrl.importFromFile(file);
        final FlightSegment seg = repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).segments().get(0);
        assertEquals(List.of(8000.0), seg.altitudeSlots());
    }

    @Test
    void ensureImportedSegmentHasCorrectWidth() throws Exception {
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        ctrl.importFromFile(file);
        final FlightSegment seg = repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).segments().get(0);
        assertEquals(50.0, seg.widthMeters(), 0.001);
    }

    @Test
    void ensureImportedSegmentHasCorrectWind() throws Exception {
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        ctrl.importFromFile(file);
        final FlightSegment seg = repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).segments().get(0);
        assertEquals(270.0, seg.windAngleDeg(), 0.001);
        assertEquals(10.0,  seg.windSpeedMs(),  0.001);
    }

    // ── multi-leg import ───────────────────────────────────────────────────────

    private static final String MULTI_LEG_CONTENT =
            "flight TP002 {\n" +
            "    type: regular\n" +
            "    leg {\n" +
            "        departure: OPO 2026-06-15 07:00\n" +
            "        arrival:   LIS 08:00\n" +
            "        fuel:      4500 kg\n" +
            "        segment {\n" +
            "            from: (41.2481, -8.6814)\n" +
            "            to:   (38.7813, -9.1359)\n" +
            "            altitude_slots: [8000, 10000]\n" +
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
    void ensureMultiLegFlightImportsAllLegs() throws Exception {
        final File file = writeTempFile(MULTI_LEG_CONTENT, ".fp");
        ctrl.importFromFile(file);
        final FlightPlan saved = repo.findByFlightId("TP002").orElseThrow();
        assertEquals(2, saved.legs().size());
    }

    @Test
    void ensureMultiLegFlightImportsSegmentsForEachLeg() throws Exception {
        final File file = writeTempFile(MULTI_LEG_CONTENT, ".fp");
        ctrl.importFromFile(file);
        final FlightPlan saved = repo.findByFlightId("TP002").orElseThrow();
        assertEquals(1, saved.legs().get(0).segments().size());
        assertEquals(1, saved.legs().get(1).segments().size());
    }

    @Test
    void ensureMultiLegSegmentAltitudeSlotsImportedCorrectly() throws Exception {
        final File file = writeTempFile(MULTI_LEG_CONTENT, ".fp");
        ctrl.importFromFile(file);
        final FlightSegment seg0 = repo.findByFlightId("TP002").orElseThrow()
                .legs().get(0).segments().get(0);
        assertEquals(List.of(8000.0, 10000.0), seg0.altitudeSlots());
    }

    // ── leg data integrity ─────────────────────────────────────────────────────

    @Test
    void ensureImportedLegHasCorrectDepartureAirport() throws Exception {
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        ctrl.importFromFile(file);
        assertEquals("OPO", repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).departureAirport());
    }

    @Test
    void ensureImportedLegHasCorrectArrivalAirport() throws Exception {
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        ctrl.importFromFile(file);
        assertEquals("LIS", repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).arrivalAirport());
    }

    @Test
    void ensureImportedLegHasCorrectFuel() throws Exception {
        final File file = writeTempFile(VALID_CONTENT, ".fp");
        ctrl.importFromFile(file);
        assertEquals(5000.0, repo.findByFlightId("TP001").orElseThrow()
                .legs().get(0).fuelQuantity(), 0.001);
    }
}