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
        printf("  %-50s", name); \
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
 * US100.7 — Parameter Validation
 * ───────────────────────────────────────── */

void test_valid_parameters_accepted() {
    TEST("valid parameters are accepted");
    SimulationConfig cfg = {
        .num_flights        = 3,
        .total_steps        = 10,
        .time_step_seconds  = 1,
        .safety_threshold_m = 1000.0,
        .max_violations     = 5
    };
    ASSERT(validate_parameters(&cfg) == 0,
           "expected 0 for valid config");
}

void test_null_config_rejected() {
    TEST("null config is rejected");
    ASSERT(validate_parameters(NULL) == -1,
           "expected -1 for NULL config");
}

void test_zero_flights_rejected() {
    TEST("zero num_flights is rejected");
    SimulationConfig cfg = {
        .num_flights        = 0,
        .total_steps        = 10,
        .time_step_seconds  = 1,
        .safety_threshold_m = 1000.0,
        .max_violations     = 5
    };
    ASSERT(validate_parameters(&cfg) == -1,
           "expected -1 for num_flights=0");
}

void test_negative_flights_rejected() {
    TEST("negative num_flights is rejected");
    SimulationConfig cfg = {
        .num_flights        = -1,
        .total_steps        = 10,
        .time_step_seconds  = 1,
        .safety_threshold_m = 1000.0,
        .max_violations     = 5
    };
    ASSERT(validate_parameters(&cfg) == -1,
           "expected -1 for num_flights=-1");
}

void test_excess_flights_rejected() {
    TEST("num_flights > MAX_FLIGHTS is rejected");
    SimulationConfig cfg = {
        .num_flights        = MAX_FLIGHTS + 1,
        .total_steps        = 10,
        .time_step_seconds  = 1,
        .safety_threshold_m = 1000.0,
        .max_violations     = 5
    };
    ASSERT(validate_parameters(&cfg) == -1,
           "expected -1 for num_flights > MAX_FLIGHTS");
}

void test_zero_steps_rejected() {
    TEST("zero total_steps is rejected");
    SimulationConfig cfg = {
        .num_flights        = 3,
        .total_steps        = 0,
        .time_step_seconds  = 1,
        .safety_threshold_m = 1000.0,
        .max_violations     = 5
    };
    ASSERT(validate_parameters(&cfg) == -1,
           "expected -1 for total_steps=0");
}

void test_zero_time_step_rejected() {
    TEST("zero time_step_seconds is rejected");
    SimulationConfig cfg = {
        .num_flights        = 3,
        .total_steps        = 10,
        .time_step_seconds  = 0,
        .safety_threshold_m = 1000.0,
        .max_violations     = 5
    };
    ASSERT(validate_parameters(&cfg) == -1,
           "expected -1 for time_step_seconds=0");
}

void test_negative_threshold_rejected() {
    TEST("negative safety_threshold_m is rejected");
    SimulationConfig cfg = {
        .num_flights        = 3,
        .total_steps        = 10,
        .time_step_seconds  = 1,
        .safety_threshold_m = -100.0,
        .max_violations     = 5
    };
    ASSERT(validate_parameters(&cfg) == -1,
           "expected -1 for safety_threshold_m=-100");
}

void test_zero_threshold_rejected() {
    TEST("zero safety_threshold_m is rejected");
    SimulationConfig cfg = {
        .num_flights        = 3,
        .total_steps        = 10,
        .time_step_seconds  = 1,
        .safety_threshold_m = 0.0,
        .max_violations     = 5
    };
    ASSERT(validate_parameters(&cfg) == -1,
           "expected -1 for safety_threshold_m=0");
}

void test_zero_max_violations_rejected() {
    TEST("zero max_violations is rejected");
    SimulationConfig cfg = {
        .num_flights        = 3,
        .total_steps        = 10,
        .time_step_seconds  = 1,
        .safety_threshold_m = 1000.0,
        .max_violations     = 0
    };
    ASSERT(validate_parameters(&cfg) == -1,
           "expected -1 for max_violations=0");
}

/* ─────────────────────────────────────────
 * US100.5/6 — Pipe communication
 * ───────────────────────────────────────── */

void test_position_sent_and_received_via_pipe() {
    TEST("position update sent and received via pipe");
    int fd[2];
    assert(pipe(fd) == 0);

    Position sent = {
        .latitude  = 41.2481,
        .longitude = -8.6814,
        .altitude  = 10000.0,
        .time_step = 3
    };

    write(fd[1], &sent, sizeof(Position));
    close(fd[1]);

    Position received;
    read(fd[0], &received, sizeof(Position));
    close(fd[0]);

    int ok = (fabs(received.latitude  - sent.latitude)  < 1e-6) &&
             (fabs(received.longitude - sent.longitude) < 1e-6) &&
             (fabs(received.altitude  - sent.altitude)  < 1e-6) &&
             (received.time_step == sent.time_step);

    ASSERT(ok, "received position does not match sent");
}

/* ─────────────────────────────────────────
 * Distance computation
 * ───────────────────────────────────────── */

void test_distance_between_same_points_is_zero() {
    TEST("distance between same points is zero");
    Position p = { .latitude = 41.0, .longitude = -8.0, .altitude = 5000 };
    double d = compute_distance(&p, &p);
    ASSERT(fabs(d) < 1e-6, "expected 0 distance for same point");
}

void test_distance_between_different_points_is_positive() {
    TEST("distance between different points is positive");
    Position p1 = { .latitude = 41.2481, .longitude = -8.6814, .altitude = 1000 };
    Position p2 = { .latitude = 38.7813, .longitude = -9.1359, .altitude = 10000 };
    double d = compute_distance(&p1, &p2);
    ASSERT(d > 0.0, "expected positive distance");
}

void test_distance_is_symmetric() {
    TEST("distance is symmetric");
    Position p1 = { .latitude = 41.2, .longitude = -8.6, .altitude = 1000 };
    Position p2 = { .latitude = 38.7, .longitude = -9.1, .altitude = 5000 };
    double d1 = compute_distance(&p1, &p2);
    double d2 = compute_distance(&p2, &p1);
    ASSERT(fabs(d1 - d2) < 1e-6, "distance is not symmetric");
}

/* ─────────────────────────────────────────
 * US100.2 — Signal handling
 * ───────────────────────────────────────── */

static volatile sig_atomic_t test_sigusr1 = 0;

static void test_sigusr1_handler(int sig) {
    (void) sig;
    test_sigusr1 = 1;
}

void test_sigusr1_is_received() {
    TEST("SIGUSR1 is received and handled");
    test_sigusr1 = 0;
    signal(SIGUSR1, test_sigusr1_handler);
    kill(getpid(), SIGUSR1);
    /* give the signal time to be delivered */
    usleep(1000);
    ASSERT(test_sigusr1 == 1, "SIGUSR1 was not received");
}

/* ─────────────────────────────────────────
 * Position computation
 * ───────────────────────────────────────── */

void test_position_at_step_zero_is_start() {
    TEST("position at step 0 is start position");
    Position pos = compute_next_position(0, 0, 10);
    ASSERT(fabs(pos.latitude - 41.2481) < 1e-3,
           "latitude at step 0 should be start latitude");
}

void test_position_at_last_step_is_end() {
    TEST("position at last step is end position");
    Position pos = compute_next_position(0, 9, 10);
    ASSERT(fabs(pos.latitude - 38.7813) < 1e-3,
           "latitude at last step should be end latitude");
}

void test_position_time_step_is_set() {
    TEST("position time_step field is correctly set");
    Position pos = compute_next_position(0, 5, 10);
    ASSERT(pos.time_step == 5, "time_step should be 5");
}

/* ─────────────────────────────────────────
 * Report writing
 * ───────────────────────────────────────── */

void test_report_is_written_to_file() {
    TEST("report is written to file");
    SimulationReport report;
    memset(&report, 0, sizeof(report));
    report.total_flights    = 2;
    report.total_violations = 0;
    report.pass             = 1;

    const char *filename = "/tmp/test_report.txt";
    write_report(&report, filename);

    FILE *f = fopen(filename, "r");
    ASSERT(f != NULL, "report file was not created");
    if (f) fclose(f);
}

void test_report_contains_pass() {
    TEST("report contains PASS when no violations");
    SimulationReport report;
    memset(&report, 0, sizeof(report));
    report.total_flights    = 2;
    report.total_violations = 0;
    report.pass             = 1;

    const char *filename = "/tmp/test_pass.txt";
    write_report(&report, filename);

    FILE *f = fopen(filename, "r");
    char line[256];
    int found = 0;
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, "PASS")) { found = 1; break; }
    }
    fclose(f);
    ASSERT(found, "PASS not found in report");
}

void test_report_contains_fail() {
    TEST("report contains FAIL when violations exceed max");
    SimulationReport report;
    memset(&report, 0, sizeof(report));
    report.total_flights    = 2;
    report.total_violations = 5;
    report.pass             = 0;

    const char *filename = "/tmp/test_fail.txt";
    write_report(&report, filename);

    FILE *f = fopen(filename, "r");
    char line[256];
    int found = 0;
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, "FAIL")) { found = 1; break; }
    }
    fclose(f);
    ASSERT(found, "FAIL not found in report");
}

/* ─────────────────────────────────────────
 * Main
 * ───────────────────────────────────────── */

int main(void) {
    printf("\n========================================\n");
    printf("  AISafe US100 — Test Suite\n");
    printf("========================================\n\n");

    printf("[ Parameter Validation — US100.7 ]\n");
    test_valid_parameters_accepted();
    test_null_config_rejected();
    test_zero_flights_rejected();
    test_negative_flights_rejected();
    test_excess_flights_rejected();
    test_zero_steps_rejected();
    test_zero_time_step_rejected();
    test_negative_threshold_rejected();
    test_zero_threshold_rejected();
    test_zero_max_violations_rejected();

    printf("\n[ Pipe Communication — US100.5/6 ]\n");
    test_position_sent_and_received_via_pipe();

    printf("\n[ Distance Computation ]\n");
    test_distance_between_same_points_is_zero();
    test_distance_between_different_points_is_positive();
    test_distance_is_symmetric();

    printf("\n[ Signal Handling — US100.2 ]\n");
    test_sigusr1_is_received();

    printf("\n[ Position Computation ]\n");
    test_position_at_step_zero_is_start();
    test_position_at_last_step_is_end();
    test_position_time_step_is_set();

    printf("\n[ Report Writing — US109 ]\n");
    test_report_is_written_to_file();
    test_report_contains_pass();
    test_report_contains_fail();

    printf("\n========================================\n");
    printf("  Results: %d/%d passed", tests_passed, tests_run);
    if (tests_failed > 0) {
        printf(" (%d FAILED)", tests_failed);
    }
    printf("\n========================================\n\n");

    return tests_failed > 0 ? EXIT_FAILURE : EXIT_SUCCESS;
}