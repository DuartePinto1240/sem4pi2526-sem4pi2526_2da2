package controller;

import application.ImportWeatherResult;
import domain.*;
import dto.WeatherDataImportDTO;
import application.WeatherDataReader;
import application.WeatherDataReaderFactory;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import persistence.PersistenceContext;
import repositories.AirControlAreaRepository;
import repositories.WeatherDataRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application-layer controller for US042 — Import Bulk Weather Data.
 */
public class ImportBulkWeatherDataController {

    private final AuthorizationService authz;
    private final AirControlAreaRepository areaRepo;
    private final WeatherDataRepository weatherRepo;
    private final WeatherDataReaderFactory readerFactory;
    private final WeatherDataFactory weatherDataFactory;

    /** Production constructor — wires collaborators from framework registries. */
    public ImportBulkWeatherDataController() {
        this.authz = AuthzRegistry.authorizationService();
        this.areaRepo = PersistenceContext.getInstance().airControlAreaRepository();
        this.weatherRepo = PersistenceContext.getInstance().weatherDataRepository();
        this.readerFactory = new WeatherDataReaderFactory();
        this.weatherDataFactory = new WeatherDataFactory();
    }

    /**
     * Package-visible constructor for unit tests — allows collaborator injection.
     */
    ImportBulkWeatherDataController(final AuthorizationService authz, final AirControlAreaRepository areaRepo,
                                    final WeatherDataRepository weatherRepo, final WeatherDataReaderFactory readerFactory,
                                    final WeatherDataFactory weatherDataFactory) {
        this.authz = authz;
        this.areaRepo = areaRepo;
        this.weatherRepo = weatherRepo;
        this.readerFactory = readerFactory;
        this.weatherDataFactory = weatherDataFactory;
    }

    /**
     * Imports all weather data records from the given file.
     *
     * @param filePath path to the data file
     * @return summary of the operation (counts + per-record errors)
     * @throws IllegalArgumentException if the file extension is not supported
     */
    public ImportWeatherResult importWeatherData(final String filePath) {
        // authorisation
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.WEATHER_PERSON);

        // select strategy via factory
        final WeatherDataReader reader = readerFactory.getReader(filePath);

        // read file into DTOs
        final List<WeatherDataImportDTO> dtos;
        try {
            dtos = reader.read(filePath);
        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Could not read file '" + filePath + "': " + e.getMessage(), e);
        }

        // process each record
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

        // delegate aggregate construction to the Domain Factory
        final WeatherData weatherData;
        try {
            weatherData = weatherDataFactory.create(dto);
        } catch (final IllegalArgumentException e) {
            result.recordFailure(lineRef + " – " + e.getMessage());
            return;
        }

        // sub-area must be inside parent boundary
        if (!weatherData.subArea().isInsideBoundary(area.boundary())) {
            result.recordFailure(lineRef + " – sub-area lies outside area '"
                    + dto.areaCode + "' boundary");
            return;
        }

        // no conflicting record
        if (weatherRepo.existsConflicting(code, weatherData.subArea(), weatherData.period())) {
            result.recordFailure(lineRef + " – conflicts with an existing record for area '" + dto.areaCode + "'");
            return;
        }

        // persist
        weatherRepo.save(weatherData);
        result.recordSuccess();
    }
}
