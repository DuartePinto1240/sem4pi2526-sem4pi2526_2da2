#ifndef FLIGHT_H
#define FLIGHT_H

#include "simulation.h"

/* ─────────────────────────────────────────
 * Function Declarations
 * ───────────────────────────────────────── */

/* Entry point for a child flight process.
 * Reads a step token from the parent before sending each
 * position update — synchronization barrier.
 * flight_id   — unique identifier of this flight
 * pipe_write  — write end of the position pipe to the parent
 * pipe_step   — read end of the step-token pipe from the parent
 * total_steps — number of simulation steps to execute
 * time_step   — duration of each step in seconds */
void execute_flight(int flight_id,
                    int pipe_write,
                    int pipe_step,
                    int total_steps,
                    int time_step_seconds);

/* Compute the next position of a flight based on simplified physics.
 * Uses linear interpolation between a start and end point. */
Position compute_next_position(int flight_id,
                               int step,
                               int total_steps);

/* Signal handler for SIGUSR1 — called when a safety violation
 * is detected by the parent process. */
void handle_sigusr1(int sig);

/* Signal handler for SIGTERM — called when the parent aborts
 * the simulation due to too many violations. */
void handle_sigterm(int sig);

#endif /* FLIGHT_H */