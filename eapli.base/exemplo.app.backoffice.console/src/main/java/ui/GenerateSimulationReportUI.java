// ─── ui/GenerateSimulationReportUI.java ───────────────────────────────────

package ui;

import controller.GenerateSimulationReportController;
import domain.simulationreport.FlightExecutionStatus;
import domain.simulationreport.SafetyViolationEvent;
import domain.simulationreport.SimulationReport;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.List;

public class GenerateSimulationReportUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final GenerateSimulationReportController ctrl =
                new GenerateSimulationReportController();

        System.out.println("\n--- Generate Simulation Report ---");

        final List<String> files = ctrl.availableReportFiles();

        if (files.isEmpty()) {
            System.out.println("\n>> No simulation report files found.");
            System.out.println("   Run a simulation first (SCOMP module).");
            return false;
        }

        System.out.println("\nAvailable report files:");
        for (int i = 0; i < files.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, files.get(i));
        }

        final int choice = Console.readInteger("Select file:");
        if (choice < 1 || choice > files.size()) {
            System.out.println(">> Invalid option.");
            return false;
        }

        final String filename = files.get(choice - 1);
        System.out.println("\nImporting '" + filename + "'...");

        try {
            final SimulationReport report = ctrl.generateReport(filename);

            System.out.println("\n>> Report generated and stored successfully.");
            System.out.println();
            System.out.println("=== SIMULATION REPORT ===");
            System.out.printf("Generated  : %s%n", report.generatedAt());
            System.out.printf("Flights    : %d%n", report.totalFlights());
            System.out.printf("Violations : %d%n", report.violationEvents().size());
            System.out.printf("Result     : %s%n", report.result());

            System.out.println("\nFlight Statuses:");
            for (final FlightExecutionStatus s : report.flightStatuses()) {
                System.out.printf("  %s%n", s);
            }

            if (!report.violationEvents().isEmpty()) {
                System.out.println("\nSafety Violations:");
                int i = 1;
                for (final SafetyViolationEvent v : report.violationEvents()) {
                    System.out.printf("  #%d — %s%n", i++, v);
                }
            }

        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "US111 - Generate Simulation Report";
    }
}