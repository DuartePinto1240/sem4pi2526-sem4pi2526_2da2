package ui;

import controller.DeleteFlightRouteController;
import dto.FlightRouteDTO;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DeleteFlightRouteUI extends AbstractUI {

    private final DeleteFlightRouteController ctrl;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DeleteFlightRouteUI() {
        this.ctrl = new DeleteFlightRouteController();
    }

    @Override
    protected boolean doShow() {

        System.out.println("\n--- Delete (Deactivate) Flight Route ---");

        final List<FlightRouteDTO> routes = getCompanyRoutes();
        if (routes.isEmpty()) {
            System.out.println(">> No active routes found for your company.");
            return false;
        }

        System.out.println("\nActive routes:");
        for (int i = 0; i < routes.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, routes.get(i));
        }

        final int idx = Console.readInteger("Select route:") - 1;
        if (idx < 0 || idx >= routes.size()) {
            System.out.println(">> Invalid selection.");
            return false;
        }
        final FlightRouteDTO selected = routes.get(idx);

        LocalDate deactivationDate = null;
        while (deactivationDate == null) {
            try {
                deactivationDate = LocalDate.parse(
                        Console.readLine("Deactivation date (yyyy-MM-dd):"), DATE_FMT);
            } catch (DateTimeParseException e) {
                System.out.println(">> Invalid date format. Please use yyyy-MM-dd.");
            }
        }

        deleteRoute(selected, deactivationDate);

        return false;
    }

    private List<FlightRouteDTO> getCompanyRoutes() {
        final String email = AuthzRegistry.authorizationService()
                .session()
                .map(s -> s.authenticatedUser().username().toString())
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return ctrl.getCompanyRoutes(email);
    }

    private void deleteRoute(final FlightRouteDTO flightRoute,
                             final LocalDate deactivationDate) {
        try {
            ctrl.deleteRoute(flightRoute.designator, deactivationDate);
            System.out.println("\n>> Route deactivated successfully.");
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }
    }

    @Override
    public String headline() {
        return "US074 - Delete (Deactivate) a Flight Route";
    }
}