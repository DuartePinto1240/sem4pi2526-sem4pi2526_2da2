package ui;

import controller.MonthlyReportController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;

/**
 * UI for US112 — Monthly Report Generation.
 */
public class MonthlyReportUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final MonthlyReportController ctrl = new MonthlyReportController();

        System.out.println("\n--- Monthly Statistics Report ---");

        final int currentYear  = LocalDate.now().getYear();
        final int currentMonth = LocalDate.now().getMonthValue();

        int year = Console.readInteger(
                String.format("  Year  [default: %d]: ", currentYear));
        if (year <= 0) year = currentYear;

        int month = Console.readInteger(
                String.format("  Month [1-12, default: %d]: ", currentMonth));
        if (month < 1 || month > 12) month = currentMonth;

        try {
            System.out.println("\n  Generating report...");
            final String filename = ctrl.generateMonthlyReport(year, month);
            final String preview  = ctrl.previewMonthlyReport(year, month);

            System.out.println("\n" + preview);
            System.out.println("  >> Report saved to: " + filename);

        } catch (final IllegalArgumentException e) {
            System.out.println("\n  >> ERROR: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n  >> Unexpected error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "US112 - Monthly Statistics Report";
    }
}
