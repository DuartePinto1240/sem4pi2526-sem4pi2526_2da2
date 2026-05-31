package application;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Simple Factory that selects and configures the appropriate
 * {@link WeatherDataReader} strategy for a given file.
 *
 * <p>Adding a new format requires only a new {@link WeatherDataReader}
 * implementation and one extra {@code if} block here.</p>
 */
public class WeatherDataReaderFactory {

    /**
     * Returns a configured {@link WeatherDataReader} for the given file.
     *
     * @param filePath path to the data file
     * @return a ready-to-use reader strategy
     * @throws IllegalArgumentException if the file extension is not supported
     * or the file cannot be read for detection
     */
    public WeatherDataReader getReader(final String filePath) {
        final String lower = filePath.toLowerCase();

        if (lower.endsWith(".csv") || lower.endsWith(".tsv") || lower.endsWith(".txt")) {
            final String separator = detectSeparator(filePath);
            return new CsvWeatherDataReader(separator);
        }

        // Future formats:
        // if (lower.endsWith(".xlsx")) return new XlsxWeatherDataReader();

        final int dotIndex = filePath.lastIndexOf('.');
        final String ext = dotIndex >= 0 ? filePath.substring(dotIndex) : "(no extension)";
        throw new IllegalArgumentException("Unsupported file format: '" + ext + "'. Supported formats: .csv, .tsv, .txt");
    }

    // private helpers

    /**
     * Opens the file, reads only the header line, and infers the column
     * separator from its content.

     *
     * @param filePath path to the file
     * @return the detected separator string
     * @throws IllegalArgumentException if the file cannot be opened
     */
    private String detectSeparator(final String filePath) {
        try (final BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            final String header = reader.readLine();
            if (header == null) return ","; // empty file — default, reader will handle it

            if (header.contains("\t")) return "\t";
            if (header.contains(";"))  return ";";
            return ",";

        } catch (final IOException e) {
            throw new IllegalArgumentException("Could not read file '" + filePath + "' to detect format: " + e.getMessage(), e);
        }
    }
}
