package bootstrapper;

import controller.CreateAircraftModelController;
import domain.aircraftmodel.AircraftType;
import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import eapli.framework.actions.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Bootstrap action that pre-loads a set of aircraft models.
 *
 * Must run after EngineModelBootstrapper (engines must exist first).
 * Models that already exist are silently skipped.
 */
public class AircraftModelBootstrapper implements Action {

    @Override
    public boolean execute() {
        final CreateAircraftModelController ctrl = new CreateAircraftModelController();

        final List<EngineModel> allEngines = new ArrayList<>();
        ctrl.availableEngineModels().forEach(allEngines::add);

        if (allEngines.isEmpty()) {
            System.out.println(
                    ">> AircraftModelBootstrapper: no engine models available, skipping.");
            return true;
        }

        // ── Boeing 737 MAX 8 ─────────────────────────────────────────────────
        // Engine: LEAP-1B (CFM International) — TURBOFAN
        findEngine(allEngines, "LEAP-1B", MotorizationType.TURBOFAN).ifPresent(leap1b ->
                register(ctrl,
                        "737 MAX 8", "Boeing", AircraftType.PASSENGER,
                        List.of(leap1b),
                        41413.0,   // emptyWeight  (kg)
                        65317.0,   // mzfw         (kg)
                        79016.0,   // mtow         (kg)
                        26020.0,   // maxFuel       (l)
                        12500.0,   // serviceCeiling (m)
                        230.0,     // cruiseSpeed  (m/s)
                        5550.0,    // maxRange     (km)
                        125.08,    // wingArea     (m²)
                        0.0265,    // Cd
                        1.45)      // Cl
        );

        // ── Airbus A320neo ────────────────────────────────────────────────────
        // Engines: CFM56-5B or PW1100G — both TURBOFAN, certify both
        final List<EngineModel> a320Engines = new ArrayList<>();
        findEngine(allEngines, "CFM56-5B", MotorizationType.TURBOFAN)
                .ifPresent(a320Engines::add);
        findEngine(allEngines, "PW1100G", MotorizationType.TURBOFAN)
                .ifPresent(a320Engines::add);

        if (!a320Engines.isEmpty()) {
            register(ctrl,
                    "A320neo", "Airbus", AircraftType.PASSENGER,
                    a320Engines,
                    42400.0,   // emptyWeight  (kg)
                    64300.0,   // mzfw         (kg)
                    79000.0,   // mtow         (kg)
                    26730.0,   // maxFuel       (l)
                    12131.0,   // serviceCeiling (m)
                    228.0,     // cruiseSpeed  (m/s)
                    6300.0,    // maxRange     (km)
                    122.6,     // wingArea     (m²)
                    0.0260,    // Cd
                    1.52);     // Cl
        }

        // ── Boeing 787-9 ─────────────────────────────────────────────────────
        // Engine: GEnx-1B (General Electric) — TURBOFAN
        findEngine(allEngines, "GEnx-1B", MotorizationType.TURBOFAN).ifPresent(genx ->
                register(ctrl,
                        "787-9 Dreamliner", "Boeing", AircraftType.PASSENGER,
                        List.of(genx),
                        128850.0,  // emptyWeight  (kg)
                        193000.0,  // mzfw         (kg)
                        254011.0,  // mtow         (kg)
                        126920.0,  // maxFuel       (l)
                        13100.0,   // serviceCeiling (m)
                        248.0,     // cruiseSpeed  (m/s)
                        14140.0,   // maxRange     (km)
                        325.0,     // wingArea     (m²)
                        0.0220,    // Cd
                        1.40)      // Cl
        );

        // ── ATR 72-600 ────────────────────────────────────────────────────────
        // Engine: PW127 (Pratt & Whitney Canada) — TURBOPROP
        findEngine(allEngines, "PW127", MotorizationType.TURBOPROP).ifPresent(pw127 ->
                register(ctrl,
                        "ATR 72-600", "ATR", AircraftType.PASSENGER,
                        List.of(pw127),
                        13311.0,   // emptyWeight  (kg)
                        21350.0,   // mzfw         (kg)
                        23000.0,   // mtow         (kg)
                        6370.0,    // maxFuel       (l)
                        7600.0,    // serviceCeiling (m)
                        139.0,     // cruiseSpeed  (m/s)
                        1528.0,    // maxRange     (km)
                        61.0,      // wingArea     (m²)
                        0.0340,    // Cd
                        1.60)      // Cl
        );

        return true;
    }

    private java.util.Optional<EngineModel> findEngine(final List<EngineModel> engines,
                                                       final String name,
                                                       final MotorizationType type) {
        return engines.stream()
                .filter(e -> e.name().equalsIgnoreCase(name)
                        && e.motorizationType() == type)
                .findFirst();
    }

    private void register(final CreateAircraftModelController ctrl,
                          final String modelName,
                          final String manufacturer,
                          final AircraftType type,
                          final List<EngineModel> engines,
                          final double emptyWeight,
                          final double mzfw,
                          final double mtow,
                          final double maxFuelCapacity,
                          final double serviceCeiling,
                          final double cruiseSpeed,
                          final double maximumRange,
                          final double wingArea,
                          final double dragCoefficient,
                          final double liftCoefficient) {
        try {
            ctrl.createAircraftModel(modelName, manufacturer, type,
                    emptyWeight, mzfw, mtow, maxFuelCapacity,
                    serviceCeiling, cruiseSpeed, maximumRange,
                    wingArea, dragCoefficient, liftCoefficient,
                    engines);
        } catch (final Exception e) {
            // Already exists or validation failed — skip silently
        }
    }
}