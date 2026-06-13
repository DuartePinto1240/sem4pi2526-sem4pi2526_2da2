package controller;

import domain.WeatherData;
import domain.aircraft.Aircraft;
import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.FlightCharacteristics;
import lprog.FlightPlan;
import lprog.FlightPlanBuilderListener.FlightData;
import lprog.FlightPlanBuilderListener.LegData;
import lprog.FlightPlanBuilderListener.SegmentData;
import lprog.FlightPlanImporter;
import lprog.FlightPlanStatus;
import lprog.FlightPlanTestResult;
import persistence.PersistenceContext;
import repositories.AircraftModelRepository;
import repositories.AircraftRepository;
import repositories.FlightPlanRepository;
import repositories.WeatherDataRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * US085 — Test/Validate Flight Plan.
 *
 * <p>Two-phase validation:</p>
 * <ol>
 *   <li>DSL re-validation using the existing ANTLR pipeline (lexical + syntactic + semantic).</li>
 *   <li>Feasibility check via the C program {@code flight_plan_test}, using the
 *       aircraft model's real characteristics (maxFuelCapacity, serviceCeiling).</li>
 * </ol>
 * <p>If the plan has weather data associated (via US082), the wind values of each
 * segment are overridden by the weather record whose sub-area contains the segment's
 * midpoint and altitude range before the C checker is invoked.</p>
 * <p>On success the flight plan status transitions from DRAFT → TESTED and
 * flight statistics (distance, duration, remaining fuel) are returned.</p>
 */
public class TestFlightPlanController {

    /** Feet → metres conversion factor. */
    private static final double FT_TO_M = 0.3048;

    /** Path to the compiled C checker binary. Overridable for tests. */
    private final String checkerBinary;

    private final FlightPlanRepository    repo;
    private final AircraftRepository      aircraftRepo;
    private final AircraftModelRepository modelRepo;
    private final WeatherDataRepository   weatherRepo;

    /** Production constructor — uses real persistence and default binary path. */
    public TestFlightPlanController() {
        this.repo          = PersistenceContext.getInstance().flightPlanRepository();
        this.aircraftRepo  = PersistenceContext.getInstance().aircraftRepository();
        this.modelRepo     = PersistenceContext.getInstance().aircraftModelRepository();
        this.weatherRepo   = PersistenceContext.getInstance().weatherDataRepository();
        this.checkerBinary = resolveCheckerBinary();
    }

    /** Test constructor — injects repo and binary path (no aircraft/weather lookups). */
    TestFlightPlanController(final FlightPlanRepository repo, final String checkerBinary) {
        this.repo          = repo;
        this.aircraftRepo  = null;
        this.modelRepo     = null;
        this.weatherRepo   = null;
        this.checkerBinary = checkerBinary;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns all DRAFT flight plans available for testing.
     *
     * @param pilotEmail authenticated pilot's email
     */
    public List<FlightPlan> listDraftFlightPlans(final String pilotEmail) {
        final List<FlightPlan> result = new ArrayList<>();
        repo.findByPilotAndStatus(pilotEmail, FlightPlanStatus.DRAFT)
                .forEach(result::add);
        return result;
    }

    /**
     * Tests the flight plan identified by {@code flightPlanId}.
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Find plan — must exist and be in DRAFT status.</li>
     *   <li>Re-validate DSL with ANTLR pipeline.</li>
     *   <li>Look up assigned aircraft and its model characteristics.</li>
     *   <li>Apply associated weather data (if any) to segment wind values.</li>
     *   <li>Run C feasibility checker with aircraft parameters.</li>
     *   <li>If both pass → mark plan as TESTED, return stats.</li>
     * </ol>
     *
     * @param flightPlanId identity of the flight plan to test
     * @return test result (PASS with stats, or FAIL with details)
     * @throws IllegalArgumentException if the plan does not exist or is not DRAFT
     * @throws IOException              if the C checker cannot be executed
     */
    public FlightPlanTestResult testFlightPlan(final String flightPlanId) throws IOException {
        // ── 1. Find plan ──────────────────────────────────────────────────────
        final Optional<FlightPlan> opt = repo.findByFlightId(flightPlanId);
        if (opt.isEmpty())
            throw new IllegalArgumentException("Flight plan not found: " + flightPlanId);

        final FlightPlan plan = opt.get();
        if (plan.status() != FlightPlanStatus.DRAFT)
            throw new IllegalArgumentException(
                    "Flight plan '" + flightPlanId + "' is not in DRAFT status (current: "
                            + plan.status() + ")");

        if (plan.dslContent() == null || plan.dslContent().isBlank())
            throw new IllegalArgumentException(
                    "Flight plan '" + flightPlanId + "' has no DSL content to validate.");

        // ── 2. DSL validation (ANTLR) ─────────────────────────────────────────
        final FlightPlanImporter importer = new FlightPlanImporter();
        final FlightPlanImporter.FlightPlanImportResult dslResult =
                importer.importFromString(plan.dslContent());

        if (!dslResult.isSuccess()) {
            final List<String> errors = new ArrayList<>();
            dslResult.getErrors().forEach(e -> errors.add(e.toString()));
            return FlightPlanTestResult.fail(errors);
        }

        // ── 3. Resolve aircraft characteristics ───────────────────────────────
        final FlightCharacteristics chars = resolveCharacteristics(plan);

        // ── 4. Resolve weather data (if associated via US082) ─────────────────
        final WeatherData weather = resolveWeatherData(plan);

        // ── 5. Feasibility check (C program) ──────────────────────────────────
        final FlightPlanTestResult feasibility =
                runFeasibilityCheck(dslResult.getFlights(), chars, weather);
        if (!feasibility.isPassed()) return feasibility;

        // ── 6. Mark TESTED and persist ────────────────────────────────────────
        final String summary = String.format(
                "PASS — DSL valid. Distance: %.0f km | Duration: %s | "
                        + "Fuel burn: %.0f kg | Remaining: %.0f kg",
                feasibility.totalDistanceKm(),
                feasibility.estimatedDuration(),
                feasibility.estimatedFuelBurnKg(),
                feasibility.remainingFuelKg());

        plan.markAsTested(summary);
        repo.save(plan);

        return feasibility;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Looks up the aircraft model characteristics for the plan's assigned aircraft.
     * Returns null if no aircraft is assigned or repositories are unavailable (test mode).
     */
    private FlightCharacteristics resolveCharacteristics(final FlightPlan plan) {
        if (plan.aircraftRegistration() == null || aircraftRepo == null || modelRepo == null)
            return null;

        final Optional<Aircraft> aircraft = aircraftRepo.ofIdentity(plan.aircraftRegistration());
        if (aircraft.isEmpty()) return null;

        final Optional<AircraftModel> model = modelRepo.ofIdentity(aircraft.get().aircraftModelId());
        return model.map(AircraftModel::characteristics).orElse(null);
    }

    /**
     * Fetches the WeatherData record associated with this plan (via US082).
     * Returns null if no weather data is linked or the repository is unavailable.
     */
    private WeatherData resolveWeatherData(final FlightPlan plan) {
        if (plan.weatherDataId() == null || weatherRepo == null) return null;
        return weatherRepo.ofIdentity(plan.weatherDataId()).orElse(null);
    }

    /**
     * Applies weather data wind conditions to any segment whose midpoint (lat/lon)
     * falls inside the weather sub-area and whose altitude range (ft → m) overlaps
     * the weather altitude range.
     *
     * <p>This mutates {@code seg.windAngle} and {@code seg.windSpeed} in place,
     * which is safe because {@code SegmentData} is a transient parse DTO.</p>
     */
    private void applyWeatherToSegments(final List<LegData> legs, final WeatherData wd) {
        if (wd == null) return;

        final double minLat    = wd.subArea().subBoundary().minLat();
        final double maxLat    = wd.subArea().subBoundary().maxLat();
        final double minLon    = wd.subArea().subBoundary().minLon();
        final double maxLon    = wd.subArea().subBoundary().maxLon();
        final double minAltM   = wd.subArea().altitudeRange().minAltitude();
        final double maxAltM   = wd.subArea().altitudeRange().maxAltitude();
        final double wAngle    = wd.wind().windDirection();
        final double wSpeed    = wd.wind().windSpeed();

        for (final LegData leg : legs) {
            for (final SegmentData seg : leg.segments) {
                // Midpoint of the segment
                final double midLat = (seg.fromLat + seg.toLat) / 2.0;
                final double midLon = (seg.fromLon + seg.toLon) / 2.0;

                // Segment altitude range converted to metres
                final double segMinM = seg.altitudeMin * FT_TO_M;
                final double segMaxM = seg.altitudeMax * FT_TO_M;

                // Geographic match: midpoint inside the weather sub-area bounding box
                final boolean geoMatch = midLat >= minLat && midLat <= maxLat
                                      && midLon >= minLon && midLon <= maxLon;

                // Altitude overlap: [segMinM, segMaxM) ∩ [minAltM, maxAltM) ≠ ∅
                final boolean altMatch = segMinM < maxAltM && minAltM < segMaxM;

                if (geoMatch && altMatch) {
                    seg.windAngle = wAngle;
                    seg.windSpeed = wSpeed;
                }
            }
        }
    }

    /**
     * Builds the argument list for the C checker and runs it.
     *
     * Format:
     * <pre>
     *   flight_plan_test &lt;num_legs&gt; &lt;maxFuelCapacity&gt; &lt;serviceCeiling&gt; &lt;cruiseSpeedKmh&gt;
     *     &lt;leg_fuel&gt; &lt;leg_segs&gt;
     *       &lt;fromLat&gt; &lt;fromLon&gt; &lt;toLat&gt; &lt;toLon&gt; &lt;minAlt&gt; &lt;maxAlt&gt; &lt;windAngle&gt; &lt;windSpeed&gt; &lt;phase&gt;
     *       ...
     *     ...
     * </pre>
     * PASS output: {@code PASS <totalDistKm> <totalFuelKg> <burnKg>}
     */
    private FlightPlanTestResult runFeasibilityCheck(final List<FlightData> flights,
                                                      final FlightCharacteristics chars,
                                                      final WeatherData weather)
            throws IOException {

        // Flatten all legs across all flights
        final List<LegData> allLegs = new ArrayList<>();
        for (final FlightData f : flights) allLegs.addAll(f.legs);

        if (allLegs.isEmpty())
            return FlightPlanTestResult.fail(List.of("No legs found in flight plan."));

        // Override segment wind values with associated weather data (US082 → US085 link)
        applyWeatherToSegments(allLegs, weather);

        // Use model values if available; fall back to generous defaults.
        // serviceCeiling is stored in metres in AircraftModel; convert to feet
        // because altitude_range in the DSL (and in FlightSegment) is in feet.
        final double maxFuelCapacity      = chars != null ? chars.maxFuelCapacity() : 999_999.0;
        final double serviceCeilingMetres = chars != null ? chars.serviceCeiling()  : 13_716.0; // 45 000 ft
        final double serviceCeiling       = serviceCeilingMetres * 3.28084;                     // → feet
        final double cruiseSpeedKmh       = chars != null ? chars.cruiseSpeed()     : 800.0;

        final List<String> cmd = new ArrayList<>();
        cmd.add(checkerBinary);
        cmd.add(String.valueOf(allLegs.size()));
        cmd.add(String.valueOf(maxFuelCapacity));
        cmd.add(String.valueOf(serviceCeiling));
        cmd.add(String.valueOf(cruiseSpeedKmh));       // 4th global arg: cruise speed

        for (final LegData leg : allLegs) {
            cmd.add(String.valueOf(leg.fuelQuantity));
            cmd.add(String.valueOf(leg.segments.size()));
            for (final SegmentData seg : leg.segments) {
                cmd.add(String.valueOf(seg.fromLat));
                cmd.add(String.valueOf(seg.fromLon));
                cmd.add(String.valueOf(seg.toLat));
                cmd.add(String.valueOf(seg.toLon));
                cmd.add(String.valueOf(seg.altitudeMin));
                cmd.add(String.valueOf(seg.altitudeMax));
                cmd.add(String.valueOf(seg.windAngle));  // wind direction (degrees FROM)
                cmd.add(String.valueOf(seg.windSpeed));  // wind speed (m/s)
                cmd.add(seg.phase != null ? seg.phase : "cruise"); // flight phase
            }
        }

        final Process proc = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();

        final List<String> lines = new ArrayList<>();
        try (final BufferedReader br =
                     new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) lines.add(line);
        }

        final int exitCode;
        try {
            exitCode = proc.waitFor();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return FlightPlanTestResult.fail(List.of("Feasibility checker interrupted."));
        }

        if (exitCode == 0 && !lines.isEmpty()) {
            // PASS line format: "PASS <totalDistKm> <totalFuelKg> <burnKg>"
            final String[] parts = lines.get(0).split("\\s+");
            double dist = 0, fuel = 0, burn = 0;
            if (parts.length >= 4) {
                try {
                    dist = Double.parseDouble(parts[1]);
                    fuel = Double.parseDouble(parts[2]);
                    burn = Double.parseDouble(parts[3]);
                } catch (final NumberFormatException ignored) { }
            }
            return FlightPlanTestResult.pass(dist, fuel, burn, cruiseSpeedKmh);
        }

        // FAIL — collect error lines (skip first "FAIL" line)
        final List<String> errors = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (!lines.get(i).isBlank()) errors.add(lines.get(i));
        }
        if (errors.isEmpty()) errors.add("Feasibility check failed (no details).");
        return FlightPlanTestResult.fail(errors);
    }

    /** Resolves the path to the C checker binary. */
    private static String resolveCheckerBinary() {
        final String[] candidates = {
                "../Scomp/bin/flight_plan_test",
                "Scomp/bin/flight_plan_test",
                "./flight_plan_test"
        };
        for (final String path : candidates) {
            if (new java.io.File(path).exists()) return path;
        }
        return "../Scomp/bin/flight_plan_test";
    }
}
