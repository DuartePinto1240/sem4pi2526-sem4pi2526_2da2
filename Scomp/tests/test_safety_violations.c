#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <math.h>

#include "../src/simulation.h"
#include "../src/flight.h"

static int run = 0, passed = 0, failed = 0;

#define TEST(name)  do { printf("  %-55s", name); run++; } while(0)
#define PASS()      do { printf("PASS\n"); passed++; } while(0)
#define FAIL(msg)   do { printf("FAIL - %s\n", msg); failed++; } while(0)
#define CHECK(c, m) do { if (c) PASS(); else FAIL(m); } while(0)

/* need handlers registered before tests because check_safety_violations
 * sends SIGUSR1 to the pids in the state array — and in unit tests
 * those are set to getpid(), so the signal comes straight back here */
static volatile sig_atomic_t sigusr1_count = 0;
static volatile sig_atomic_t sigterm_count = 0;

static void on_sigusr1(int sig) {
    (void)sig;
    sigset_t all;
    sigfillset(&all);
    sigprocmask(SIG_BLOCK, &all, NULL);
    sigusr1_count++;
    sigprocmask(SIG_UNBLOCK, &all, NULL);
}

static void on_sigterm(int sig) {
    (void)sig;
    sigterm_count++;
}

static FlightState mkstate(int id, double lat, double lon, double alt) {
    FlightState s;
    memset(&s, 0, sizeof(s));
    s.flight_id = id;
    s.pid       = getpid();
    s.active    = 1;
    s.current.latitude  = lat;
    s.current.longitude = lon;
    s.current.altitude  = alt;
    return s;
}

void test_violation_below_threshold() {
    TEST("violation detected when distance < threshold");
    FlightState st[2];
    st[0] = mkstate(0, 41.0, -9.0, 10000.0);
    st[1] = mkstate(1, 41.0, -9.0, 10000.0);
    SimulationReport r; memset(&r, 0, sizeof(r));
    int v = check_safety_violations(st, 2, 500.0, &r, 0.0);
    usleep(5000);
    CHECK(v > 0, "expected at least one violation");
}

void test_no_violation_above_threshold() {
    TEST("no violation when distance > threshold");
    FlightState st[2];
    st[0] = mkstate(0, 41.0, -9.0, 10000.0);
    st[1] = mkstate(1, 38.0, -8.0,  5000.0);
    SimulationReport r; memset(&r, 0, sizeof(r));
    int v = check_safety_violations(st, 2, 500.0, &r, 0.0);
    CHECK(v == 0, "expected zero violations");
}

void test_no_violation_at_threshold() {
    TEST("no violation when distance equals threshold");
    FlightState st[2];
    st[0] = mkstate(0, 41.0, -9.0, 10000.0);
    st[1] = mkstate(1, 41.0, -9.0, 10500.0); /* exactly 500 m alt diff */
    SimulationReport r; memset(&r, 0, sizeof(r));
    int v = check_safety_violations(st, 2, 500.0, &r, 0.0);
    CHECK(v == 0, "expected zero violations when distance == threshold");
}

void test_violation_is_logged() {
    TEST("violation is logged in report");
    FlightState st[2];
    st[0] = mkstate(0, 41.0, -9.0, 10000.0);
    st[1] = mkstate(1, 41.0, -9.0, 10000.0);
    SimulationReport r; memset(&r, 0, sizeof(r));
    check_safety_violations(st, 2, 500.0, &r, 42.0);
    usleep(5000);
    CHECK(r.total_violations > 0, "expected total_violations > 0");
}

void test_violation_timestamp() {
    TEST("violation event has correct timestamp");
    FlightState st[2];
    st[0] = mkstate(0, 41.0, -9.0, 10000.0);
    st[1] = mkstate(1, 41.0, -9.0, 10000.0);
    SimulationReport r; memset(&r, 0, sizeof(r));
    check_safety_violations(st, 2, 500.0, &r, 15.0);
    usleep(5000);
    CHECK(fabs(r.violations[0].timestamp - 15.0) < 1e-6, "wrong timestamp");
}

void test_violation_flight_ids() {
    TEST("violation event records correct flight IDs");
    FlightState st[2];
    st[0] = mkstate(3, 41.0, -9.0, 10000.0);
    st[1] = mkstate(7, 41.0, -9.0, 10000.0);
    SimulationReport r; memset(&r, 0, sizeof(r));
    check_safety_violations(st, 2, 500.0, &r, 0.0);
    usleep(5000);
    CHECK(r.violations[0].flight_id_1 == 3 && r.violations[0].flight_id_2 == 7,
          "expected IDs 3 and 7");
}

void test_violation_positions() {
    TEST("violation event records positions of both aircraft");
    FlightState st[2];
    st[0] = mkstate(0, 41.2, -9.1, 10000.0);
    st[1] = mkstate(1, 41.2, -9.1, 10200.0);
    SimulationReport r; memset(&r, 0, sizeof(r));
    check_safety_violations(st, 2, 500.0, &r, 0.0);
    usleep(5000);
    CHECK(fabs(r.violations[0].position_1.latitude - 41.2) < 1e-6 &&
          fabs(r.violations[0].position_2.altitude - 10200.0) < 0.1,
          "positions not correctly recorded");
}

void test_flight_statuses_on_violation() {
    TEST("flight statuses set to VIOLATED on violation");
    FlightState st[2];
    st[0] = mkstate(0, 41.0, -9.0, 10000.0);
    st[1] = mkstate(1, 41.0, -9.0, 10000.0);
    SimulationReport r; memset(&r, 0, sizeof(r));
    check_safety_violations(st, 2, 500.0, &r, 0.0);
    usleep(5000);
    CHECK(r.flight_statuses[0] == 1 && r.flight_statuses[1] == 1,
          "expected flight_statuses to be 1");
}

void test_inactive_flight_skipped() {
    TEST("inactive flights are not checked for violations");
    FlightState st[2];
    st[0] = mkstate(0, 41.0, -9.0, 10000.0);
    st[1] = mkstate(1, 41.0, -9.0, 10000.0);
    st[1].active = 0;
    SimulationReport r; memset(&r, 0, sizeof(r));
    int v = check_safety_violations(st, 2, 500.0, &r, 0.0);
    CHECK(v == 0, "inactive flight should be skipped");
}

void test_three_colocated_flights() {
    TEST("three co-located flights produce three violations");
    FlightState st[3];
    st[0] = mkstate(0, 41.0, -9.0, 10000.0);
    st[1] = mkstate(1, 41.0, -9.0, 10000.0);
    st[2] = mkstate(2, 41.0, -9.0, 10000.0);
    SimulationReport r; memset(&r, 0, sizeof(r));
    int v = check_safety_violations(st, 3, 500.0, &r, 0.0);
    usleep(5000);
    CHECK(v == 3, "expected 3 violations for pairs (0,1) (0,2) (1,2)");
}

void test_sigusr1_does_not_kill_process() {
    TEST("SIGUSR1 handled without terminating the process");
    int before = sigusr1_count;
    kill(getpid(), SIGUSR1);
    usleep(5000);
    CHECK(sigusr1_count == before + 1, "expected counter to increment by 1");
}

void test_sigterm_is_received() {
    TEST("SIGTERM is received and handled");
    int before = sigterm_count;
    kill(getpid(), SIGTERM);
    usleep(5000);
    CHECK(sigterm_count == before + 1, "expected counter to increment by 1");
}

void test_distance_same_point() {
    TEST("distance between co-located aircraft is zero");
    Position p = { 41.0, -9.0, 10000.0, 0 };
    CHECK(fabs(compute_distance(&p, &p)) < 1e-6, "expected zero");
}

void test_distance_altitude_only() {
    TEST("distance with only altitude difference is correct");
    Position p1 = { 41.0, -9.0, 10000.0, 0 };
    Position p2 = { 41.0, -9.0, 10300.0, 0 };
    CHECK(fabs(compute_distance(&p1, &p2) - 300.0) < 1.0,
          "expected ~300 m for 300 m altitude difference");
}

void test_predicted_violation_converging() {
    TEST("predicted violation detected when trajectories converge");

    FlightState st[2];
    memset(st, 0, sizeof(st));

    /* flight 0: was at 41.5, now at 41.0 -> moving south */
    st[0].flight_id = 0; st[0].active = 1; st[0].pid = getpid();
    st[0].current.latitude  = 41.0; st[0].current.longitude = -9.0;
    st[0].current.altitude  = 10000.0;
    st[0].history[0].latitude  = 41.5; st[0].history[0].longitude = -9.0;
    st[0].history[0].altitude  = 10000.0; st[0].history_count = 1;

    /* flight 1: was at 40.0, now at 40.6 -> moving north */
    st[1].flight_id = 1; st[1].active = 1; st[1].pid = getpid();
    st[1].current.latitude  = 40.6; st[1].current.longitude = -9.0;
    st[1].current.altitude  = 10000.0;
    st[1].history[0].latitude  = 40.0; st[1].history[0].longitude = -9.0;
    st[1].history[0].altitude  = 10000.0; st[1].history_count = 1;

    SimulationReport r; memset(&r, 0, sizeof(r));

    /* predicted positions ~100 km apart, within 2x threshold of 100 km */
    int p = check_predicted_violations(st, 2, 100000.0, &r, 0.0);
    CHECK(p > 0, "expected at least one predicted violation");
}

void test_no_predicted_violation_diverging() {
    TEST("no predicted violation when trajectories diverge");

    FlightState st[2];
    memset(st, 0, sizeof(st));

    /* flight 0 moving west */
    st[0].flight_id = 0; st[0].active = 1; st[0].pid = getpid();
    st[0].current.latitude  = 41.0; st[0].current.longitude = -9.5;
    st[0].current.altitude  = 10000.0;
    st[0].history[0].latitude  = 41.0; st[0].history[0].longitude = -9.0;
    st[0].history[0].altitude  = 10000.0; st[0].history_count = 1;

    /* flight 1 moving east */
    st[1].flight_id = 1; st[1].active = 1; st[1].pid = getpid();
    st[1].current.latitude  = 41.0; st[1].current.longitude = -8.5;
    st[1].current.altitude  = 10000.0;
    st[1].history[0].latitude  = 41.0; st[1].history[0].longitude = -9.0;
    st[1].history[0].altitude  = 10000.0; st[1].history_count = 1;

    SimulationReport r; memset(&r, 0, sizeof(r));
    int p = check_predicted_violations(st, 2, 100.0, &r, 0.0);
    CHECK(p == 0, "expected zero predicted violations for diverging flights");
}

int main(void) {
    signal(SIGUSR1, on_sigusr1);
    signal(SIGTERM, on_sigterm);

    printf("\n========================================\n");
    printf("  AISafe US102 - Safety Violations Test Suite\n");
    printf("========================================\n\n");

    printf("[ Violation Detection ]\n");
    test_violation_below_threshold();
    test_no_violation_above_threshold();
    test_no_violation_at_threshold();

    printf("\n[ Violation Logging ]\n");
    test_violation_is_logged();
    test_violation_timestamp();
    test_violation_flight_ids();
    test_violation_positions();
    test_flight_statuses_on_violation();

    printf("\n[ Edge Cases ]\n");
    test_inactive_flight_skipped();
    test_three_colocated_flights();

    printf("\n[ Signal Handling ]\n");
    test_sigusr1_does_not_kill_process();
    test_sigterm_is_received();

    printf("\n[ Distance Computation ]\n");
    test_distance_same_point();
    test_distance_altitude_only();

    printf("\n[ Predicted Violations ]\n");
    test_predicted_violation_converging();
    test_no_predicted_violation_diverging();

    printf("\n========================================\n");
    printf("  Results: %d/%d passed", passed, run);
    if (failed > 0) printf(" (%d FAILED)", failed);
    printf("\n========================================\n\n");

    return failed > 0 ? EXIT_FAILURE : EXIT_SUCCESS;
}