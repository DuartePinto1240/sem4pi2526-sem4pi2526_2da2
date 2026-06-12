package ui;

import controller.ListPilotRosterController;
import domain.AirTransportCompany;
import dto.PilotDTO;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ListPilotRosterUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final ListPilotRosterController ctrl = new ListPilotRosterController();

        System.out.println("\n--- List Pilot Roster ---");

        final String email = currentUserEmail();
        if (email == null) {
            System.out.println(">> No authenticated user.");
            return false;
        }

        final List<PilotDTO> pilots = ctrl.activePilotsOfMyCompany(email);
        printRoster(pilots);
        return false;
    }

    private String currentUserEmail() {
        final Optional<UserSession> session = AuthzRegistry.authorizationService().session();
        return session.map(UserSession::authenticatedUser)
                .map(u -> u.username().toString())
                .orElse(null);
    }

    private void printRoster(final List<PilotDTO> pilots) {
        System.out.println();
        System.out.println("┌──────────────────────────────┬───────────────────────────────┬─────────────────┐");
        System.out.println("│ Name                         │ Email                         │ Phone           │");
        System.out.println("├──────────────────────────────┼───────────────────────────────┼─────────────────┤");

        if (pilots.isEmpty()) {
            System.out.println("│              No active pilots found for this company.                        │");
            System.out.println("└──────────────────────────────┴───────────────────────────────┴─────────────────┘");
            return;
        }

        for (final PilotDTO p : pilots) {
            System.out.printf("│ %-28s │ %-29s │ %-15s │%n",
                    p.name, p.email, p.phoneNumber);

            final String models = String.join(", ", p.certifiedModels);
            System.out.printf("│   Certified models: %-59s│%n", models);
            System.out.println("├──────────────────────────────┼───────────────────────────────┼─────────────────┤");
        }

        System.out.println("└──────────────────────────────┴───────────────────────────────┴─────────────────┘");
    }

    @Override
    public String headline() {
        return "US076 - List Pilot Roster";
    }
}