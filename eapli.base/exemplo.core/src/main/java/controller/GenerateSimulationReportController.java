// ─── controller/GenerateSimulationReportController.java ───────────────────

package controller;

import domain.simulationreport.*;
import persistence.PersistenceContext;
import repositories.SimulationReportRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GenerateSimulationReportController {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy",
                    java.util.Locale.ENGLISH);

    private final SimulationReportRepository repo;
    private final String reportsDir;

    public GenerateSimulationReportController() {
        this.repo       = PersistenceContext.getInstance().simulationReportRepository();
        this.reportsDir = System.getProperty("reports.dir", resolveReportsDir());
    }

    /** Package-private — for unit tests (inject repo only, keeps default dir). */
    GenerateSimulationReportController(final SimulationReportRepository repo) {
        this.repo       = repo;
        this.reportsDir = System.getProperty("reports.dir", resolveReportsDir());
    }

    /**
     * Tries several candidate paths for the Scomp reports directory,
     * returning the first one that exists. Falls back to "../Scomp".
     */
    private static String resolveReportsDir() {
        final String[] candidates = { "../Scomp", "../../Scomp", "Scomp" };
        for (final String candidate : candidates) {
            if (new File(candidate).isDirectory()) return candidate;
        }
        return "../Scomp";
    }

    /** Package-private — for unit tests (inject repo + custom reports directory). */
    GenerateSimulationReportController(final SimulationReportRepository repo,
                                        final String reportsDir) {
        this.repo       = repo;
        this.reportsDir = reportsDir;
    }

    public List<String> availableReportFiles() {
        final File dir = new File(reportsDir);
        final File[] files = dir.listFiles(
                f -> f.isFile() && f.getName().startsWith("simulation_report_")
                        && f.getName().endsWith(".txt"));
        if (files == null) return new ArrayList<>();
        return Arrays.stream(files)
                .map(File::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    public SimulationReport generateReport(final String filename) {
        final File file = new File(reportsDir, filename);
        if (!file.exists())
            throw new IllegalArgumentException("Report file not found: " + filename);

        try (final BufferedReader reader =
                     new BufferedReader(new FileReader(file))) {

            int    totalFlights   = 0;
            int    totalViolations = 0;
            boolean pass          = false;
            LocalDateTime generatedAt = LocalDateTime.now();

            final List<FlightExecutionStatus> statuses   = new ArrayList<>();
            final List<SafetyViolationEvent>  violations = new ArrayList<>();

            String line;
            String section = "";

            // violation parsing state
            double  vTimestamp = 0;
            int     vFlightI   = -1;
            int     vFlightJ   = -1;
            double  vLatI = 0, vLonI = 0, vAltI = 0;
            double  vVxI = 0,  vVyI = 0,  vVzI = 0;
            double  vLatJ = 0, vLonJ = 0, vAltJ = 0;
            double  vVxJ = 0,  vVyJ = 0,  vVzJ = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("Generated:")) {
                    try {
                        final String dateStr = line.substring("Generated:".length()).trim();
                        generatedAt = LocalDateTime.parse(dateStr, FMT);
                    } catch (final Exception ignored) {
                        generatedAt = LocalDateTime.now();
                    }
                } else if (line.startsWith("Total flights")) {
                    totalFlights = Integer.parseInt(
                            line.split(":")[1].trim());
                } else if (line.startsWith("Total violations")) {
                    totalViolations = Integer.parseInt(
                            line.split(":")[1].trim());
                } else if (line.startsWith("Result")) {
                    pass = line.split(":")[1].trim().equalsIgnoreCase("PASS");
                } else if (line.startsWith("--- Flight Statuses")) {
                    section = "STATUSES";
                } else if (line.startsWith("--- Safety Violation")) {
                    section = "VIOLATIONS";
                } else if (section.equals("STATUSES") && line.startsWith("Flight")) {
                    final String[] parts = line.split(":");
                    final int flightId = Integer.parseInt(
                            parts[0].replace("Flight", "").trim());
                    final String statusStr = parts[1].trim();
                    final FlightExecutionStatus.Status status;
                    switch (statusStr) {
                        case "VIOLATED": status = FlightExecutionStatus.Status.VIOLATED; break;
                        case "ABORTED":  status = FlightExecutionStatus.Status.ABORTED;  break;
                        default:         status = FlightExecutionStatus.Status.OK;       break;
                    }
                    statuses.add(new FlightExecutionStatus(flightId, status));
                } else if (section.equals("VIOLATIONS")) {
                    if (line.startsWith("Violation #")) {
                        vTimestamp = 0; vFlightI = -1; vFlightJ = -1;
                    } else if (line.startsWith("Timestamp")) {
                        vTimestamp = Double.parseDouble(
                                line.split(":")[1].trim().replace(" s", ""));
                    } else if (line.startsWith("Flight 1")) {
                        vFlightI = Integer.parseInt(
                                line.split(":")[1].trim());
                    } else if (line.startsWith("Flight 2")) {
                        vFlightJ = Integer.parseInt(
                                line.split(":")[1].trim());
                        // position line comes after Flight 1 or Flight 2
                    } else if (line.startsWith("Lat=") && vFlightJ == -1) {
                        final double[] pos = parseLatLonAlt(line);
                        vLatI = pos[0]; vLonI = pos[1]; vAltI = pos[2];
                    } else if (line.startsWith("Velocity") && vFlightJ == -1) {
                        final double[] vel = parseVelocity(line);
                        vVxI = vel[0]; vVyI = vel[1]; vVzI = vel[2];
                    } else if (line.startsWith("Lat=") && vFlightJ >= 0) {
                        final double[] pos = parseLatLonAlt(line);
                        vLatJ = pos[0]; vLonJ = pos[1]; vAltJ = pos[2];
                    } else if (line.startsWith("Velocity") && vFlightJ >= 0) {
                        final double[] vel = parseVelocity(line);
                        vVxJ = vel[0]; vVyJ = vel[1]; vVzJ = vel[2];
                        // all data collected — save violation
                        violations.add(new SafetyViolationEvent(
                                vTimestamp,
                                vFlightI, vFlightJ,
                                vLatI, vLonI, vAltI,
                                vVxI,  vVyI,  vVzI,
                                vLatJ, vLonJ, vAltJ,
                                vVxJ,  vVyJ,  vVzJ));
                    }
                }
            }

            // ensure we have at least one status per flight
            if (statuses.isEmpty()) {
                for (int i = 0; i < totalFlights; i++)
                    statuses.add(new FlightExecutionStatus(i,
                            FlightExecutionStatus.Status.OK));
            }

            final SimulationReportId reportId =
                    SimulationReportId.of(filename.replace(".txt", ""));

            final SimulationReport report = new SimulationReport(
                    reportId,
                    generatedAt,
                    Math.max(totalFlights, 1),
                    statuses,
                    violations,
                    pass ? OverallValidationResult.pass() : OverallValidationResult.fail());

            return repo.save(report);

        } catch (final IOException e) {
            throw new IllegalArgumentException(
                    "Could not read report file: " + e.getMessage());
        }
    }

    private double[] parseLatLonAlt(final String line) {
        // "Lat=41.2481 Lon=-8.6814 Alt=1000.0"
        final double[] result = new double[3];
        try {
            result[0] = Double.parseDouble(
                    line.split("Lat=")[1].split(" ")[0]);
            result[1] = Double.parseDouble(
                    line.split("Lon=")[1].split(" ")[0]);
            result[2] = Double.parseDouble(
                    line.split("Alt=")[1].split(" ")[0]);
        } catch (final Exception ignored) {}
        return result;
    }

    private double[] parseVelocity(final String line) {
        // "Velocity : vx=41.2481 vy=-8.6814 vz=1000.0"
        final double[] result = new double[3];
        try {
            result[0] = Double.parseDouble(
                    line.split("vx=")[1].split(" ")[0]);
            result[1] = Double.parseDouble(
                    line.split("vy=")[1].split(" ")[0]);
            result[2] = Double.parseDouble(
                    line.split("vz=")[1].split(" ")[0]);
        } catch (final Exception ignored) {}
        return result;
    }
}