package controller;

import domain.simulationreport.SimulationReport;
import persistence.PersistenceContext;
import report.MonthlyStatisticsReport;
import repositories.SimulationReportRepository;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for US112 — Monthly Report Generation.
 *
 * <p>Loads all {@link SimulationReport} entries, filters by the requested
 * month and year, builds a {@link MonthlyStatisticsReport} and writes it
 * to a file.</p>
 */
public class MonthlyReportController {

    private static final DateTimeFormatter FILE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final String REPORTS_OUT_DIR = System.getProperty("monthly.reports.dir", "reports");

    private final SimulationReportRepository repo;

    public MonthlyReportController() {
        this.repo = PersistenceContext.getInstance().simulationReportRepository();
    }

    /** Package-private constructor for unit tests (dependency injection). */
    MonthlyReportController(final SimulationReportRepository repo) {
        this.repo = repo;
    }

    /**
     * Generates a monthly statistics report for the given year and month,
     * saves it to a file and returns the file path.
     *
     * @param year  calendar year (e.g. 2026)
     * @param month calendar month 1–12
     * @return path of the generated report file
     */
    public String generateMonthlyReport(final int year, final int month) {
        validateMonth(month);
        final List<SimulationReport> filtered = reportsForMonth(year, month);

        final MonthlyStatisticsReport report =
                new MonthlyStatisticsReport(year, month, filtered);

        final File outDir = new File(REPORTS_OUT_DIR);
        if (!outDir.exists()) outDir.mkdirs();

        final String filename = REPORTS_OUT_DIR + File.separator
                + String.format("report_monthly_%04d_%02d_%s.txt",
                        year, month, LocalDateTime.now().format(FILE_FMT));

        report.generateToFile(filename);
        return filename;
    }

    /**
     * Returns the report content as a String (used by the UI for on-screen display).
     */
    public String previewMonthlyReport(final int year, final int month) {
        validateMonth(month);
        return new MonthlyStatisticsReport(year, month, reportsForMonth(year, month))
                .generate();
    }

    private static void validateMonth(final int month) {
        if (month < 1 || month > 12)
            throw new IllegalArgumentException("Month must be between 1 and 12, got: " + month);
    }

    // Filters all persisted SimulationReports by year+month of generatedAt
    private List<SimulationReport> reportsForMonth(final int year, final int month) {
        final List<SimulationReport> result = new ArrayList<>();
        for (final SimulationReport r : repo.findAll()) {
            final LocalDateTime at = r.generatedAt();
            if (at.getYear() == year && at.getMonthValue() == month) {
                result.add(r);
            }
        }
        return result;
    }
}
