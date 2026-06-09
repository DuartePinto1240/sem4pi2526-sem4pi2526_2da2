#ifndef SIMULATION_H
#define SIMULATION_H

#include <sys/types.h>
#include <sys/mman.h>
#include <semaphore.h>
#include <pthread.h>
#include <signal.h>


 // Constants

#define MAX_FLIGHTS    20
#define MAX_HISTORY    100
#define MAX_VIOLATIONS 50
#define SEM_NPROC_NAME "/aisafe_nproc"
#define SEM_BARRIER1_NAME "/aisafe_barrier1"
#define SEM_BARRIER2_NAME "/aisafe_barrier2"

 // Data Structures

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

/* -----------------------------------------
 * Wind data written by the environment thread (US110)
 * ----------------------------------------- */
typedef struct {
    double speed_ms;       /* wind speed in m/s              */
    double direction_deg;  /* wind direction in degrees       */
    double vx;             /* east component  (m/s)          */
    double vy;             /* north component (m/s)          */
} WindData;

/* -----------------------------------------
 * Shared Memory Layout  (US105)
 *
 * One shm_open() segment holds the state shared by the parent
 * process and all child flight processes.  Each child writes
 * only to its own slot (positions[i], steps_completed[i],
 * active[i]) so no mutex is needed for those writes.
 * total_violations and abort_flag are written only by the
 * parent and are protected by a mutex in simulation.c.
 * wind is written only by the environment thread (US110)
 * and protected by the same mutex.
 * ----------------------------------------- */
typedef struct {
    Position positions[MAX_FLIGHTS];       /* current position per flight    */
    int      steps_completed[MAX_FLIGHTS]; /* step counter per flight        */
    int      active[MAX_FLIGHTS];          /* 1=running, 0=finished          */
    int      violations[MAX_FLIGHTS];      /* per-flight violation count      */
    int      total_violations;             /* global counter (mutex-protected)*/
    int      abort_flag;                   /* 1=simulation must stop          */
    int      nproc_at_barrier;             /* flights that finished this step */
    int      num_active_flights;           /* active flights (barrier target) */
    WindData wind;                         /* current wind (US110)            */
} SharedSimulationData;

/* State of one flight in the simulation */
typedef struct {
    int           flight_id;
    pid_t         pid;
    char          sem_name[32];     /* named semaphore, e.g. "/flight_0" */
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

/* -----------------------------------------
 * Function Declarations
 * ----------------------------------------- */

/* Validate simulation parameters.
 * Returns 0 if valid, -1 if invalid. */
int validate_parameters(const SimulationConfig *config);

/* -- US105: Initialisation -- */

/* Allocate and map a shared memory segment (shm_open/mmap).
 * Zeroes all fields; sets active[i]=1 for first num_flights entries.
 * Returns a pointer to the mapped segment, or NULL on error. */
SharedSimulationData *init_shared_memory(int num_flights);

/* Create one named semaphore per flight (e.g. "/flight_0").
 * Each semaphore is initialised to 0 so children block immediately
 * on sem_wait() until the parent posts.
 * Semaphore names are written into states[i].sem_name.
 * Returns 0 on success, -1 on error. */
int init_semaphores(FlightState *states, int num_flights);

/* Spawn the four parent-process threads:
 *   SimulationController, ViolationDetector, ReportGenerator, Environment.
 * Thread IDs are stored in out_tids[0..3].
 * Returns 0 on success, -1 on error. */
int spawn_parent_threads(pthread_t *out_tids,
                         const SimulationConfig *config,
                         FlightState *states,
                         SimulationReport *report,
                         SharedSimulationData *shm);

/* Unmap and unlink the shared memory segment (munmap + shm_unlink).
 * Unlinks all named semaphores stored in states[].sem_name.
 * Joins all parent threads listed in tids[0..3]. */
void cleanup_simulation(SharedSimulationData *shm,
                        FlightState *states,
                        int num_flights,
                        pthread_t *tids);

/* -- Simulation loop -- */

/* Start the simulation: sets up shared memory, semaphores and parent
 * threads, forks one child per flight, runs the simulation loop,
 * and cleans up on exit. */
int start_simulation(const SimulationConfig *config,
                     FlightState *states,
                     SimulationReport *report);

/* Post the step semaphore to every active flight, then poll
 * shm->steps_completed[i] until every active flight replies.
 * Returns the number of flights that completed the step. */
int synchronize_step(FlightState *states,
                     int num_flights,
                     int step,
                     SharedSimulationData *shm);

/* Check for safety violations among all current positions.
 * Returns the number of new violations detected. */
int check_safety_violations(FlightState *states,
                             int num_flights,
                             double threshold_m,
                             SimulationReport *report,
                             double timestamp,
                             SharedSimulationData *shm);

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

/* -- US110: Weather -- */

/* Load weather entries from a CSV file into weather.c internal dataset.
 * Returns 0 on success, -1 on error. */
int weather_load_csv(const char *csv_path);

/* Find the wind for a given position (lat, lon, altitude in feet).
 * Fills wind and returns 1 if a match was found, 0 if no zone matched. */
int weather_lookup(double lat, double lon, double alt_ft, WindData *wind);

#endif /* SIMULATION_H */