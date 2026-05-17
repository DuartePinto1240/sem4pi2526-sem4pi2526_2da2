#include "flight.h"

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <math.h>

// Global state for signal handling

static volatile sig_atomic_t sigusr1_received = 0;
static volatile sig_atomic_t sigterm_received  = 0;

// Signal handlers

void handle_sigusr1(int sig) {
    (void) sig;
    sigusr1_received = 1;
    // block other signals while handling
    sigset_t mask;
    sigemptyset(&mask);
    sigaddset(&mask, SIGUSR1);
    sigprocmask(SIG_BLOCK, &mask, NULL);

    write(STDOUT_FILENO, "[FLIGHT] Safety violation detected!\n", 37);

    sigprocmask(SIG_UNBLOCK, &mask, NULL);
}

void handle_sigterm(int sig) {
    (void) sig;
    sigterm_received = 1;
    write(STDOUT_FILENO, "[FLIGHT] Termination signal received. Cleaning up.\n", 51);
}

// Position computation

Position compute_next_position(int flight_id, int step, int total_steps) {
    Position pos;

    /* Simplified flight paths — each flight has a
     * predefined start and end coordinate. */

    double start_lat, start_lon, start_alt;
    double end_lat,   end_lon,   end_alt;

    /* Offset each flight slightly so they don't overlap */
    double offset = flight_id * 0.5;

    start_lat = 41.2481 + offset;
    start_lon = -8.6814;
    start_alt = 1000.0;

    end_lat   = 38.7813 + offset;
    end_lon   = -9.1359;
    end_alt   = 10000.0;

    double t = (total_steps > 1) ? (double) step / (double)(total_steps - 1) : 0.0;

    pos.latitude  = start_lat + t * (end_lat  - start_lat);
    pos.longitude = start_lon + t * (end_lon  - start_lon);
    pos.altitude  = start_alt + t * (end_alt  - start_alt);
    pos.time_step = step;

    return pos;
}

// Flight process entry point — US103 synchronization

void execute_flight(int flight_id,
                    int pipe_write,
                    int pipe_step,
                    int total_steps,
                    int time_step_seconds) {

    // register signal handlers
    signal(SIGUSR1, handle_sigusr1);
    signal(SIGTERM, handle_sigterm);

    printf("[FLIGHT %d] Started (PID %d)\n", flight_id, getpid());

    for (int step = 0; step < total_steps; step++) {

        // check if we received a termination signal
        if (sigterm_received) {
            printf("[FLIGHT %d] Aborting at step %d\n", flight_id, step);
            break;
        }

        // notify of violation if received
        if (sigusr1_received) {
            printf("[FLIGHT %d] Violation notified at step %d\n",
                   flight_id, step);
            sigusr1_received = 0;
        }

        /* US103 — wait for the step token from the parent.
         * Blocks here until the parent signals it is ready
         * to receive this step's position update. */
        char token;
        ssize_t bytes = read(pipe_step, &token, 1);
        if (bytes <= 0) {
            /* pipe closed — parent aborted */
            printf("[FLIGHT %d] Step pipe closed — exiting\n", flight_id);
            break;
        }

        /* compute next position */
        Position pos = compute_next_position(flight_id, step, total_steps);

        // send position update to parent via pipe
        ssize_t written = write(pipe_write, &pos, sizeof(Position));
        if (written < 0) {
            perror("[FLIGHT] write error");
            break;
        }

        printf("[FLIGHT %d] Step %d — position update sent: lat=%.4f lon=%.4f alt=%.1f\n",
               flight_id, step, pos.latitude, pos.longitude, pos.altitude);

        // wait for next time step
        if (time_step_seconds > 0)
            sleep(time_step_seconds);
    }

    close(pipe_write);
    close(pipe_step);
    printf("[FLIGHT %d] Finished\n", flight_id);
    exit(0);
}