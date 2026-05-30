package application;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import dto.WeatherDataDTO;
import repositories.AirControlAreaRepository;
import repositories.WeatherDataRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Application Service for querying weather data (US043).
 */
public class WeatherDataQueryService {

    private final AirControlAreaRepository areaRepo;
    private final WeatherDataRepository    weatherRepo;

    public WeatherDataQueryService(final AirControlAreaRepository areaRepo, final WeatherDataRepository weatherRepo) {
        this.areaRepo    = areaRepo;
        this.weatherRepo = weatherRepo;
    }

    /**
     * Returns all registered air control areas.
     *
     * @return iterable of all {@link AirControlArea} instances
     */
    public Iterable<AirControlArea> allAreas() {
        return areaRepo.findAll();
    }

    /**
     * Returns weather data records for a given area that cover the specified date.
     *
     * <p>Area existence is validated here — if the area code is unknown,
     * an {@link IllegalArgumentException} is thrown.</p>
     *
     * @param areaCode string area code (case-insensitive)
     * @param date     the day to filter by
     * @return list of {@link WeatherDataDTO}, possibly empty
     * @throws IllegalArgumentException if the area code does not exist
     */
    public List<WeatherDataDTO> weatherDataByAreaAndDate(final String areaCode, final LocalDate date) {
        final AirControlAreaCode code = new AirControlAreaCode(areaCode);

        areaRepo.ofIdentity(code).orElseThrow(() -> new IllegalArgumentException("Air control area '" + areaCode + "' does not exist."));

        final List<WeatherDataDTO> result = new ArrayList<>();
        weatherRepo.findByAreaAndDate(code, date).forEach(wd -> result.add(wd.toDTO()));
        return result;
    }
}
