package controller;

import application.ImportWeatherResult;
import application.WeatherDataReader;
import application.WeatherDataReaderFactory;
import domain.*;
import dto.WeatherDataImportDTO;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import persistence.PersistenceContext;
import repositories.AirControlAreaRepository;
import repositories.WeatherDataRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application-layer controller for US042
 *
 * <p>Orchestrates the bulk import of weather data records from a file</p>
 */
public class ImportBulkWeatherDataController {

    private final AuthorizationService     authz;
    private final AirControlAreaRepository areaRepo;
    private final WeatherDataRepository    weatherRepo;
    private final WeatherDataReaderFactory readerFactory;

    public ImportBulkWeatherDataController() {
        this.authz         = AuthzRegistry.authorizationService();
        this.areaRepo      = PersistenceContext.getInstance().airControlAreaRepository();
        this.weatherRepo   = PersistenceContext.getInstance().weatherDataRepository();
        this.readerFactory = new WeatherDataReaderFactory();
    }

    ImportBulkWeatherDataController(final AuthorizationService authz, final AirControlAreaRepository areaRepo,
                                    final WeatherDataRepository weatherRepo, final WeatherDataReaderFactory readerFactory) {
        this.authz         = authz;
        this.areaRepo      = areaRepo;
        this.weatherRepo   = weatherRepo;
        this.readerFactory = readerFactory;
    }

    /**
     * Imports all weather data records from the given file.
     *
     * @param filePath path to the data file
     * @return summary of the operation (counts + per-record errors)
     * @throws IllegalArgumentException if the file extension is not supported
     */
    public ImportWeatherResult importWeatherData(final String filePath) {
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.WEATHER_PERSON);

        final WeatherDataReader reader = readerFactory.getReader(filePath);

        final List<WeatherDataImportDTO> dtos;
        try {
            dtos = reader.read(filePath);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Could not read file '" + filePath + "': " + e.getMessage(), e);
        }

        final ImportWeatherResult result = new ImportWeatherResult();
        for (final WeatherDataImportDTO dto : dtos) {
            processRecord(dto, result);
        }

        return result;
    }

    /**
     * Attempts to build and persist one {@link WeatherData} from a DTO.
     * Failures are recorded in {@code result}; the loop continues regardless.
     */
    private void processRecord(final WeatherDataImportDTO dto, final ImportWeatherResult result) {
        final String lineRef = "line " + dto.sourceLineNumber;

        // area must exist
        final AirControlAreaCode code = new AirControlAreaCode(dto.areaCode);
        final Optional<AirControlArea> areaOpt = areaRepo.ofIdentity(code);
        if (areaOpt.isEmpty()) {
            result.recordFailure(lineRef + " – area '" + dto.areaCode + "' does not exist");
            return;
        }
        final AirControlArea area = areaOpt.get();

        // build value objects (constructors enforce their own invariants)
        final WeatherSubArea subArea;
        try {
            final GeographicBoundary subBoundary = new GeographicBoundary(dto.subMinLat, dto.subMaxLat, dto.subMinLon, dto.subMaxLon);
            final AltitudeRange altRange = new AltitudeRange(dto.minAltitude, dto.maxAltitude);
            subArea = new WeatherSubArea(subBoundary, altRange);
        } catch (final IllegalArgumentException e) {
            result.recordFailure(lineRef + " – invalid sub-area: " + e.getMessage());
            return;
        }

        // sub-area must be inside parent boundary
        if (!subArea.isInsideBoundary(area.boundary())) {
            result.recordFailure(lineRef + " – sub-area lies outside area '" + dto.areaCode + "' boundary");
            return;
        }

        // build period and wind
        final ObservationPeriod period;
        final WindCondition wind;
        try {
            period = new ObservationPeriod(dto.startDateTime, dto.endDateTime);
            wind   = new WindCondition(dto.windDirection, dto.windSpeed);
        } catch (final IllegalArgumentException e) {
            result.recordFailure(lineRef + " – " + e.getMessage());
            return;
        }

        // no conflicting record
        if (weatherRepo.existsConflicting(code, subArea, period)) {
            result.recordFailure(lineRef + " – conflicts with an existing record for area '" + dto.areaCode + "'");
            return;
        }

        // persist
        weatherRepo.save(new WeatherData(code, subArea, period, wind));
        result.recordSuccess();
    }
}
