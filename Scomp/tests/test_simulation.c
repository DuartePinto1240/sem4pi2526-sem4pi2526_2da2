#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include <signal.h>
#include <math.h>

#include "../src/simulation.h"
#include "../src/flight.h"

static int run = 0, passed = 0, failed = 0;

#define TEST(name)  do { printf("  %-50s", name); run++; } while(0)
#define PASS()      do { printf("PASS\n"); passed++; } while(0)
#define FAIL(msg)   do { printf("FAIL - %s\n", msg); failed++; } while(0)
#define CHECK(c, m) do { if (c) PASS(); else FAIL(m); } while(0)

static SimulationConfig make_cfg(int flights, int steps, int tstep,
                                  double thresh, int maxv) {
    SimulationConfig c;
    c.num_flights        = flights;
    c.total_steps        = steps;
    c.time_step_seconds  = tstep;
    c.safety_threshold_m = thresh;
    c.max_violations     = maxv;
    return c;
}

static SimulationConfig valid_cfg(void) {
    return make_cfg(3, 10, 1, 1000.0, 5);
}


void test_valid_parameters_accepted() {
    TEST("valid parameters are accepted");
    SimulationConfig c = valid_cfg();
    CHECK(validate_parameters(&c) == 0, "expected 0 for valid config");
}

void test_null_config_rejected() {
    TEST("null config is rejected");
    CHECK(validate_parameters(NULL) == -1, "expected -1 for NULL");
}

void test_zero_flights_rejected() {
    TEST("zero num_flights is rejected");
    SimulationConfig c = valid_cfg(); c.num_flights = 0;
    CHECK(validate_parameters(&c) == -1, "expected -1");
}

void test_negative_flights_rejected() {
    TEST("negative num_flights is rejected");
    SimulationConfig c = valid_cfg(); c.num_flights = -1;
    CHECK(validate_parameters(&c) == -1, "expected -1");
}

void test_excess_flights_rejected() {
    TEST("num_flights > MAX_FLIGHTS is rejected");
    SimulationConfig c = valid_cfg(); c.num_flights = MAX_FLIGHTS + 1;
    CHECK(validate_parameters(&c) == -1, "expected -1");
}

void test_zero_steps_rejected() {
    TEST("zero total_steps is rejected");
    SimulationConfig c = valid_cfg(); c.total_steps = 0;
    CHECK(validate_parameters(&c) == -1, "expected -1");
}

void test_zero_time_step_rejected() {
    TEST("zero time_step_seconds is rejected");
    SimulationConfig c = valid_cfg(); c.time_step_seconds = 0;
    CHECK(validate_parameters(&c) == -1, "expected -1");
}

void test_negative_threshold_rejected() {
    TEST("negative safety_threshold_m is rejected");
    SimulationConfig c = valid_cfg(); c.safety_threshold_m = -100.0;
    CHECK(validate_parameters(&c) == -1, "expected -1");
}

void test_zero_threshold_rejected() {
    TEST("zero safety_threshold_m is rejected");
    SimulationConfig c = valid_cfg(); c.safety_threshold_m = 0.0;
    CHECK(validate_parameters(&c) == -1, "expected -1");
}

void test_zero_max_violations_rejected() {
    TEST("zero max_violations is rejected");
    SimulationConfig c = valid_cfg(); c.max_violations = 0;
    CHECK(validate_parameters(&c) == -1, "expected -1");
}

void test_position_sent_and_received_via_pipe() {
    TEST("position update sent and received via pipe");

    int fd[2];
    assert(pipe(fd) == 0);

    Position sent = { 41.2481, -8.6814, 10000.0, 3 };
    write(fd[1], &sent, sizeof(sent));
    close(fd[1]);

    Position got;
    read(fd[0], &got, sizeof(got));
    close(fd[0]);

    int ok = fabs(got.latitude  - sent.latitude)  < 1e-6 &&
             fabs(got.longitude - sent.longitude) < 1e-6 &&
             fabs(got.altitude  - sent.altitude)  < 1e-6 &&
             got.time_step == sent.time_step;

    CHECK(ok, "received position does not match sent");
}

void test_distance_between_same_points_is_zero() {
    TEST("distance between same points is zero");
    Position p = { 41.0, -8.0, 5000, 0 };
    CHECK(fabs(compute_distance(&p, &p)) < 1e-6, "expected 0");
}

void test_distance_between_different_points_is_positive() {
    TEST("distance between different points is positive");
    Position p1 = { 41.2481, -8.6814, 1000, 0 };
    Position p2 = { 38.7813, -9.1359, 10000, 0 };
    CHECK(compute_distance(&p1, &p2) > 0.0, "expected positive distance");
}

void test_distance_is_symmetric() {
    TEST("distance is symmetric");
    Position p1 = { 41.2, -8.6, 1000, 0 };
    Position p2 = { 38.7, -9.1, 5000, 0 };
    double d1 = compute_distance(&p1, &p2);
    double d2 = compute_distance(&p2, &p1);
    CHECK(fabs(d1 - d2) < 1e-6, "distance is not symmetric");
}

static volatile sig_atomic_t got_sigusr1 = 0;

static void on_sigusr1(int sig) {
    (void)sig;
    got_sigusr1 = 1;
}

void test_sigusr1_is_received() {
    TEST("SIGUSR1 is received and handled");
    got_sigusr1 = 0;
    signal(SIGUSR1, on_sigusr1);
    kill(getpid(), SIGUSR1);
    usleep(1000);
    CHECK(got_sigusr1 == 1, "SIGUSR1 was not received");
}

void test_position_at_step_zero_is_start() {
    TEST("position at step 0 is start position");
    Position p = compute_next_position(0, 0, 10);
    CHECK(fabs(p.latitude - 41.2481) < 1e-3, "wrong start latitude");
}

void test_position_at_last_step_is_end() {
    TEST("position at last step is end position");
    Position p = compute_next_position(0, 9, 10);
    CHECK(fabs(p.latitude - 38.7813) < 1e-3, "wrong end latitude");
}

void test_position_time_step_is_set() {
    TEST("position time_step field is correctly set");
    Position p = compute_next_position(0, 5, 10);
    CHECK(p.time_step == 5, "time_step should be 5");
}

void test_report_is_written_to_file() {
    TEST("report is written to file");
    SimulationReport r;
    memset(&r, 0, sizeof(r));
    r.total_flights = 2; r.pass = 1;
    write_report(&r, "/tmp/test_report.txt");
    FILE *f = fopen("/tmp/test_report.txt", "r");
    CHECK(f != NULL, "report file was not created");
    if (f) fclose(f);
}

void test_report_contains_pass() {
    TEST("report contains PASS when no violations");
    SimulationReport r;
    memset(&r, 0, sizeof(r));
    r.total_flights = 2; r.pass = 1;
    write_report(&r, "/tmp/test_pass.txt");
    FILE *f = fopen("/tmp/test_pass.txt", "r");
    char line[256]; int found = 0;
    while (fgets(line, sizeof(line), f))
        if (strstr(line, "PASS")) { found = 1; break; }
    fclose(f);
    CHECK(found, "PASS not found in report");
}

void test_report_contains_fail() {
    TEST("report contains FAIL when violations exceed max");
    SimulationReport r;
    memset(&r, 0, sizeof(r));
    r.total_flights = 2; r.total_violations = 5; r.pass = 0;
    write_report(&r, "/tmp/test_fail.txt");
    FILE *f = fopen("/tmp/test_fail.txt", "r");
    char line[256]; int found = 0;
    while (fgets(line, sizeof(line), f))
        if (strstr(line, "FAIL")) { found = 1; break; }
    fclose(f);
    CHECK(found, "FAIL not found in report");
}

int main(void) {
    printf("\n========================================\n");
    printf("  AISafe US100 - Test Suite\n");
    printf("========================================\n\n");

    printf("[ Parameter Validation ]\n");
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

    printf("\n[ Pipe Communication ]\n");
    test_position_sent_and_received_via_pipe();

    printf("\n[ Distance Computation ]\n");
    test_distance_between_same_points_is_zero();
    test_distance_between_different_points_is_positive();
    test_distance_is_symmetric();

    printf("\n[ Signal Handling ]\n");
    test_sigusr1_is_received();

    printf("\n[ Position Computation ]\n");
    test_position_at_step_zero_is_start();
    test_position_at_last_step_is_end();
    test_position_time_step_is_set();

    printf("\n[ Report Writing ]\n");
    test_report_is_written_to_file();
    test_report_contains_pass();
    test_report_contains_fail();

    printf("\n========================================\n");
    printf("  Results: %d/%d passed", passed, run);
    if (failed > 0) printf(" (%d FAILED)", failed);
    printf("\n========================================\n\n");

    return failed > 0 ? EXIT_FAILURE : EXIT_SUCCESS;
}