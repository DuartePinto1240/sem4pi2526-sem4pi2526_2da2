package report;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Template Method base for all AISafe reports.
 *
 * <p>Subclasses only need to implement {@link #generateBody()} — the header
 * and footer are shared across all report types, guaranteeing consistent
 * branding and structure (US112 AC US112.3 / US112.5).</p>
 */
public abstract class AbstractReport {

    /** Builds and returns the full report as a single String. */
    public final String generate() {
        return generateHeader() + generateBody() + generateFooter();
    }

    /** Writes the full report to the given file path. */
    public final void generateToFile(final String filePath) {
        try (final PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.print(generate());
        } catch (final IOException e) {
            throw new IllegalStateException("Could not write report to file: " + filePath, e);
        }
    }

    /** Common header — same for every report type. */
    public final String generateHeader() {
        return "========================================\n"
             + "  AISafe — Flight Control Management\n"
             + "  " + reportTitle() + "\n"
             + "  Period : " + reportPeriod() + "\n"
             + "  Generated: " + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n"
             + "========================================\n\n";
    }

    /** Report-specific body — implemented by each concrete subclass. */
    protected abstract String generateBody();

    /** Common footer — same for every report type. */
    public final String generateFooter() {
        return "\n========================================\n"
             + "  AISafe Flight Control Management System\n"
             + "  Report generated automatically — do not alter\n"
             + "========================================\n";
    }

    /** Human-readable title shown in the header (e.g. "Monthly Statistics Report"). */
    protected abstract String reportTitle();

    /** Human-readable period shown in the header (e.g. "May 2026"). */
    protected abstract String reportPeriod();
}
