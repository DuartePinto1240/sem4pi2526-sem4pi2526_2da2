package domain;

import dto.WeatherDataImportDTO;

/**
 * Domain Factory responsible for constructing a {@link WeatherData} aggregate
 */
public class WeatherDataFactory {

    /**
     * Constructs a {@link WeatherData} aggregate from the data in the given DTO.
     *
     * @param dto raw data read from the import file
     * @return a fully constructed {@link WeatherData} ready to be persisted
     * @throws IllegalArgumentException if any value object invariant is violated
     */
    public WeatherData create(final WeatherDataImportDTO dto) {
        final AirControlAreaCode code = new AirControlAreaCode(dto.areaCode);
        final GeographicBoundary subBoundary = new GeographicBoundary(dto.subMinLat, dto.subMaxLat, dto.subMinLon, dto.subMaxLon);
        final AltitudeRange altRange = new AltitudeRange(dto.minAltitude, dto.maxAltitude);
        final WeatherSubArea subArea = new WeatherSubArea(subBoundary, altRange);
        final ObservationPeriod period = new ObservationPeriod(dto.startDateTime, dto.endDateTime);
        final WindCondition wind = new WindCondition(dto.windDirection, dto.windSpeed);
        return new WeatherData(code, subArea, period, wind);
    }
}
