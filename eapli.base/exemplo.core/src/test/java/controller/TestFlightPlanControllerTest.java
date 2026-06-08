package controller;

import lprog.FlightLeg;
import lprog.FlightPlan;
import lprog.FlightPlanStatus;
import lprog.FlightPlanTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.FlightPlanRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for US085 — TestFlightPlanController.
 *
 * <p>Uses a mocked {@link FlightPlanRepository} and a stub C checker script
 * written to {@code @TempDir} so no real binary or database is needed.</p>
 */
class TestFlightPlanControllerTest {

    @TempDir
    Path tempDir;

    private FlightPlanRepository repo;

    // ── DSL helpers ───────────────────────────────────────────────────────────

    /** Minimal valid single-leg DSL matching the actual ANTLR grammar. */
    private static final String VALID_DSL =
        "flight F001 {\n"
        + "    type: regular\n"
        + "    leg {\n"
        + "        departure: LIS 2026-06-01 08:00\n"
        + "        arrival:   MAD 10:30\n"
        + "        fuel:      1600 kg\n"
        + "        segment {\n"
        + "            from: (38.7813, -9.1359)\n"
        + "            to:   (40.4937, -3.5670)\n"
        + "            altitude_slots: [28000, 30000]\n"
        + "            width: 50 m\n"
        + "            wind:  090 deg 15 m/s\n"
        + "        }\n"
        + "    }\n"
        + "}\n";

    /** DSL with insufficient fuel — 100 kg declared, far below what Haversine requires. */
    private static final String LOW_FUEL_DSL =
        "flight F002 {\n"
        + "    type: regular\n"
        + "    leg {\n"
        + "        departure: LIS 2026-06-01 08:00\n"
        + "        arrival:   MAD 10:30\n"
        + "        fuel:      100 kg\n"
        + "        segment {\n"
        + "            from: (38.7813, -9.1359)\n"
        + "            to:   (40.4937, -3.5670)\n"
        + "            altitude_slots: [28000, 30000]\n"
        + "            width: 50 m\n"
        + "            wind:  090 deg 15 m/s\n"
        + "        }\n"
        + "    }\n"
        + "}\n";

    /** DSL with a syntactic error. */
    private static final String INVALID_DSL =
        "flight { this is not valid DSL content }\n";

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        repo = mock(FlightPlanRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Creates a FlightPlan with the given DSL and registers it in the mock repo. */
    private FlightPlan planWithDsl(final String flightId, final String dsl) {
        final FlightPlan plan = new FlightPlan(
                flightId, "ifr",
                List.of(new FlightLeg("LPPT", "2026-06-01", "08:00", "LPFR", "10:30", 1000.0, "litros")));
        plan.setDslContent(dsl);
        when(repo.findByFlightId(flightId)).thenReturn(java.util.Optional.of(plan));
        return plan;
    }

    /** Writes a checker stub to tempDir that works on both Windows and Unix. */
    private String makeChecker(final int exitCode, final String output) throws IOException {
        final boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        final Path script;
        if (isWindows) {
            script = tempDir.resolve("flight_plan_test.bat");
            final StringBuilder sb = new StringBuilder("@echo off\r\n");
            for (final String line : output.split("\n", -1)) {
                sb.append("echo ").append(line.trim()).append("\r\n");
            }
            sb.append("exit /b ").append(exitCode).append("\r\n");
            java.nio.file.Files.writeString(script, sb.toString());
        } else {
            script = tempDir.resolve("flight_plan_test");
            java.nio.file.Files.writeString(script,
                    "#!/bin/sh\necho '" + output.replace("'", "'\\''") + "'\nexit " + exitCode + "\n");
            script.toFile().setExecutable(true);
        }
        return script.toString();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Test 1 — Non-existent flight plan throws IllegalArgumentException.
     * Refers to: US085.1
     */
    @Test
    void ensureNonExistentPlanThrows() {
        when(repo.findByFlightId("MISSING")).thenReturn(java.util.Optional.empty());
        final TestFlightPlanController ctrl =
                new TestFlightPlanController(repo, "unused");
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.testFlightPlan("MISSING"));
    }

    /**
     * Test 2 — Non-DRAFT plan throws IllegalArgumentException.
     * Refers to: US085.1
     */
    @Test
    void ensureNonDraftPlanThrows() throws Exception {
        final FlightPlan plan = planWithDsl("F001", VALID_DSL);
        // Manually set status to TESTED via markAsTested
        plan.markAsTested("previous result");

        final TestFlightPlanController ctrl =
                new TestFlightPlanController(repo, "unused");
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.testFlightPlan("F001"));
    }

    /**
     * Test 3 — Invalid DSL returns FAIL without running the C checker.
     * Refers to: US085.2
     */
    @Test
    void ensureInvalidDslReturnsFail() throws Exception {
        planWithDsl("F003", INVALID_DSL);
        final TestFlightPlanController ctrl =
                new TestFlightPlanController(repo, makeChecker(0, "PASS"));

        final FlightPlanTestResult result = ctrl.testFlightPlan("F003");

        assertFalse(result.isPassed(), "Invalid DSL must produce FAIL");
        assertFalse(result.errors().isEmpty(), "Errors list must not be empty");
        verify(repo, never()).save(any());
    }

    /**
     * Test 4 — Valid DSL + C checker PASS → plan marked TESTED.
     * Refers to: US085.2, US085.3
     */
    @Test
    void ensureValidDslAndPassCheckerMarksTested() throws Exception {
        final FlightPlan plan = planWithDsl("F001", VALID_DSL);
        final TestFlightPlanController ctrl =
                new TestFlightPlanController(repo, makeChecker(0, "PASS"));

        final FlightPlanTestResult result = ctrl.testFlightPlan("F001");

        assertTrue(result.isPassed(), "Valid DSL + PASS checker must produce PASS");
        assertEquals(FlightPlanStatus.TESTED, plan.status(), "Plan must be TESTED");
        verify(repo, times(1)).save(plan);
    }

    /**
     * Test 5 — Valid DSL + C checker FAIL → result is FAIL, plan stays DRAFT.
     * Refers to: US085.2
     */
    @Test
    void ensureValidDslButFailCheckerReturnsFail() throws Exception {
        final FlightPlan plan = planWithDsl("F002", LOW_FUEL_DSL);
        final String failOutput = "FAIL\nLeg 1: declared fuel 100 is below minimum 300";
        final TestFlightPlanController ctrl =
                new TestFlightPlanController(repo, makeChecker(1, failOutput));

        final FlightPlanTestResult result = ctrl.testFlightPlan("F002");

        assertFalse(result.isPassed(), "C checker FAIL must produce FAIL");
        assertEquals(FlightPlanStatus.DRAFT, plan.status(), "Plan must remain DRAFT");
        verify(repo, never()).save(any());
    }

    /**
     * Test 6 — listDraftFlightPlans returns only DRAFT plans.
     * Refers to: US085.1
     */
    @Test
    void ensureListReturnsOnlyDraftPlans() throws Exception {
        final FlightPlan draft  = planWithDsl("F_DRAFT",  VALID_DSL);
        final FlightPlan tested = planWithDsl("F_TESTED", VALID_DSL);
        tested.markAsTested("previous");

        when(repo.findByPilotAndStatus("pilot@tap.pt", FlightPlanStatus.DRAFT))
                .thenReturn(List.of(draft));

        final TestFlightPlanController ctrl =
                new TestFlightPlanController(repo, "unused");
        final List<FlightPlan> plans = ctrl.listDraftFlightPlans("pilot@tap.pt");

        assertEquals(1, plans.size());
        assertEquals("F_DRAFT", plans.get(0).flightId());
    }

    /**
     * Test 7 — Plan with no DSL content throws IllegalArgumentException.
     * Refers to: US085.2
     */
    @Test
    void ensurePlanWithNoDslThrows() throws Exception {
        final FlightPlan plan = new FlightPlan(
                "F_NODSL", "ifr",
                List.of(new FlightLeg("LPPT", "2026-06-01", "08:00", "LPFR", "10:30", 1000.0, "litros")));
        // no dslContent set
        when(repo.findByFlightId("F_NODSL")).thenReturn(java.util.Optional.of(plan));

        final TestFlightPlanController ctrl =
                new TestFlightPlanController(repo, "unused");
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.testFlightPlan("F_NODSL"));
    }
}
