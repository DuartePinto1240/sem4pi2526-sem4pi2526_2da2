// ─── controller/ExportWeatherDataController.java ─────────────────────────

package controller;

import domain.WeatherData;
import dto.WeatherDataDTO;
import persistence.PersistenceContext;
import repositories.WeatherDataRepository;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ExportWeatherDataController {

    private final WeatherDataRepository repo;
    private static final String DEFAULT_PATH = "/home/guest/aisafe/CW_wind.csv";

    public ExportWeatherDataController() {
        this.repo = PersistenceContext.getInstance().weatherDataRepository();
    }

    ExportWeatherDataController(final WeatherDataRepository repo) {
        this.repo = repo;
    }

    public List<WeatherDataDTO> availableWeatherData() {
        final List<WeatherDataDTO> dtos = new ArrayList<>();
        repo.findAll().forEach(w -> dtos.add(w.toDTO()));
        return dtos;
    }

    public String exportToCsv() {
        return exportToCsv(DEFAULT_PATH);
    }

    public String exportToCsv(final String path) {
        final List<WeatherData> records = new ArrayList<>();
        repo.findAll().forEach(records::add);

        if (records.isEmpty())
            throw new IllegalStateException(
                    "No weather data found. Import weather data first.");

        try (final PrintWriter pw =
                     new PrintWriter(new FileWriter(path))) {

            pw.println("ACA,Lat.,Long.,Lat.,Long.," +
                    "Alt_inf (ft),Alt_Sup (ft)," +
                    "Direction (degree),Value (knot)," +
                    "Day,Star,End");

            for (final WeatherData w : records) {
                pw.printf("%s,%.6f,%.6f,%.6f,%.6f,%.1f,%.1f,%.1f,%.4f,%s,%s,%s%n",
                        w.areaCode().code(),
                        w.subArea().subBoundary().maxLat(),
                        w.subArea().subBoundary().minLon(),
                        w.subArea().subBoundary().minLat(),
                        w.subArea().subBoundary().maxLon(),
                        w.subArea().altitudeRange().minAltitude(),
                        w.subArea().altitudeRange().maxAltitude(),
                        w.wind().windDirection(),
                        w.wind().windSpeed(),
                        w.period().startDateTime().toLocalDate(),
                        w.period().startDateTime().toLocalTime(),
                        w.period().endDateTime().toLocalTime());
            }

            System.out.printf("[EXPORT] %d weather records exported to %s%n",
                    records.size(), path);
            return path;

        } catch (final IOException e) {
            throw new IllegalStateException(
                    "Could not write weather CSV: " + e.getMessage());
        }
    }
}