package ui;

import controller.RemovePilotController;
import dto.PilotDTO;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * UI for US077 – Remove (Deactivate) a Pilot.
 */
public class RemovePilotUI extends AbstractUI {

    private final RemovePilotController ctrl;

    public RemovePilotUI() {
        this.ctrl = new RemovePilotController();
    }

    @Override
    protected boolean doShow() {
        final RemovePilotController ctrl = new RemovePilotController();

        System.out.println("\n--- Remove (Deactivate) Pilot ---");

        final List<PilotDTO> pilots = getActivePilots();
        if (pilots.isEmpty()) {
            System.out.println(">> No active pilots found for your company.");
            return false;
        }

        System.out.println("\nActive pilots:");
        for (int i = 0; i < pilots.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, pilots.get(i));
        }

        final int idx = Console.readInteger("Select pilot:") - 1;
        if (idx < 0 || idx >= pilots.size()) {
            System.out.println(">> Invalid selection.");
            return false;
        }
        final PilotDTO selected = pilots.get(idx);

        removePilot(selected);

        return false;
    }

    private List<PilotDTO> getActivePilots () {
        final String email = AuthzRegistry.authorizationService()
                .session()
                .map(s -> s.authenticatedUser().username().toString())
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return ctrl.getActivePilots(email);
    }

    private void removePilot (PilotDTO pilot) {
        try {
            ctrl.removePilot(pilot.email);
            System.out.println("\n>> Pilot deactivated successfully.");
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }
    }

    @Override
    public String headline() {
        return "US077 - Remove a Pilot";
    }
}