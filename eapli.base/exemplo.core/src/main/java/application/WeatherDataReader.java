package application;

import dto.WeatherDataImportDTO;

import java.util.List;

/**
 * Strategy interface for reading weather data records from a file.
 *
 * <p>Each concrete implementation encapsulates the parsing algorithm for one
 * specific file format (e.g. CSV, JSON, XML).</p>
 *
 * <p>Implementations must:</p>
 * <ul>
 *   <li>Return one {@link WeatherDataImportDTO} per data record in the file.</li>
 *   <li>Set {@code sourceLineNumber} to the 1-based line number in the file,
 *       so the controller can include it in per-record error messages.</li>
 *   <li>Skip header lines internally — the returned list contains data only.</li>
 *   <li>Throw {@link java.io.IOException} if the file cannot be opened or read.</li>
 *   <li>Throw {@link IllegalArgumentException} for structural/format errors
 *       that prevent parsing a record (e.g. wrong column count).</li>
 * </ul>
 */
public interface WeatherDataReader {

    /**
     * Reads all weather data records from the given file.
     *
     * @param filePath absolute or relative path to the data file
     * @return list of DTOs, one per record (never {@code null})
     * @throws java.io.IOException      if the file cannot be read
     * @throws IllegalArgumentException if the file format is structurally invalid
     */
    List<WeatherDataImportDTO> read(String filePath) throws Exception;
}
