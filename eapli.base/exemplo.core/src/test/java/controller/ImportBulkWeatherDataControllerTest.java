package controller;

import application.ImportWeatherResult;
import dto.WeatherDataImportDTO;
import application.WeatherDataReaderFactory;
import domain.WeatherDataFactory;
import domain.*;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthenticatedException;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.PersistenceContext;
import persistence.impl.inmemory.InMemoryAirControlAreaRepository;
import persistence.impl.inmemory.InMemoryWeatherDataRepository;
import repositories.AirControlAreaRepository;
import repositories.WeatherDataRepository;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ImportBulkWeatherDataController} (US042).
 */
class ImportBulkWeatherDataControllerTest {

    // persistence unit
    private static final String PERSISTENCE_UNIT = "eapli.base.AlSafePU";

    // test user
    private static final String WP_EMAIL = "wp.bulk@alsafe.com";
    private static final String WP_PASSWORD = "Weather1234";

    // test area
    private static final String AREA_CODE = "121";
    private static final double AREA_MIN_LAT = 36.0, AREA_MAX_LAT = 44.0;
    private static final double AREA_MIN_LON = -10.0, AREA_MAX_LON = 5.0;

    // valid record values (inside area boundary)
    private static final LocalDateTime START = LocalDateTime.of(2026, 6, 22, 5, 0);
    private static final LocalDateTime END   = LocalDateTime.of(2026, 6, 22, 7, 30);

    // collaborators
    private AirControlAreaRepository areaRepo;
    private WeatherDataRepository weatherRepo;
    private ImportBulkWeatherDataController controller;

    @TempDir
    Path tempDir;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setUp() throws Exception {
        // Bootstrap AuthzRegistry
        try {
            AuthzRegistry.configure(new JpaAutoTxUserRepository(PERSISTENCE_UNIT, new HashMap<>()), new AlSafePasswordPolicy(), new PlainTextEncoder());
        } catch (final Exception ignored) { /* already configured */ }

        // Reset PersistenceContext singleton
        final Field instanceField = PersistenceContext.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        areaRepo = new InMemoryAirControlAreaRepository();
        weatherRepo = new InMemoryWeatherDataRepository();

        final PersistenceContext ctx = PersistenceContext.getInstance();
        injectField(ctx, "airControlAreaRepository", areaRepo);
        injectField(ctx, "weatherDataRepository",    weatherRepo);

        // Register and log in as WeatherPerson
        AuthzRegistry.authorizationService().clearSession();
        try {
            final Set<Role> roles = new HashSet<>();
            roles.add(BaseRoles.WEATHER_PERSON);
            AuthzRegistry.userService().registerNewUser(WP_EMAIL, WP_PASSWORD, "Alice", "Bulk", WP_EMAIL, roles);
        } catch (final Exception ignored) { /* already registered */ }
        new LoginController().login(WP_EMAIL, WP_PASSWORD);

        // Seed area
        areaRepo.save(new AirControlArea(AREA_CODE, "Lisboa ACC", new GeographicBoundary(AREA_MIN_LAT, AREA_MAX_LAT, AREA_MIN_LON, AREA_MAX_LON)));
        controller = new ImportBulkWeatherDataController();
    }


    // valid file imports all records

    @Test
    void ensureValidCsvImportsAllRecords() throws IOException {
        final Path csv = writeCsv(
                csvRow(AREA_CODE, 43.8, -9.8, 40.2, -7.9, 0,    1000, 90,  25, START, END),
                csvRow(AREA_CODE, 40.2, -9.8, 37.0, -7.9, 0,    1000, 105, 18, START, END),
                csvRow(AREA_CODE, 43.8, -7.9, 41.1, -2.3, 1001, 2000, 45,  12, START, END)
        );

        final ImportWeatherResult result = controller.importWeatherData(csv.toString());

        assertEquals(3, result.importedCount(), "All 3 records should be imported");
        assertEquals(0, result.rejectedCount());
        assertEquals(3, weatherRepo.count());
    }

    // invalid rows skipped; valid rows still imported

    @Test
    void ensureInvalidRowsAreSkippedButOthersImported() throws IOException {
        final Path csv = writeCsv(
                csvRow(AREA_CODE, 43.8, -9.8, 40.2, -7.9, 0, 1000, 90,  5.0,  START, END),
                csvRow(AREA_CODE, 40.2, -9.8, 37.0, -7.9, 0, 1000, 105, -1.0, START, END), // negative speed
                csvRow(AREA_CODE, 43.8, -7.9, 41.1, -2.3, 0, 1000, 45,  12.0, START, END)
        );

        final ImportWeatherResult result = controller.importWeatherData(csv.toString());

        assertEquals(2, result.importedCount());
        assertEquals(1, result.rejectedCount());
        assertEquals(1, result.errors().size());
    }

    @Test
    void ensureNonExistentAreaIsRejectedPerRecord() throws IOException {
        final Path csv = writeCsv(csvRow("9999", 43.8, -9.8, 40.2, -7.9, 0, 1000, 90, 25, START, END));

        final ImportWeatherResult result = controller.importWeatherData(csv.toString());

        assertEquals(0, result.importedCount());
        assertEquals(1, result.rejectedCount());
        assertTrue(result.errors().get(0).contains("does not exist"));
    }

    @Test
    void ensureSubAreaOutsideBoundaryIsRejected() throws IOException {
        // maxLat=10, minLat=5 — outside AREA [36..44]
        final Path csv = writeCsv(csvRow(AREA_CODE, 10.0, -9.8, 5.0, -7.9, 0, 1000, 90, 25, START, END));

        final ImportWeatherResult result = controller.importWeatherData(csv.toString());

        assertEquals(0, result.importedCount());
        assertEquals(1, result.rejectedCount());
    }

    @Test
    void ensureConflictingRecordIsRejected() throws IOException {
        // Pre-load a conflicting record
        final AirControlAreaCode code = new AirControlAreaCode(AREA_CODE);
        weatherRepo.save(new WeatherData(code, new WeatherSubArea(new GeographicBoundary(40.2, 43.8, -9.8, -7.9),
                        new AltitudeRange(0.0, 1000.0)), new ObservationPeriod(START, END), new WindCondition(90.0, 25.0)));

        final Path csv = writeCsv(csvRow(AREA_CODE, 43.8, -9.8, 40.2, -7.9, 0, 1000, 90, 25, START, END));

        final ImportWeatherResult result = controller.importWeatherData(csv.toString());

        assertEquals(0, result.importedCount());
        assertEquals(1, result.rejectedCount());
        assertTrue(result.errors().get(0).contains("conflicts"));
    }

    // controller is format-agnostic (Strategy pattern)

    @Test
    void ensureControllerWorksWithAnyReaderStrategy() {
        // Stub factory — subclasses WeatherDataReaderFactory (concrete class)
        // and returns an in-memory reader with pre-built DTOs, no file needed
        final WeatherDataImportDTO dto1 = makeDto(40.2, 43.8, 1);
        final WeatherDataImportDTO dto2 = makeDto(37.0, 40.2, 2);

        final WeatherDataReaderFactory stubFactory = new WeatherDataReaderFactory() {
            @Override
            public application.WeatherDataReader getReader(final String filePath) {
                return path -> List.of(dto1, dto2);
            }
        };

        final ImportBulkWeatherDataController ctrl = new ImportBulkWeatherDataController(AuthzRegistry.authorizationService(),
                        areaRepo, weatherRepo, stubFactory, new WeatherDataFactory());

        final ImportWeatherResult result = ctrl.importWeatherData("irrelevant.csv");

        assertEquals(2, result.importedCount());
        assertEquals(0, result.rejectedCount());
    }

    // only WEATHER_PERSON may import
    @Test
    void ensureUnauthenticatedUserCannotImport() {
        AuthzRegistry.authorizationService().clearSession();

        assertThrows(UnauthenticatedException.class, () -> controller.importWeatherData("any.csv"));
    }

    // ImportResult summary

    @Test
    void ensureImportResultTotalReadIsCorrect() throws IOException {
        final Path csv = writeCsv(
                csvRow(AREA_CODE, 43.8, -9.8, 40.2, -7.9, 0, 1000, 90,  5.0,  START, END),
                csvRow(AREA_CODE, 40.2, -9.8, 37.0, -7.9, 0, 1000, 105, -1.0, START, END) // invalid
        );

        final ImportWeatherResult result = controller.importWeatherData(csv.toString());

        assertEquals(2, result.totalRead());
    }

    @Test
    void ensureEmptyFileProducesZeroCounts() throws IOException {
        final Path csv = writeCsv(/* no data rows */);

        final ImportWeatherResult result = controller.importWeatherData(csv.toString());

        assertEquals(0, result.importedCount());
        assertEquals(0, result.rejectedCount());
        assertEquals(0, result.totalRead());
    }

    // Unsupported format

    @Test
    void ensureUnsupportedFormatThrows() {
        assertThrows(IllegalArgumentException.class, () -> controller.importWeatherData("data.xml"));
    }

    // Helpers

    /**
     * Writes a semicolon-separated CSV file with the real client header,
     * followed by the given data rows.
     */
    private Path writeCsv(final String... dataRows) throws IOException {
        final Path file = tempDir.resolve("weather_test.csv");
        final List<String> lines = new ArrayList<>();
        lines.add("ACA;Lat.;Long.;Lat.;Long.;Alt_inf (ft);Alt_Sup (ft);Direction (degree);Value (knot);Day;Star;End");
        for (final String row : dataRows) lines.add(row);
        Files.write(file, lines);
        return file;
    }

    /**
     * Builds one semicolon-separated data row matching the real client format.
     *
     * <p>Column order: ACA ; maxLat ; minLon ; minLat ; maxLon ;
     * altInf ; altSup ; direction ; speed ; dd/MM/yyyy ; HH:mm ; HH:mm</p>
     */
    private String csvRow(final String area, final double maxLat, final double minLon, final double minLat, final double maxLon,
                          final double altInf, final double altSup, final double direction, final double speed, final LocalDateTime start, final LocalDateTime end) {
        return String.format("%s;%.1f;%.1f;%.1f;%.1f;%.1f;%.1f;%.1f;%.1f;%s;%s;%s", area, maxLat, minLon, minLat, maxLon,
                altInf, altSup, direction, speed, start.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                start.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), end.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
    }

    /** Creates a valid {@link WeatherDataImportDTO} inside the test area boundary. */
    private WeatherDataImportDTO makeDto(final double minLat, final double maxLat, final int lineNo) {
        return new WeatherDataImportDTO(AREA_CODE, minLat, maxLat, -9.8, -7.9, 0.0, 1000.0,
                START, END, 90.0, 25.0, lineNo);
    }

    private static void injectField(final Object target, final String fieldName, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
