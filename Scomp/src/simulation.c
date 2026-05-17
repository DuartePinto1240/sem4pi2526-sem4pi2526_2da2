#include "simulation.h"
#include "flight.h"
#include "report.h"
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
 *
 * Before we do anything, make sure the config
 * values are sane. Better to catch bad input here
 * than halfway through a simulation.
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
 * Distance Between Two Aircraft
 *
 * Straight-line distance in 3D space.
 * We convert degrees to metres using a rough
 * approximation (1 degree ≈ 111 km) — good enough
 * for detecting nearby aircraft, not for navigation.
 * ───────────────────────────────────────── */

double compute_distance(const Position *p1, const Position *p2) {
    double dlat = (p1->latitude  - p2->latitude)  * 111000.0;
    double dlon = (p1->longitude - p2->longitude)  * 111000.0;
    double dalt =  p1->altitude  - p2->altitude;
    return sqrt(dlat * dlat + dlon * dlon + dalt * dalt);
}

/* ─────────────────────────────────────────
 * Step Synchronisation
 *
 * The parent process acts as a conductor:
 * it tells each flight when to move, then waits
 * until every active flight has reported back
 * before moving to the next step.
 *
 * How it works:
 *   1. Send a 1-byte token to each active flight
 *      through the control pipe — this unblocks
 *      the child that was waiting for its turn.
 *   2. Read one Position back from each active
 *      flight. We block here until everyone replies.
 *
 * If a child closes its pipe (flight is done),
 * we mark it inactive and move on.
 * ───────────────────────────────────────── */

int synchronize_step(FlightState *states, int num_flights, int step) {
    /* wake up all active children for this step */
    char token = (char)(step & 0xFF);
    for (int i = 0; i < num_flights; i++) {
        if (!states[i].active) continue;
        ssize_t w = write(states[i].pipe_step, &token, 1);
        if (w < 0) {
            perror("[MAIN] write step token error");
            states[i].active = 0;
        }
    }

    /* collect positions from every child we just woke */
    int updates = 0;
    for (int i = 0; i < num_flights; i++) {
        if (!states[i].active) continue;

        Position pos;
        ssize_t bytes = read(states[i].pipe_read,
                             &pos, sizeof(Position));

        if (bytes == (ssize_t)sizeof(Position)) {
            /* save the current position to history before overwriting it */
            if (states[i].history_count < MAX_HISTORY) {
                states[i].history[states[i].history_count++] =
                        states[i].current;
            }
            states[i].current = pos;
            updates++;
        } else if (bytes == 0) {
            /* child closed the pipe — flight ended normally */
            states[i].active = 0;
            close(states[i].pipe_read);
            close(states[i].pipe_step);
            printf("[MAIN] Flight %d finished\n", states[i].flight_id);
        } else {
            /* something went wrong reading from the pipe */
            perror("[MAIN] read position error");
            states[i].active = 0;
        }
    }

    return updates;
}

/* ─────────────────────────────────────────
 * Safety Violation Detection — US102
 *
 * Check every pair of active flights. If two
 * aircraft are closer than the safety threshold,
 * that's a violation.
 *
 * When a violation is found we:
 *   - Log it in the report, including positions
 *     and velocity vectors estimated from history
 *   - Send SIGUSR1 to both processes so they know
 *     they are in danger
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

                /* record the event if we still have space in the report */
                if (report->total_violations < MAX_VIOLATIONS) {
                    ViolationEvent *v =
                            &report->violations[report->total_violations];
                    v->timestamp   = timestamp;
                    v->flight_id_1 = states[i].flight_id;
                    v->flight_id_2 = states[j].flight_id;
                    v->position_1  = states[i].current;
                    v->position_2  = states[j].current;

                    /* estimate velocity as the difference between
                     * the current and previous positions */
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

                /* notify both processes with a signal */
                kill(states[i].pid, SIGUSR1);
                kill(states[j].pid, SIGUSR1);
            }
        }
    }

    return new_violations;
}

/* ─────────────────────────────────────────
 * Predicted Violation Detection — US101
 *
 * Instead of waiting for two aircraft to collide,
 * we try to predict where they will be next step
 * based on their current trajectory.
 *
 * The idea is simple: if a flight moved X metres
 * between the last step and this one, it will
 * probably move another X metres next step.
 * If that predicted position is within 2x the
 * safety threshold, we warn early.
 * ───────────────────────────────────────── */

int check_predicted_violations(FlightState *states,
                                int num_flights,
                                double threshold_m,
                                SimulationReport *report,
                                double timestamp) {
    int predictions = 0;

    for (int i = 0; i < num_flights; i++) {
        if (!states[i].active || states[i].history_count < 1) continue;

        /* project flight i one step ahead */
        Position *cur_i  = &states[i].current;
        Position *prev_i = &states[i].history[states[i].history_count - 1];

        Position next_i;
        next_i.latitude  = cur_i->latitude  + (cur_i->latitude  - prev_i->latitude);
        next_i.longitude = cur_i->longitude + (cur_i->longitude - prev_i->longitude);
        next_i.altitude  = cur_i->altitude  + (cur_i->altitude  - prev_i->altitude);

        for (int j = i + 1; j < num_flights; j++) {
            if (!states[j].active || states[j].history_count < 1) continue;

            /* project flight j one step ahead */
            Position *cur_j  = &states[j].current;
            Position *prev_j = &states[j].history[states[j].history_count - 1];

            Position next_j;
            next_j.latitude  = cur_j->latitude  + (cur_j->latitude  - prev_j->latitude);
            next_j.longitude = cur_j->longitude + (cur_j->longitude - prev_j->longitude);
            next_j.altitude  = cur_j->altitude  + (cur_j->altitude  - prev_j->altitude);

            double predicted_dist = compute_distance(&next_i, &next_j);

            /* warn if the predicted gap is tighter than 2x the safety threshold */
            if (predicted_dist < threshold_m * 2.0) {
                printf("[MAIN] PREDICTED VIOLATION: flights %d and %d"
                       " — predicted distance=%.1f m (2x threshold=%.1f m)\n",
                       states[i].flight_id, states[j].flight_id,
                       predicted_dist, threshold_m * 2.0);
                predictions++;
            }
        }
    }

    return predictions;
}

/* ─────────────────────────────────────────
 * Main Simulation Loop — US100
 *
 * This is where everything comes together:
 *   1. Create two pipes per flight (one for positions,
 *      one for step tokens) and fork a child process
 *      for each flight.
 *   2. Step through the simulation: synchronise all
 *      flights, check for current violations, then
 *      predict future ones.
 *   3. If violations exceed the limit, kill all
 *      flights and stop early.
 *   4. Wait for all children to exit, then write
 *      the pass/fail result to the report.
 * ───────────────────────────────────────── */

int start_simulation(const SimulationConfig *config,
                     FlightState *states,
                     SimulationReport *report) {

    /* zero out report and states before we start */
    memset(report, 0, sizeof(SimulationReport));
    report->total_flights = config->num_flights;
    memset(states, 0, sizeof(FlightState) * config->num_flights);

    /* spawn one child process per flight */
    for (int i = 0; i < config->num_flights; i++) {
        int pos_pipe[2];
        int step_pipe[2];

        if (pipe(pos_pipe) < 0) {
            perror("[MAIN] pipe (pos) error");
            return -1;
        }
        if (pipe(step_pipe) < 0) {
            perror("[MAIN] pipe (step) error");
            return -1;
        }

        pid_t pid = fork();
        if (pid < 0) {
            perror("[MAIN] fork error");
            return -1;
        }

        if (pid == 0) {
            /* child: close the ends it does not use and start flying */
            close(pos_pipe[0]);
            close(step_pipe[1]);
            execute_flight(i,
                           pos_pipe[1],   /* writes positions to parent */
                           step_pipe[0],  /* reads step tokens from parent */
                           config->total_steps,
                           config->time_step_seconds);
            /* execute_flight never returns — it calls exit() */
        }

        /* parent: close the child-side ends and keep track of the pipes */
        close(pos_pipe[1]);
        close(step_pipe[0]);
        states[i].flight_id     = i;
        states[i].pid           = pid;
        states[i].pipe_read     = pos_pipe[0];
        states[i].pipe_step     = step_pipe[1];
        states[i].active        = 1;
        states[i].history_count = 0;
        states[i].violated      = 0;

        printf("[MAIN] Forked flight %d (PID %d)\n", i, pid);
    }

    /* advance the simulation one step at a time */
    for (int step = 0; step < config->total_steps; step++) {
        printf("[MAIN] --- Time step %d ---\n", step);

        /* tick all flights and collect their new positions */
        int updates = synchronize_step(states, config->num_flights, step);
        printf("[MAIN] Step %d: received %d position update(s)\n",
               step, updates);
        (void) updates;

        /* check if any pair of flights is too close right now */
        double timestamp = step * config->time_step_seconds;
        int violations = check_safety_violations(
                states, config->num_flights,
                config->safety_threshold_m,
                report, timestamp);

        /* also look ahead using history to catch problems early */
        check_predicted_violations(states, config->num_flights,
                                    config->safety_threshold_m,
                                    report,
                                    timestamp);

        /* too many violations — shut everything down now */
        if (report->total_violations >= config->max_violations) {
            printf("[MAIN] Max violations reached — aborting.\n");
            for (int i = 0; i < config->num_flights; i++) {
                if (states[i].active) {
                    close(states[i].pipe_step); /* unblock the child first */
                    kill(states[i].pid, SIGTERM);
                    report->flight_statuses[i] = 2;
                    states[i].active = 0;
                }
            }
            break;
        }

        (void) violations;
    }

    /* close remaining step pipes so children can exit on their own */
    for (int i = 0; i < config->num_flights; i++) {
        if (states[i].active)
            close(states[i].pipe_step);
    }

    /* wait for every child to finish before we write the report */
    for (int i = 0; i < config->num_flights; i++) {
        int status;
        waitpid(states[i].pid, &status, 0);
        printf("[MAIN] Flight %d exited\n", states[i].flight_id);
    }

    /* pass if we stayed below the violation limit, fail otherwise */
    report->pass = (report->total_violations < config->max_violations)
                   ? 1 : 0;

    return 0;
}