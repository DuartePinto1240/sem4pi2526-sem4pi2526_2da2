#include "simulation.h"
#include "flight.h"
#include "report.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

/* ─────────────────────────────────────────
 * Print usage instructions
 * ───────────────────────────────────────── */

static void print_usage(const char *prog) {
    fprintf(stderr,
            "Usage: %s <num_flights> <total_steps> "
            "<time_step_sec> <safety_threshold_m> "
            "<max_violations>\n", prog);
    fprintf(stderr, "Example: %s 3 10 1 50000 5\n", prog);
}

/* ─────────────────────────────────────────
 * Generate report filename with timestamp
 * ───────────────────────────────────────── */



/* ─────────────────────────────────────────
 * Entry point
 * ───────────────────────────────────────── */

int main(int argc, char *argv[]) {

    /* parse arguments */
    if (argc != 6) {
        print_usage(argv[0]);
        return EXIT_FAILURE;
    }

    SimulationConfig config;
    config.num_flights        = atoi(argv[1]);
    config.total_steps        = atoi(argv[2]);
    config.time_step_seconds  = atoi(argv[3]);
    config.safety_threshold_m = atof(argv[4]);
    config.max_violations     = atoi(argv[5]);

    printf("========================================\n");
    printf("  AISafe Flight Simulation\n");
    printf("========================================\n");
    printf("  Flights        : %d\n", config.num_flights);
    printf("  Steps          : %d\n", config.total_steps);
    printf("  Time step      : %d s\n", config.time_step_seconds);
    printf("  Safety thresh  : %.1f m\n", config.safety_threshold_m);
    printf("  Max violations : %d\n", config.max_violations);
    printf("========================================\n\n");

    /* validate parameters */
    if (validate_parameters(&config) != 0) {
        fprintf(stderr, "[ERROR] Invalid simulation parameters.\n");
        return EXIT_FAILURE;
    }

    /* allocate state and report */
    FlightState     states[MAX_FLIGHTS];
    SimulationReport report;
    memset(states, 0, sizeof(states));
    memset(&report, 0, sizeof(report));

    /* run simulation */
    printf("[MAIN] Starting simulation...\n\n");
    int result = start_simulation(&config, states, &report);

    if (result != 0) {
        fprintf(stderr, "[ERROR] Simulation failed.\n");
        return EXIT_FAILURE;
    }


    /* write report to file */
    char filename[128];
    make_report_filename(filename, sizeof(filename));
    print_report_summary(&report);
    write_report(&report, filename);

    return report.pass ? EXIT_SUCCESS : EXIT_FAILURE;
}