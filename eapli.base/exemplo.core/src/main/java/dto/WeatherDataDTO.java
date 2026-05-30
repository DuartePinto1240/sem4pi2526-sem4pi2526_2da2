package dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for a weather data record (US043).
 *
 * <p>Created by the {@link domain.WeatherData} aggregate via {@code toDTO()} </p>
 */
public final class WeatherDataDTO {

    public final Long          id;
    public final String        areaCode;
    public final double        subMinLat;
    public final double        subMaxLat;
    public final double        subMinLon;
    public final double        subMaxLon;
    public final double        minAltitude;
    public final double        maxAltitude;
    public final LocalDateTime startDateTime;
    public final LocalDateTime endDateTime;
    public final double        windDirection;
    public final double        windSpeed;

    public WeatherDataDTO(final Long id, final String areaCode, final double subMinLat,
                          final double subMaxLat, final double subMinLon, final double subMaxLon,
                          final double minAltitude, final double maxAltitude,
                          final LocalDateTime startDateTime, final LocalDateTime endDateTime,
                          final double windDirection, final double windSpeed) {
        this.id            = id;
        this.areaCode      = areaCode;
        this.subMinLat     = subMinLat;
        this.subMaxLat     = subMaxLat;
        this.subMinLon     = subMinLon;
        this.subMaxLon     = subMaxLon;
        this.minAltitude   = minAltitude;
        this.maxAltitude   = maxAltitude;
        this.startDateTime = startDateTime;
        this.endDateTime   = endDateTime;
        this.windDirection = windDirection;
        this.windSpeed     = windSpeed;
    }

    @Override
    public String toString() {
        return String.format("[%d] Area=%-6s | SubArea Lat[%.2f,%.2f] Lon[%.2f,%.2f] "
                        + "Alt[%.0f,%.0f]m | Period: %s → %s | Wind: %.1f° / %.2f m/s",
                id, areaCode, subMinLat, subMaxLat, subMinLon, subMaxLon, minAltitude, maxAltitude,
                startDateTime, endDateTime, windDirection, windSpeed);
    }
}
