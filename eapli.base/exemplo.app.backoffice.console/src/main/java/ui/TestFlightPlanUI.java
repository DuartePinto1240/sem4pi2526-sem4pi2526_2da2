package ui;

import controller.TestFlightPlanController;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import lprog.FlightPlan;
import lprog.FlightPlanTestResult;

import java.io.IOException;
import java.util.List;

/**
 * UI for US085 — Test/Validate Flight Plan.
 *
 * Lists the pilot's DRAFT flight plans and runs the two-phase validation
 * (DSL re-validation + C feasibility check).
 */
public class TestFlightPlanUI extends AbstractUI {

    private final TestFlightPlanController ctrl = new TestFlightPlanController();

    @Override
    protected boolean doShow() {
        System.out.println("\n--- Test / Validate Flight Plan ---");

        final String email = currentUserEmail();

        final List<FlightPlan> plans = ctrl.listDraftFlightPlans(email);
        if (plans.isEmpty()) {
            System.out.println(">> No DRAFT flight plans available for testing.");
            return false;
        }

        System.out.println("\nDRAFT flight plans:");
        for (int i = 0; i < plans.size(); i++) {
            final FlightPlan p = plans.get(i);
            System.out.printf("  %d. [%s] %s  (%d leg(s))%n",
                    i + 1,
                    p.status(),
                    p.flightId(),
                    p.legs().size());
        }

        final int idx = Console.readInteger("Select flight plan to test:") - 1;
        if (idx < 0 || idx >= plans.size()) {
            System.out.println(">> Invalid selection.");
            return false;
        }

        final FlightPlan selected = plans.get(idx);
        System.out.printf("%nTesting flight plan '%s'...%n", selected.flightId());

        try {
            final FlightPlanTestResult result = ctrl.testFlightPlan(selected.flightId());

            // Compute box width from the longest line
            final List<String> errors = result.errors();
            int contentWidth = 44; // minimum (covers stat lines like "  Remaining fuel   : 99999 kg")
            for (final String err : errors) {
                contentWidth = Math.max(contentWidth, err.length() + 6);
            }
            final String hr    = "═".repeat(contentWidth);
            final String top   = "╔" + hr + "╗";
            final String mid   = "╠" + hr + "╣";
            final String bot   = "╚" + hr + "╝";

            System.out.println("\n" + top);
            if (result.isPassed()) {
                System.out.println("║  Result : PASS ✓" + " ".repeat(contentWidth - 17) + "║");
                System.out.println(mid);
                final String distLine   = String.format("  Total distance   : %.0f km", result.totalDistanceKm());
                final String durLine    = String.format("  Estimated flight : %s", result.estimatedDuration());
                final String fuelLine   = String.format("  Declared fuel    : %.0f kg", result.totalDeclaredFuelKg());
                final String burnLine   = String.format("  Estimated burn   : %.0f kg", result.estimatedFuelBurnKg());
                final String remLine    = String.format("  Remaining fuel   : %.0f kg", result.remainingFuelKg());
                for (final String statLine : new String[]{distLine, durLine, fuelLine, burnLine, remLine}) {
                    System.out.println("║" + statLine + " ".repeat(contentWidth - statLine.length()) + "║");
                }
            } else {
                System.out.println("║  Result : FAIL ✗" + " ".repeat(contentWidth - 17) + "║");
                System.out.println(mid);
                for (final String err : errors) {
                    final String line = "  • " + err;
                    System.out.println("║" + line + " ".repeat(contentWidth - line.length()) + "║");
                }
            }
            System.out.println(bot);

            if (result.isPassed()) {
                System.out.printf("%nFlight plan '%s' marked as TESTED.%n",
                        selected.flightId());
            }

        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        } catch (final IOException e) {
            System.out.println("\n>> ERROR: Could not run feasibility checker: "
                    + e.getMessage());
            System.out.println("   Make sure the C checker is compiled: "
                    + "cd Scomp && make flight_plan_test");
        }

        return false;
    }

    private String currentUserEmail() {
        return AuthzRegistry.authorizationService()
                .session()
                .map(s -> s.authenticatedUser().username().toString())
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    @Override
    public String headline() {
        return "US085 - Test/Validate Flight Plan";
    }
}
