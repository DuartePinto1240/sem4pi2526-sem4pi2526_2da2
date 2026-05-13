#ifndef SIMULATION_H
#define SIMULATION_H

#include <sys/types.h>
#include <signal.h>

/* ─────────────────────────────────────────
 * Constants
 * ───────────────────────────────────────── */

#define MAX_FLIGHTS   20
#define MAX_HISTORY   100
#define MAX_VIOLATIONS 50

/* ─────────────────────────────────────────
 * Data Structures
 * ───────────────────────────────────────── */

/* Position of an aircraft at a given time step */
typedef struct {
    double latitude;
    double longitude;
    double altitude;
    int    time_step;
} Position;

/* Velocity vector of an aircraft */
typedef struct {
    double vx;
    double vy;
    double vz;
} VelocityVector;

/* State of one flight in the simulation */
typedef struct {
    int           flight_id;
    pid_t         pid;
    int           pipe_fd;          /* read end of pipe from child */
    Position      current;
    Position      history[MAX_HISTORY];
    int           history_count;
    int           active;           /* 1 = running, 0 = finished */
    int           violated;         /* 1 = received SIGUSR1 */
} FlightState;

/* A recorded safety violation event */
typedef struct {
    double        timestamp;
    int           flight_id_1;
    int           flight_id_2;
    Position      position_1;
    Position      position_2;
    VelocityVector velocity_1;
    VelocityVector velocity_2;
} ViolationEvent;

/* Simulation configuration */
typedef struct {
    int    num_flights;
    int    time_step_seconds;
    int    total_steps;
    double safety_threshold_m;
    int    max_violations;
} SimulationConfig;

/* Simulation report */
typedef struct {
    int            total_flights;
    int            total_violations;
    int            pass;            /* 1 = PASS, 0 = FAIL */
    ViolationEvent violations[MAX_VIOLATIONS];
    int            flight_statuses[MAX_FLIGHTS]; /* 0=ok, 1=violated, 2=aborted */
} SimulationReport;

/* ─────────────────────────────────────────
 * Function Declarations
 * ───────────────────────────────────────── */

/* Validate simulation parameters.
 * Returns 0 if valid, -1 if invalid. */
int validate_parameters(const SimulationConfig *config);

/* Start the simulation.
 * Forks one child process per flight,
 * tracks positions and detects violations. */
int start_simulation(const SimulationConfig *config,
                     FlightState *states,
                     SimulationReport *report);

/* Read position updates from all active flight pipes.
 * Returns the number of updates read. */
int read_all_positions(FlightState *states, int num_flights);

/* Check for safety violations among all current positions.
 * Returns the number of new violations detected. */
int check_safety_violations(FlightState *states,
                             int num_flights,
                             double threshold_m,
                             SimulationReport *report,
                             double timestamp);

/* Check for predicted violations using position history.
 * Returns number of predicted violations found. */
int check_predicted_violations(FlightState *states,
                                int num_flights,
                                double threshold_m,
                                SimulationReport *report,
                                double timestamp);

/* Compute Euclidean distance between two positions in metres. */
double compute_distance(const Position *p1, const Position *p2);

/* Write the simulation report to a file. */
void write_report(const SimulationReport *report,
                  const char *filename);

#endif /* SIMULATION_H */