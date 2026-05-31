package dto;

import application.WeatherDataReader;
import controller.ImportBulkWeatherDataController;

import java.time.LocalDateTime;

/**
 * Data Transfer Object used by {@link WeatherDataReader} strategies to carry
 * raw data parsed from a file to {@link ImportBulkWeatherDataController}.
 *
 * <p>The {@code sourceLineNumber} field allows the controller to include the
 * original line number in per-record error messages.</p>
 */
public class WeatherDataImportDTO {

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

    /** based line number in the source file, for error reporting. */
    public final int           sourceLineNumber;

    public WeatherDataImportDTO(final String areaCode, final double subMinLat,
                          final double subMaxLat, final double subMinLon, final double subMaxLon, final double minAltitude,
                          final double maxAltitude, final LocalDateTime startDateTime, final LocalDateTime endDateTime,
                          final double windDirection, final double windSpeed, final int sourceLineNumber) {
        this.areaCode        = areaCode;
        this.subMinLat       = subMinLat;
        this.subMaxLat       = subMaxLat;
        this.subMinLon       = subMinLon;
        this.subMaxLon       = subMaxLon;
        this.minAltitude     = minAltitude;
        this.maxAltitude     = maxAltitude;
        this.startDateTime   = startDateTime;
        this.endDateTime     = endDateTime;
        this.windDirection   = windDirection;
        this.windSpeed       = windSpeed;
        this.sourceLineNumber = sourceLineNumber;
    }
}
