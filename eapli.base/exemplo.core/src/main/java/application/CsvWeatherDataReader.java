package application;

import dto.WeatherDataImportDTO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete Strategy that reads weather data records from a delimiter-separated
 * file (CSV, TSV, or semicolon-separated) exported from a spreadsheet.
 *
 * <p>Expected column order (12 columns, first line is a header):</p>
 * <pre>
 * ACA | maxLat | minLon | minLat | maxLon | Alt_inf | Alt_Sup |
 * Direction | Value | Day (dd/MM/yyyy) | Start (HH:mm) | End (HH:mm)
 * </pre>
 */
public class CsvWeatherDataReader implements WeatherDataReader {

    private static final int EXPECTED_COLUMNS = 12;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    /** Column separator decided by the factory before instantiation. */
    private final String separator;

    /**
     * @param separator the column separator character(s) to use when splitting
     * lines — determined by {@link WeatherDataReaderFactory}
     */
    public CsvWeatherDataReader(final String separator) {
        this.separator = separator;
    }

    @Override
    public List<WeatherDataImportDTO> read(final String filePath) throws IOException {
        final List<WeatherDataImportDTO> result = new ArrayList<>();

        try (final BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.readLine(); // skip header
            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                final String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                result.add(parseLine(trimmed, lineNumber));
            }
        }

        return result;
    }

    // private helpers

    /**
     * Parses one data line into a {@link WeatherDataImportDTO}.
     *
     * <p>Column mapping (0-based):</p>
     * <pre>
     *  0  ACA          → areaCode
     *  1  Lat. (max)   → subMaxLat
     *  2  Long. (min)  → subMinLon
     *  3  Lat. (min)   → subMinLat
     *  4  Long. (max)  → subMaxLon
     *  5  Alt_inf      → minAltitude
     *  6  Alt_Sup      → maxAltitude
     *  7  Direction    → windDirection
     *  8  Value        → windSpeed
     *  9  Day          → date  (dd/MM/yyyy)
     * 10  Star         → start time (HH:mm)
     * 11  End          → end time   (HH:mm)
     * </pre>
     */
    private WeatherDataImportDTO parseLine(final String line, final int lineNumber) {
        final String[] parts = line.split(separator, -1);

        if (parts.length != EXPECTED_COLUMNS) {
            throw new IllegalArgumentException("expected " + EXPECTED_COLUMNS + " columns but found "
                    + parts.length + " — raw: [" + line + "]");
        }

        try {
            final String areaCode      = parts[0].trim();
            final double subMaxLat     = parseEuropeanDouble(parts[1]);
            final double subMinLon     = parseEuropeanDouble(parts[2]);
            final double subMinLat     = parseEuropeanDouble(parts[3]);
            final double subMaxLon     = parseEuropeanDouble(parts[4]);
            final double minAltitude   = parseEuropeanDouble(parts[5]);
            final double maxAltitude   = parseEuropeanDouble(parts[6]);
            final double windDirection = parseEuropeanDouble(parts[7]);
            final double windSpeed     = parseEuropeanDouble(parts[8]);

            final LocalDate date       = LocalDate.parse(parts[9].trim(), DATE_FMT);
            final LocalTime startTime  = LocalTime.parse(parts[10].trim(), TIME_FMT);
            final LocalTime endTime    = LocalTime.parse(parts[11].trim(), TIME_FMT);

            final LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
            final LocalDateTime endDateTime   = LocalDateTime.of(date, endTime);

            return new WeatherDataImportDTO(areaCode, subMinLat, subMaxLat, subMinLon, subMaxLon,
                    minAltitude, maxAltitude, startDateTime, endDateTime, windDirection, windSpeed, lineNumber);

        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("invalid numeric value — " + e.getMessage() + " — raw: [" + line + "]");
        } catch (final DateTimeParseException e) {
            throw new IllegalArgumentException("invalid date/time — " + e.getMessage() + " — raw: [" + line + "]");
        }
    }

    /**
     * Parses a decimal number accepting both comma and dot as decimal separator.
     * European spreadsheet exports use comma by default.
     */
    private double parseEuropeanDouble(final String raw) {
        return Double.parseDouble(raw.trim().replace(',', '.'));
    }
}
