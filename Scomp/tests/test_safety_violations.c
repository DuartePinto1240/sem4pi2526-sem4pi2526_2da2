#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include <signal.h>
#include <math.h>

#include "../src/simulation.h"
#include "../src/flight.h"

/* ─────────────────────────────────────────
 * Test helpers
 * ───────────────────────────────────────── */

static int tests_run    = 0;
static int tests_passed = 0;
static int tests_failed = 0;

#define TEST(name) \
    do { \
        printf("  %-55s", name); \
        tests_run++; \
    } while(0)

#define PASS() \
    do { \
        printf("PASS\n"); \
        tests_passed++; \
    } while(0)

#define FAIL(msg) \
    do { \
        printf("FAIL — %s\n", msg); \
        tests_failed++; \
    } while(0)

#define ASSERT(cond, msg) \
    do { \
        if (cond) { PASS(); } \
        else       { FAIL(msg); } \
    } while(0)

/* ─────────────────────────────────────────
 * Global signal counters
 * Registered once in main() so that calls to
 * check_safety_violations() — which send SIGUSR1
 * to getpid() — do not terminate the test process.
 * ───────────────────────────────────────── */

static volatile sig_atomic_t global_sigusr1_count = 0;
static volatile sig_atomic_t global_sigterm_count = 0;

static void global_sigusr1_handler(int sig) {
    (void) sig;
    sigset_t block_all;
    sigfillset(&block_all);
    sigprocmask(SIG_BLOCK, &block_all, NULL);
    global_sigusr1_count++;
    sigprocmask(SIG_UNBLOCK, &block_all, NULL);
}

static void global_sigterm_handler(int sig) {
    (void) sig;
    global_sigterm_count++;
}

/* ─────────────────────────────────────────
 * Helpers — build test states
 * ───────────────────────────────────────── */

static FlightState make_state(int flight_id, pid_t pid,
                               double lat, double lon, double alt) {
    FlightState s;
    memset(&s, 0, sizeof(s));
    s.flight_id = flight_id;
    s.pid       = pid;
    s.active    = 1;
    s.current.latitude  = lat;
    s.current.longitude = lon;
    s.current.altitude  = alt;
    s.current.time_step = 0;
    return s;
}

/* ─────────────────────────────────────────
 * US102.1 — Violation detected below threshold
 * ───────────────────────────────────────── */

void test_violation_detected_when_below_threshold() {
    TEST("violation detected when distance < threshold");

    FlightState states[2];
    states[0] = make_state(0, getpid(), 41.0, -9.0, 10000.0);
    states[1] = make_state(1, getpid(), 41.0, -9.0, 10000.0);

    SimulationReport report;
    memset(&report, 0, sizeof(report));

    int violations = check_safety_violations(
            states, 2, 500.0, &report, 0.0);
    usleep(5000); /* allow signal delivery */

    ASSERT(violations > 0, "expected at least one violation");
}

/* ─────────────────────────────────────────
 * US102.1 — No violation above threshold
 * ───────────────────────────────────────── */

void test_no_violation_when_above_threshold() {
    TEST("no violation when distance > threshold");

    FlightState states[2];
    states[0] = make_state(0, getpid(), 41.0, -9.0, 10000.0);
    states[1] = make_state(1, getpid(), 38.0, -8.0,  5000.0);

    SimulationReport report;
    memset(&report, 0, sizeof(report));

    int violations = check_safety_violations(
            states, 2, 500.0, &report, 0.0);

    ASSERT(violations == 0, "expected zero violations");
}

/* ─────────────────────────────────────────
 * US102.1 — Exactly at threshold — no violation
 * ───────────────────────────────────────── */

void test_no_violation_at_exact_threshold() {
    TEST("no violation when distance equals threshold");

    FlightState states[2];
    states[0] = make_state(0, getpid(), 41.0, -9.0, 10000.0);
    /* 500 m altitude difference — distance == threshold, not < */
    states[1] = make_state(1, getpid(), 41.0, -9.0, 10500.0);

    SimulationReport report;
    memset(&report, 0, sizeof(report));
    

    int violations = check_safety_violations(
            states, 2, 500.0, &report, 0.0);

    ASSERT(violations == 0,
           "expected zero violations when distance == threshold");
}

/* ─────────────────────────────────────────
 * US102.2 — Violation event is logged
 * ───────────────────────────────────────── */

void test_violation_event_is_logged() {
    TEST("violation event is logged in report");

    FlightState states[2];
    states[0] = make_state(0, getpid(), 41.0, -9.0, 10000.0);
    states[1] = make_state(1, getpid(), 41.0, -9.0, 10000.0);

    SimulationReport report;
    memset(&report, 0, sizeof(report));

    check_safety_violations(states, 2, 500.0, &report, 42.0);
    usleep(5000);

    ASSERT(report.total_violations > 0,
           "expected total_violations > 0 after violation");
}

/* ─────────────────────────────────────────
 * US102.2 — Violation timestamp is recorded
 * ───────────────────────────────────────── */

void test_violation_timestamp_is_correct() {
    TEST("violation event has correct timestamp");

    FlightState states[2];
    states[0] = make_state(0, getpid(), 41.0, -9.0, 10000.0);
    states[1] = make_state(1, getpid(), 41.0, -9.0, 10000.0);

    SimulationReport report;
    memset(&report, 0, sizeof(report));

    check_safety_violations(states, 2, 500.0, &report, 15.0);
    usleep(5000);

    ASSERT(fabs(report.violations[0].timestamp - 15.0) < 1e-6,
           "violation timestamp should be 15.0");
}

/* ─────────────────────────────────────────
 * US102.2 — Violation records correct flight IDs
 * ───────────────────────────────────────── */

void test_violation_records_correct_flight_ids() {
    TEST("violation event records correct flight IDs");

    FlightState states[2];
    states[0] = make_state(3, getpid(), 41.0, -9.0, 10000.0);
    states[1] = make_state(7, getpid(), 41.0, -9.0, 10000.0);

    SimulationReport report;
    memset(&report, 0, sizeof(report));

    check_safety_violations(states, 2, 500.0, &report, 0.0);
    usleep(5000);

    ASSERT(report.violations[0].flight_id_1 == 3 &&
           report.violations[0].flight_id_2 == 7,
           "expected flight IDs 3 and 7 in violation event");
}

/* ─────────────────────────────────────────
 * US102.2 — Violation records positions
 * ───────────────────────────────────────── */

void test_violation_records_positions() {
    TEST("violation event records positions of both aircraft");

    FlightState states[2];
    states[0] = make_state(0, getpid(), 41.2, -9.1, 10000.0);
    states[1] = make_state(1, getpid(), 41.2, -9.1, 10200.0);

    SimulationReport report;
    memset(&report, 0, sizeof(report));

    check_safety_violations(states, 2, 500.0, &report, 0.0);
    usleep(5000);

    ASSERT(fabs(report.violations[0].position_1.latitude  - 41.2) < 1e-6 &&
           fabs(report.violations[0].position_2.altitude  - 10200.0) < 1e-1,
           "violation positions not correctly recorded");
}

/* ─────────────────────────────────────────
 * US102 — Flight statuses marked on violation
 * ───────────────────────────────────────── */

void test_flight_statuses_marked_on_violation() {
    TEST("flight statuses set to 1 (VIOLATED) on violation");

    FlightState states[2];
    states[0] = make_state(0, getpid(), 41.0, -9.0, 10000.0);
    states[1] = make_state(1, getpid(), 41.0, -9.0, 10000.0);

    SimulationReport report;
    memset(&report, 0, sizeof(report));

    check_safety_violations(states, 2, 500.0, &report, 0.0);
    usleep(5000);

    ASSERT(report.flight_statuses[0] == 1 &&
           report.flight_statuses[1] == 1,
           "expected flight_statuses[0] and [1] to be 1 (VIOLATED)");
}

/* ─────────────────────────────────────────
 * US102 — Inactive flights are skipped
 * ───────────────────────────────────────── */

void test_inactive_flights_are_skipped() {
    TEST("inactive flights are not checked for violations");

    FlightState states[2];
    states[0] = make_state(0, getpid(), 41.0, -9.0, 10000.0);
    states[1] = make_state(1, getpid(), 41.0, -9.0, 10000.0);
    states[1].active = 0;

    SimulationReport report;
    memset(&report, 0, sizeof(report));

    int violations = check_safety_violations(
            states, 2, 500.0, &report, 0.0);

    ASSERT(violations == 0,
           "expected zero violations — inactive flight should be skipped");
}

/* ─────────────────────────────────────────
 * US102 — Multiple violations detected
 * ───────────────────────────────────────── */

void test_multiple_violations_detected() {
    TEST("multiple violations detected in one step (3 flights)");

    FlightState states[3];
    states[0] = make_state(0, getpid(), 41.0, -9.0, 10000.0);
    states[1] = make_state(1, getpid(), 41.0, -9.0, 10000.0);
    states[2] = make_state(2, getpid(), 41.0, -9.0, 10000.0);

    SimulationReport report;
    memset(&report, 0, sizeof(report));

    int violations = check_safety_violations(
            states, 3, 500.0, &report, 0.0);
    usleep(5000);

    /* pairs: (0,1), (0,2), (1,2) → all violate */
    ASSERT(violations == 3,
           "expected 3 violations for 3 co-located flights");
}

/* ─────────────────────────────────────────
 * US102.4/5 — SIGUSR1 handled without termination
 * ───────────────────────────────────────── */

void test_sigusr1_handled_without_termination() {
    TEST("SIGUSR1 handled without terminating the process");

    int before = global_sigusr1_count;
    kill(getpid(), SIGUSR1);
    usleep(5000);

    ASSERT(global_sigusr1_count == before + 1,
           "expected SIGUSR1 to increment counter by 1");
}

/* ─────────────────────────────────────────
 * US102.6/7 — SIGTERM handled with cleanup
 * ───────────────────────────────────────── */

void test_sigterm_handled_by_process() {
    TEST("SIGTERM is received and handled by process");

    int before = global_sigterm_count;
    kill(getpid(), SIGTERM);
    usleep(5000);

    ASSERT(global_sigterm_count == before + 1,
           "expected SIGTERM to increment counter by 1");
}

/* ─────────────────────────────────────────
 * US102 — Distance computation
 * ───────────────────────────────────────── */

void test_distance_between_co_located_aircraft_is_zero() {
    TEST("distance between co-located aircraft is zero");

    Position p = { .latitude = 41.0, .longitude = -9.0, .altitude = 10000.0 };
    double d = compute_distance(&p, &p);

    ASSERT(fabs(d) < 1e-6, "expected zero distance for same position");
}

void test_distance_only_altitude_difference() {
    TEST("distance with only altitude difference is correct");

    Position p1 = { .latitude = 41.0, .longitude = -9.0, .altitude = 10000.0 };
    Position p2 = { .latitude = 41.0, .longitude = -9.0, .altitude = 10300.0 };
    double d = compute_distance(&p1, &p2);

    ASSERT(fabs(d - 300.0) < 1.0,
           "expected distance ~300 m for 300 m altitude difference");
}

/* ─────────────────────────────────────────
 * US102 — Predicted violations (US101 integration)
 * ───────────────────────────────────────── */

void test_predicted_violation_detected() {
    TEST("predicted violation detected when trajectories converge");

    FlightState states[2];
    memset(states, 0, sizeof(states));

    /* Flight 0: currently at lat=41.0, previously at lat=41.5
     * → moving south (decreasing lat), next predicted: 40.5       */
    states[0].flight_id = 0;
    states[0].active    = 1;
    states[0].pid       = getpid();
    states[0].current.latitude  = 41.0;
    states[0].current.longitude = -9.0;
    states[0].current.altitude  = 10000.0;
    states[0].history[0].latitude  = 41.5;
    states[0].history[0].longitude = -9.0;
    states[0].history[0].altitude  = 10000.0;
    states[0].history_count = 1;

    /* Flight 1: currently at lat=40.6, previously at lat=40.0
     * → moving north (increasing lat), next predicted: 41.2
     * Both predicted positions are close → within 2x threshold   */
    states[1].flight_id = 1;
    states[1].active    = 1;
    states[1].pid       = getpid();
    states[1].current.latitude  = 40.6;
    states[1].current.longitude = -9.0;
    states[1].current.altitude  = 10000.0;
    states[1].history[0].latitude  = 40.0;
    states[1].history[0].longitude = -9.0;
    states[1].history[0].altitude  = 10000.0;
    states[1].history_count = 1;

    SimulationReport report;
    memset(&report, 0, sizeof(report));

    /* threshold = 100 000 m (100 km) so 2x = 200 km;
     * predicted positions are ~100 km apart → within 2x threshold */
    int predictions = check_predicted_violations(
            states, 2, 100000.0, &report, 0.0);

    ASSERT(predictions > 0,
           "expected at least one predicted violation for converging flights");
}

void test_no_predicted_violation_when_diverging() {
    TEST("no predicted violation when trajectories diverge");

    FlightState states[2];
    memset(states, 0, sizeof(states));

    states[0].flight_id = 0;
    states[0].active    = 1;
    states[0].pid       = getpid();
    states[0].current.latitude  = 41.0;
    states[0].current.longitude = -9.5;
    states[0].current.altitude  = 10000.0;
    states[0].history[0].latitude  = 41.0;
    states[0].history[0].longitude = -9.0;
    states[0].history[0].altitude  = 10000.0;
    states[0].history_count = 1;

    states[1].flight_id = 1;
    states[1].active    = 1;
    states[1].pid       = getpid();
    states[1].current.latitude  = 41.0;
    states[1].current.longitude = -8.5;
    states[1].current.altitude  = 10000.0;
    states[1].history[0].latitude  = 41.0;
    states[1].history[0].longitude = -9.0;
    states[1].history[0].altitude  = 10000.0;
    states[1].history_count = 1;

    SimulationReport report;
    memset(&report, 0, sizeof(report));

    int predictions = check_predicted_violations(
            states, 2, 100.0, &report, 0.0);

    ASSERT(predictions == 0,
           "expected zero predicted violations for diverging flights");
}

/* ─────────────────────────────────────────
 * Main
 * ───────────────────────────────────────── */

int main(void) {
    /* Register handlers globally so that calls to
     * check_safety_violations() which send SIGUSR1/SIGTERM
     * to getpid() do not terminate this test process. */
    signal(SIGUSR1, global_sigusr1_handler);
    signal(SIGTERM, global_sigterm_handler);

    printf("\n========================================\n");
    printf("  AISafe US102 — Safety Violations Test Suite\n");
    printf("========================================\n\n");

    printf("[ US102.1 — Violation Detection ]\n");
    test_violation_detected_when_below_threshold();
    test_no_violation_when_above_threshold();
    test_no_violation_at_exact_threshold();

    printf("\n[ US102.2 — Violation Logging ]\n");
    test_violation_event_is_logged();
    test_violation_timestamp_is_correct();
    test_violation_records_correct_flight_ids();
    test_violation_records_positions();
    test_flight_statuses_marked_on_violation();

    printf("\n[ US102 — Edge Cases ]\n");
    test_inactive_flights_are_skipped();
    test_multiple_violations_detected();

    printf("\n[ US102.4/5 — Signal Handling ]\n");
    test_sigusr1_handled_without_termination();
    test_sigterm_handled_by_process();

    printf("\n[ US102 — Distance Computation ]\n");
    test_distance_between_co_located_aircraft_is_zero();
    test_distance_only_altitude_difference();

    printf("\n[ US102 — Predicted Violations (US101 integration) ]\n");
    test_predicted_violation_detected();
    test_no_predicted_violation_when_diverging();

    printf("\n========================================\n");
    printf("  Results: %d/%d passed", tests_passed, tests_run);
    if (tests_failed > 0) {
        printf(" (%d FAILED)", tests_failed);
    }
    printf("\n========================================\n\n");

    return tests_failed > 0 ? EXIT_FAILURE : EXIT_SUCCESS;
}