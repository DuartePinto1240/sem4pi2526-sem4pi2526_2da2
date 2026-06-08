package controller;

import lprog.FlightPlan;
import lprog.FlightPlanBuilderListener.FlightData;
import lprog.FlightPlanBuilderListener.LegData;
import lprog.FlightPlanBuilderListener.SegmentData;
import lprog.FlightPlanImporter;
import lprog.FlightPlanStatus;
import lprog.FlightPlanTestResult;
import persistence.PersistenceContext;
import repositories.FlightPlanRepository;

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
 *   <li>Feasibility check via the C program {@code flight_plan_test} (fuel vs. segments).</li>
 * </ol>
 * <p>On success the flight plan status transitions from DRAFT → TESTED.</p>
 */
public class TestFlightPlanController {

    /** Path to the compiled C checker binary. Overridable for tests. */
    private final String checkerBinary;

    private final FlightPlanRepository repo;

    /** Production constructor — uses real persistence and default binary path. */
    public TestFlightPlanController() {
        this.repo          = PersistenceContext.getInstance().flightPlanRepository();
        this.checkerBinary = resolveCheckerBinary();
    }

    /** Test constructor — injects repo and binary path. */
    TestFlightPlanController(final FlightPlanRepository repo, final String checkerBinary) {
        this.repo          = repo;
        this.checkerBinary = checkerBinary;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns all DRAFT flight plans available for testing.
     * In production these belong to the authenticated pilot; in this sprint
     * the pilotEmail filter is a best-effort proxy (see FlightPlanRepository note).
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
     *   <li>Run C feasibility checker.</li>
     *   <li>If both pass → mark plan as TESTED and save.</li>
     * </ol>
     *
     * @param flightPlanId identity of the flight plan to test
     * @return test result (PASS or FAIL with details)
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

        // ── 3. Feasibility check (C program) ──────────────────────────────────
        final FlightPlanTestResult feasibility = runFeasibilityCheck(dslResult.getFlights());
        if (!feasibility.isPassed()) return feasibility;

        // ── 4. Mark TESTED ────────────────────────────────────────────────────
        plan.markAsTested("PASS — DSL valid, fuel feasibility confirmed.");
        repo.save(plan);

        return FlightPlanTestResult.pass();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Builds the argument list for the C checker and runs it.
     *
     * Format:
     *   flight_plan_test <num_legs>
     *     <leg_fuel> <leg_segs>
     *       <fromLat> <fromLon> <toLat> <toLon> <minAlt> <maxAlt> <windSpeed>
     *       ... (one set of 7 values per segment)
     *     ... (repeated per leg)
     */
    private FlightPlanTestResult runFeasibilityCheck(final List<FlightData> flights)
            throws IOException {

        // Flatten all legs across all flights
        final List<LegData> allLegs = new ArrayList<>();
        for (final FlightData f : flights) allLegs.addAll(f.legs);

        if (allLegs.isEmpty())
            return FlightPlanTestResult.fail(List.of("No legs found in flight plan."));

        final List<String> cmd = new ArrayList<>();
        cmd.add(checkerBinary);
        cmd.add(String.valueOf(allLegs.size()));

        for (final LegData leg : allLegs) {
            cmd.add(String.valueOf(leg.fuelQuantity));
            cmd.add(String.valueOf(leg.segments.size()));
            for (final SegmentData seg : leg.segments) {
                cmd.add(String.valueOf(seg.fromLat));
                cmd.add(String.valueOf(seg.fromLon));
                cmd.add(String.valueOf(seg.toLat));
                cmd.add(String.valueOf(seg.toLon));
                // min and max altitude from the altitude slots list
                final double minAlt = seg.altitudeSlots.isEmpty() ? 0.0
                        : seg.altitudeSlots.stream().mapToDouble(d -> d).min().getAsDouble();
                final double maxAlt = seg.altitudeSlots.isEmpty() ? 0.0
                        : seg.altitudeSlots.stream().mapToDouble(d -> d).max().getAsDouble();
                cmd.add(String.valueOf(minAlt));
                cmd.add(String.valueOf(maxAlt));
                cmd.add(String.valueOf(seg.windSpeed));
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

        if (exitCode == 0) return FlightPlanTestResult.pass();

        // FAIL — collect error lines (skip first "FAIL" line)
        final List<String> errors = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (!lines.get(i).isBlank()) errors.add(lines.get(i));
        }
        if (errors.isEmpty()) errors.add("Feasibility check failed (no details).");
        return FlightPlanTestResult.fail(errors);
    }

    /**
     * Resolves the path to the C checker binary.
     * Looks for a pre-built binary relative to the working directory.
     */
    private static String resolveCheckerBinary() {
        // Try common locations: project root Scomp/bin, or same dir
        final String[] candidates = {
                "../Scomp/bin/flight_plan_test",
                "Scomp/bin/flight_plan_test",
                "./flight_plan_test"
        };
        for (final String path : candidates) {
            if (new java.io.File(path).exists()) return path;
        }
        // Default — let ProcessBuilder throw if not found
        return "../Scomp/bin/flight_plan_test";
    }
}
