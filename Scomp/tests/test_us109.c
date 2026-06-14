/*
 * test_us109.c — Unit tests for US109: Generate and Store Final Simulation Report
 */

#include <assert.h>
#include <stdio.h>
#include <string.h>
#include "../src/simulation.h"
#include "../src/report.h"

/* Helper — returns 1 if the file contains the substring, 0 otherwise. */
static int file_contains(const char *filename, const char *needle) {
    FILE *f = fopen(filename, "r");
    if (!f) return 0;
    char line[512];
    int found = 0;
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, needle)) { found = 1; break; }
    }
    fclose(f);
    return found;
}

/* US109.2 — report includes total number of flights */
static void test_report_includes_total_flights(void) {
    SimulationReport r;
    memset(&r, 0, sizeof(r));
    r.total_flights    = 3;
    r.total_violations = 0;
    r.pass             = 1;

    write_report(&r, "/tmp/test109_flights.txt");

    assert(file_contains("/tmp/test109_flights.txt", "Total flights"));
    assert(file_contains("/tmp/test109_flights.txt", "3"));

    printf("PASS: test_report_includes_total_flights\n");
}

/* US109.2 — report includes individual flight statuses */
static void test_report_includes_flight_statuses(void) {
    SimulationReport r;
    memset(&r, 0, sizeof(r));
    r.total_flights       = 2;
    r.flight_statuses[0]  = 0;  /* OK */
    r.flight_statuses[1]  = 1;  /* VIOLATED */

    write_report(&r, "/tmp/test109_statuses.txt");

    assert(file_contains("/tmp/test109_statuses.txt", "OK"));
    assert(file_contains("/tmp/test109_statuses.txt", "VIOLATED"));

    printf("PASS: test_report_includes_flight_statuses\n");
}

/* US109.3 — report includes violation events with timestamps and positions */
static void test_report_includes_violation_events(void) {
    SimulationReport r;
    memset(&r, 0, sizeof(r));
    r.total_flights    = 2;
    r.total_violations = 1;
    r.pass             = 0;

    ViolationEvent *v     = &r.violations[0];
    v->timestamp          = 42.5;
    v->flight_id_1        = 0;
    v->flight_id_2        = 1;
    v->position_1.latitude  = 41.2481;
    v->position_1.longitude = -8.6814;
    v->position_1.altitude  = 10000.0;
    v->velocity_1.vx = 100.0; v->velocity_1.vy = 50.0; v->velocity_1.vz = 0.0;
    v->position_2.latitude  = 41.2490;
    v->position_2.longitude = -8.6800;
    v->position_2.altitude  = 10000.0;
    v->velocity_2.vx = -100.0; v->velocity_2.vy = 50.0; v->velocity_2.vz = 0.0;

    write_report(&r, "/tmp/test109_violations.txt");

    assert(file_contains("/tmp/test109_violations.txt", "42.50"));
    assert(file_contains("/tmp/test109_violations.txt", "41.2481"));
    assert(file_contains("/tmp/test109_violations.txt", "100.0000"));

    printf("PASS: test_report_includes_violation_events\n");
}

/* US109.4 — report clearly indicates PASS or FAIL */
static void test_report_indicates_pass_fail(void) {
    SimulationReport pass_r;
    memset(&pass_r, 0, sizeof(pass_r));
    pass_r.total_flights    = 2;
    pass_r.total_violations = 0;
    pass_r.pass             = 1;

    SimulationReport fail_r;
    memset(&fail_r, 0, sizeof(fail_r));
    fail_r.total_flights    = 2;
    fail_r.total_violations = 5;
    fail_r.pass             = 0;

    write_report(&pass_r, "/tmp/test109_pass.txt");
    write_report(&fail_r, "/tmp/test109_fail.txt");

    assert(file_contains("/tmp/test109_pass.txt", "PASS"));
    assert(file_contains("/tmp/test109_fail.txt", "FAIL"));

    printf("PASS: test_report_indicates_pass_fail\n");
}

/* US109.5 — report is saved to a file */
static void test_report_is_saved_to_file(void) {
    SimulationReport r;
    memset(&r, 0, sizeof(r));
    r.total_flights    = 1;
    r.total_violations = 0;
    r.pass             = 1;

    write_report(&r, "/tmp/test109_saved.txt");

    FILE *f = fopen("/tmp/test109_saved.txt", "r");
    assert(f != NULL);
    fclose(f);

    printf("PASS: test_report_is_saved_to_file\n");
}

/* make_report_filename generates a non-empty timestamped name */
static void test_make_report_filename(void) {
    char buf[64];
    make_report_filename(buf, sizeof(buf));
    assert(strlen(buf) > 0);
    assert(strstr(buf, "simulation_report_") != NULL);
    printf("PASS: test_make_report_filename\n");
}

int main(void) {
    printf("=== US109 Tests ===\n\n");
    test_report_includes_total_flights();
    test_report_includes_flight_statuses();
    test_report_includes_violation_events();
    test_report_indicates_pass_fail();
    test_report_is_saved_to_file();
    test_make_report_filename();
    printf("\nAll US109 tests passed.\n");
    return 0;
}