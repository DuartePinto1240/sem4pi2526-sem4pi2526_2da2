package bootstrapper;

import controller.CreateEngineModelController;
import domain.enginemodel.MotorizationType;
import eapli.framework.actions.Action;

/**
 * Bootstrap action that pre-loads a set of engine models.
 *
 * Must run before AircraftModelBootstrapper.
 * Engines that already exist are silently skipped.
 */
public class EngineModelBootstrapper implements Action {

    @Override
    public boolean execute() {
        final CreateEngineModelController ctrl = new CreateEngineModelController();

        // ── Turbofan ──────────────────────────────────────────────────────────
        // CFM LEAP-1B — Boeing 737 MAX
        register(ctrl, "LEAP-1B", "CFM International",
                MotorizationType.TURBOFAN, 101.8, 121.4, 0.0157);

        // CFM56-5B — Airbus A320 family
        register(ctrl, "CFM56-5B", "CFM International",
                MotorizationType.TURBOFAN, 120.0, 138.0, 0.0160);

        // Rolls-Royce Trent 900 — Airbus A380
        register(ctrl, "Trent 900", "Rolls-Royce",
                MotorizationType.TURBOFAN, 311.0, 340.0, 0.0152);

        // GE GEnx-1B — Boeing 787
        register(ctrl, "GEnx-1B", "General Electric",
                MotorizationType.TURBOFAN, 296.0, 339.0, 0.0148);

        // Pratt & Whitney PW1100G — Airbus A320neo
        register(ctrl, "PW1100G", "Pratt & Whitney",
                MotorizationType.TURBOFAN, 120.0, 147.0, 0.0143);

        // ── Turboprop ─────────────────────────────────────────────────────────
        // Pratt & Whitney Canada PW127 — ATR 72
        register(ctrl, "PW127", "Pratt & Whitney Canada",
                MotorizationType.TURBOPROP, 15.0, 20.5, 0.0300);

        // General Electric CT7 — ATR 42
        register(ctrl, "CT7-9C", "General Electric",
                MotorizationType.TURBOPROP, 12.5, 16.0, 0.0320);

        return true;
    }

    private void register(final CreateEngineModelController ctrl,
                          final String name,
                          final String manufacturer,
                          final MotorizationType type,
                          final double thrustStopped,
                          final double thrustCruise,
                          final double tsfc) {
        try {
            ctrl.createEngineModel(name, manufacturer, type,
                    thrustStopped, thrustCruise, tsfc);
        } catch (final Exception e) {
            // Already exists or validation failed — skip silently
        }
    }
}