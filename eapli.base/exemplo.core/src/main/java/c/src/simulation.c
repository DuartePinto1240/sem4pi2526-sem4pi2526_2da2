#include "simulation.h"
#include "flight.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <math.h>
#include <sys/wait.h>
#include <time.h>

/* ─────────────────────────────────────────
 * Parameter Validation
 * ───────────────────────────────────────── */

int validate_parameters(const SimulationConfig *config) {
    if (config == NULL) {
        fprintf(stderr, "[ERROR] Config is NULL\n");
        return -1;
    }
    if (config->num_flights <= 0) {
        fprintf(stderr,
                "[ERROR] num_flights must be positive (got %d)\n",
                config->num_flights);
        return -1;
    }
    if (config->num_flights > MAX_FLIGHTS) {
        fprintf(stderr,
                "[ERROR] num_flights exceeds MAX_FLIGHTS (%d)\n",
                MAX_FLIGHTS);
        return -1;
    }
    if (config->total_steps <= 0) {
        fprintf(stderr,
                "[ERROR] total_steps must be positive (got %d)\n",
                config->total_steps);
        return -1;
    }
    if (config->time_step_seconds <= 0) {
        fprintf(stderr,
                "[ERROR] time_step_seconds must be positive (got %d)\n",
                config->time_step_seconds);
        return -1;
    }
    if (config->safety_threshold_m <= 0.0) {
        fprintf(stderr,
                "[ERROR] safety_threshold_m must be positive (got %.2f)\n",
                config->safety_threshold_m);
        return -1;
    }
    if (config->max_violations <= 0) {
        fprintf(stderr,
                "[ERROR] max_violations must be positive (got %d)\n",
                config->max_violations);
        return -1;
    }
    return 0;
}

/* ─────────────────────────────────────────
 * Distance Computation
 * ───────────────────────────────────────── */

double compute_distance(const Position *p1, const Position *p2) {
    /* simplified Euclidean distance in 3D space
     * using degree-to-metre approximation */
    double dlat = (p1->latitude  - p2->latitude)  * 111000.0;
    double dlon = (p1->longitude - p2->longitude)  * 111000.0;
    double dalt =  p1->altitude  - p2->altitude;
    return sqrt(dlat * dlat + dlon * dlon + dalt * dalt);
}

/* ─────────────────────────────────────────
 * Read Position Updates
 * ───────────────────────────────────────── */

int read_all_positions(FlightState *states, int num_flights) {
    int updates = 0;

    for (int i = 0; i < num_flights; i++) {
        if (!states[i].active) continue;

        Position pos;
        ssize_t bytes = read(states[i].pipe_fd,
                             &pos, sizeof(Position));

        if (bytes == sizeof(Position)) {
            /* store in history */
            if (states[i].history_count < MAX_HISTORY) {
                states[i].history[states[i].history_count++] =
                        states[i].current;
            }
            states[i].current = pos;
            updates++;

        } else if (bytes == 0) {
            /* pipe closed — flight finished */
            states[i].active = 0;
            close(states[i].pipe_fd);
            printf("[MAIN] Flight %d finished\n", states[i].flight_id);

        } else {
            perror("[MAIN] read error");
            states[i].active = 0;
        }
    }

    return updates;
}

/* ─────────────────────────────────────────
 * Safety Violation Detection
 * ───────────────────────────────────────── */

int check_safety_violations(FlightState *states,
                             int num_flights,
                             double threshold_m,
                             SimulationReport *report,
                             double timestamp) {
    int new_violations = 0;

    for (int i = 0; i < num_flights; i++) {
        if (!states[i].active) continue;

        for (int j = i + 1; j < num_flights; j++) {
            if (!states[j].active) continue;

            double dist = compute_distance(
                    &states[i].current,
                    &states[j].current);

            if (dist < threshold_m) {
                printf("[MAIN] VIOLATION: flight %d and flight %d"
                       " — distance=%.1f m (threshold=%.1f m)\n",
                       states[i].flight_id,
                       states[j].flight_id,
                       dist, threshold_m);

                /* record violation */
                if (report->total_violations < MAX_VIOLATIONS) {
                    ViolationEvent *v =
                            &report->violations[report->total_violations];
                    v->timestamp   = timestamp;
                    v->flight_id_1 = states[i].flight_id;
                    v->flight_id_2 = states[j].flight_id;
                    v->position_1  = states[i].current;
                    v->position_2  = states[j].current;

                    /* simplified velocity vectors */
                    if (states[i].history_count > 0) {
                        Position *prev = &states[i].history[
                                states[i].history_count - 1];
                        v->velocity_1.vx =
                                states[i].current.latitude  - prev->latitude;
                        v->velocity_1.vy =
                                states[i].current.longitude - prev->longitude;
                        v->velocity_1.vz =
                                states[i].current.altitude  - prev->altitude;
                    }
                    if (states[j].history_count > 0) {
                        Position *prev = &states[j].history[
                                states[j].history_count - 1];
                        v->velocity_2.vx =
                                states[j].current.latitude  - prev->latitude;
                        v->velocity_2.vy =
                                states[j].current.longitude - prev->longitude;
                        v->velocity_2.vz =
                                states[j].current.altitude  - prev->altitude;
                    }
                }

                report->total_violations++;
                report->flight_statuses[i] = 1;
                report->flight_statuses[j] = 1;
                new_violations++;

                /* notify involved processes */
                kill(states[i].pid, SIGUSR1);
                kill(states[j].pid, SIGUSR1);
            }
        }
    }

    return new_violations;
}

/* ─────────────────────────────────────────
 * Report Writing
 * ───────────────────────────────────────── */

void write_report(const SimulationReport *report,
                  const char *filename) {
    FILE *f = fopen(filename, "w");
    if (!f) {
        perror("[MAIN] Cannot open report file");
        return;
    }

    time_t now = time(NULL);
    fprintf(f, "========================================\n");
    fprintf(f, "  AISafe Simulation Report\n");
    fprintf(f, "  Generated: %s", ctime(&now));
    fprintf(f, "========================================\n\n");

    fprintf(f, "--- Summary ---\n");
    fprintf(f, "Total flights    : %d\n", report->total_flights);
    fprintf(f, "Total violations : %d\n", report->total_violations);
    fprintf(f, "Result           : %s\n\n",
            report->pass ? "PASS" : "FAIL");

    fprintf(f, "--- Flight Statuses ---\n");
    for (int i = 0; i < report->total_flights; i++) {
        const char *status;
        switch (report->flight_statuses[i]) {
            case 0:  status = "OK";       break;
            case 1:  status = "VIOLATED"; break;
            case 2:  status = "ABORTED";  break;
            default: status = "UNKNOWN";  break;
        }
        fprintf(f, "Flight %d : %s\n", i, status);
    }

    fprintf(f, "\n--- Safety Violation Events ---\n");
    int max = report->total_violations < MAX_VIOLATIONS
              ? report->total_violations : MAX_VIOLATIONS;

    if (max == 0) {
        fprintf(f, "No violations recorded.\n");
    } else {
        for (int i = 0; i < max; i++) {
            const ViolationEvent *v = &report->violations[i];
            fprintf(f, "\nViolation #%d\n", i + 1);
            fprintf(f, "  Timestamp : %.2f s\n",   v->timestamp);
            fprintf(f, "  Flight 1  : %d\n",        v->flight_id_1);
            fprintf(f, "    Lat=%.4f Lon=%.4f Alt=%.1f\n",
                    v->position_1.latitude,
                    v->position_1.longitude,
                    v->position_1.altitude);
            fprintf(f, "    Velocity: vx=%.4f vy=%.4f vz=%.4f\n",
                    v->velocity_1.vx,
                    v->velocity_1.vy,
                    v->velocity_1.vz);
            fprintf(f, "  Flight 2  : %d\n",        v->flight_id_2);
            fprintf(f, "    Lat=%.4f Lon=%.4f Alt=%.1f\n",
                    v->position_2.latitude,
                    v->position_2.longitude,
                    v->position_2.altitude);
            fprintf(f, "    Velocity: vx=%.4f vy=%.4f vz=%.4f\n",
                    v->velocity_2.vx,
                    v->velocity_2.vy,
                    v->velocity_2.vz);
        }
    }

    fprintf(f, "\n========================================\n");
    fprintf(f, "  End of Report\n");
    fprintf(f, "========================================\n");

    fclose(f);
    printf("[MAIN] Report saved to: %s\n", filename);
}

/* ─────────────────────────────────────────
 * Main Simulation
 * ───────────────────────────────────────── */

int start_simulation(const SimulationConfig *config,
                     FlightState *states,
                     SimulationReport *report) {

    /* initialise report */
    memset(report, 0, sizeof(SimulationReport));
    report->total_flights = config->num_flights;

    /* initialise states */
    memset(states, 0, sizeof(FlightState) * config->num_flights);

    /* create pipes and fork one process per flight */
    for (int i = 0; i < config->num_flights; i++) {
        int fd[2];
        if (pipe(fd) < 0) {
            perror("[MAIN] pipe error");
            return -1;
        }

        pid_t pid = fork();
        if (pid < 0) {
            perror("[MAIN] fork error");
            return -1;
        }

        if (pid == 0) {
            /* child process */
            close(fd[0]); /* close read end */
            execute_flight(i, fd[1],
                           config->total_steps,
                           config->time_step_seconds);
            /* execute_flight calls exit() — never returns */
        }

        /* parent process */
        close(fd[1]); /* close write end */
        states[i].flight_id     = i;
        states[i].pid           = pid;
        states[i].pipe_fd       = fd[0];
        states[i].active        = 1;
        states[i].history_count = 0;
        states[i].violated      = 0;

        printf("[MAIN] Forked flight %d (PID %d)\n", i, pid);
    }

    /* main simulation loop */
    for (int step = 0; step < config->total_steps; step++) {
        printf("[MAIN] --- Time step %d ---\n", step);

        /* read position updates from all flights */
        read_all_positions(states, config->num_flights);

        /* check for safety violations */
        double timestamp = step * config->time_step_seconds;
        int violations = check_safety_violations(
                states, config->num_flights,
                config->safety_threshold_m,
                report, timestamp);

        /* abort if too many violations */
        if (report->total_violations >= config->max_violations) {
            printf("[MAIN] Max violations reached — aborting.\n");
            for (int i = 0; i < config->num_flights; i++) {
                if (states[i].active) {
                    kill(states[i].pid, SIGTERM);
                    report->flight_statuses[i] = 2;
                }
            }
            break;
        }

        (void) violations;
    }

    /* wait for all child processes */
    for (int i = 0; i < config->num_flights; i++) {
        int status;
        waitpid(states[i].pid, &status, 0);
        printf("[MAIN] Flight %d exited\n", states[i].flight_id);
    }

    /* determine pass/fail */
    report->pass = (report->total_violations < config->max_violations)
                   ? 1 : 0;

    return 0;
}