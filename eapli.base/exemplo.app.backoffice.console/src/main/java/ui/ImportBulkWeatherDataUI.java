package ui;

import controller.ImportBulkWeatherDataController;
import application.ImportWeatherResult;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

 // Console UI for US042

public class ImportBulkWeatherDataUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final ImportBulkWeatherDataController ctrl = new ImportBulkWeatherDataController();

        System.out.println("\n--- Import Bulk Weather Data ---\n");
        System.out.println("Supported formats: CSV (.csv)");

        final String filePath = Console.readLine("File path: ").trim();
        if (filePath.isEmpty()) {
            System.out.println(">> Cancelled — no file path entered.");
            return false;
        }

        try {
            final ImportWeatherResult result = ctrl.importWeatherData(filePath);
            displaySummary(result);
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Import Bulk Weather Data";
    }

    private void displaySummary(final ImportWeatherResult result) {
        System.out.println("\n--- Import Summary ---");
        System.out.printf("  Total read : %d%n", result.totalRead());
        System.out.printf("  Imported   : %d%n", result.importedCount());
        System.out.printf("  Rejected   : %d%n", result.rejectedCount());

        if (!result.errors().isEmpty()) {
            System.out.println("\n  Rejected records:");
            for (final String error : result.errors()) {
                System.out.println("    [REJECTED] " + error);
            }
        }

        if (result.rejectedCount() == 0) {
            System.out.println("\n>> All records imported successfully.");
        } else if (result.importedCount() == 0) {
            System.out.println("\n>> No records were imported. Please review the errors above.");
        } else {
            System.out.println("\n>> Import complete with some rejections. See errors above.");
        }
    }
}