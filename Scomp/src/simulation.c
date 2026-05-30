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
#include <sys/ipc.h>
#include <sys/shm.h>
#include <semaphore.h>
#include <pthread.h>
#include <fcntl.h>
#include <time.h>
#include <errno.h>

/* Mutex protecting shared counters total_violations and abort_flag */
static pthread_mutex_t g_shm_mutex = PTHREAD_MUTEX_INITIALIZER;

/* -----------------------------------------
 * Parameter Validation
 * ----------------------------------------- */

int validate_parameters(const SimulationConfig *config) {
    if (config == NULL) {
        fprintf(stderr, "[ERROR] Config is NULL\n");
        return -1;
    }
    if (config->num_flights <= 0 || config->num_flights > MAX_FLIGHTS) {
        fprintf(stderr, "[ERROR] num_flights out of range (got %d)\n",
                config->num_flights);
        return -1;
    }
    if (config->total_steps <= 0) {
        fprintf(stderr, "[ERROR] total_steps must be positive (got %d)\n",
                config->total_steps);
        return -1;
    }
    if (config->time_step_seconds <= 0) {
        fprintf(stderr, "[ERROR] time_step_seconds must be positive (got %d)\n",
                config->time_step_seconds);
        return -1;
    }
    if (config->safety_threshold_m <= 0.0) {
        fprintf(stderr, "[ERROR] safety_threshold_m must be positive\n");
        return -1;
    }
    if (config->max_violations <= 0) {
        fprintf(stderr, "[ERROR] max_violations must be positive\n");
        return -1;
    }
    return 0;
}

/* -----------------------------------------
 * Distance Between Two Aircraft
 * ----------------------------------------- */

double compute_distance(const Position *p1, const Position *p2) {
    double dlat = (p1->latitude  - p2->latitude)  * 111000.0;
    double dlon = (p1->longitude - p2->longitude)  * 111000.0;
    double dalt =  p1->altitude  - p2->altitude;
    return sqrt(dlat * dlat + dlon * dlon + dalt * dalt);
}

/* -----------------------------------------
 * US105 -- Shared Memory Initialisation
 *
 * Allocates one shared memory segment via shmget/shmat.
 * Zeroes all fields; sets active[i]=1 for the first
 * num_flights entries.  Stores shmid via out_shmid.
 * ----------------------------------------- */

SharedSimulationData *init_shared_memory(int num_flights, int *out_shmid) {
    int shmid = shmget(IPC_PRIVATE,
                       sizeof(SharedSimulationData),
                       IPC_CREAT | 0666);
    if (shmid == -1) {
        perror("[MAIN] shmget");
        return NULL;
    }

    SharedSimulationData *shm = shmat(shmid, NULL, 0);
    if (shm == (void *)-1) {
        perror("[MAIN] shmat");
        shmctl(shmid, IPC_RMID, NULL);
        return NULL;
    }

    memset(shm, 0, sizeof(SharedSimulationData));
    for (int i = 0; i < num_flights; i++)
        shm->active[i] = 1;

    *out_shmid = shmid;
    printf("[MAIN] Shared memory allocated (shmid=%d, size=%zu bytes)\n",
           shmid, sizeof(SharedSimulationData));
    return shm;
}

/* -----------------------------------------
 * US105 -- Per-Flight Semaphore Initialisation
 *
 * Creates one named semaphore per flight, initial value 0.
 * Children call sem_open + sem_wait and block until the parent
 * posts for each step.  Stale semaphores from previous runs
 * are unlinked first.
 * ----------------------------------------- */

int init_semaphores(FlightState *states, int num_flights) {
    for (int i = 0; i < num_flights; i++) {
        snprintf(states[i].sem_name, sizeof(states[i].sem_name),
                 "/flight_%d", i);

        sem_unlink(states[i].sem_name);  /* remove stale if present */

        sem_t *sem = sem_open(states[i].sem_name,
                              O_CREAT | O_EXCL, 0644, 0);
        if (sem == SEM_FAILED) {
            perror("[MAIN] sem_open");
            for (int j = 0; j < i; j++) {
                sem_t *prev = sem_open(states[j].sem_name, 0);
                if (prev != SEM_FAILED) sem_close(prev);
                sem_unlink(states[j].sem_name);
            }
            return -1;
        }
        sem_close(sem);
        printf("[MAIN] Semaphore created: %s\n", states[i].sem_name);
    }
    return 0;
}

/* -----------------------------------------
 * US105 -- Parent Thread Functions
 *
 * Three dedicated threads satisfy US105.1 and US105.5.
 * Full logic is defined in US106-US109; here we provide
 * the skeletons so pthread_create/join is exercised.
 * ----------------------------------------- */

typedef struct {
    const SimulationConfig *config;
    FlightState            *states;
    SimulationReport       *report;
    SharedSimulationData   *shm;
} ThreadArgs;

static void *simulation_controller_thread(void *arg) {
    (void)arg;
    printf("[THREAD] SimulationController started\n");
    return NULL;
}

static void *violation_detector_thread(void *arg) {
    (void)arg;
    printf("[THREAD] ViolationDetector started\n");
    return NULL;
}

static void *report_generator_thread(void *arg) {
    (void)arg;
    printf("[THREAD] ReportGenerator started\n");
    return NULL;
}

int spawn_parent_threads(pthread_t *out_tids,
                         const SimulationConfig *config,
                         FlightState *states,
                         SimulationReport *report,
                         SharedSimulationData *shm) {
    ThreadArgs *args = malloc(sizeof(ThreadArgs));
    if (!args) { perror("[MAIN] malloc ThreadArgs"); return -1; }
    args->config = config;
    args->states = states;
    args->report = report;
    args->shm    = shm;

    typedef void *(*ThreadFn)(void *);
    ThreadFn fns[3] = {
        simulation_controller_thread,
        violation_detector_thread,
        report_generator_thread
    };
    const char *names[3] = {
        "SimulationController",
        "ViolationDetector",
        "ReportGenerator"
    };

    for (int i = 0; i < 3; i++) {
        if (pthread_create(&out_tids[i], NULL, fns[i], args) != 0) {
            fprintf(stderr, "[MAIN] pthread_create %s failed\n", names[i]);
            for (int j = 0; j < i; j++)
                pthread_join(out_tids[j], NULL);
            free(args);
            return -1;
        }
        printf("[MAIN] Thread spawned: %s\n", names[i]);
    }
    return 0;
}

/* -----------------------------------------
 * US105 -- Cleanup
 *
 * Joins all parent threads, removes shared memory
 * (shmdt + shmctl IPC_RMID), and unlinks all semaphores.
 * ----------------------------------------- */

void cleanup_simulation(int shmid,
                        SharedSimulationData *shm,
                        FlightState *states,
                        int num_flights,
                        pthread_t *tids) {
    for (int i = 0; i < 3; i++)
        pthread_join(tids[i], NULL);

    if (shm && shm != (void *)-1) {
        shmdt(shm);
        shmctl(shmid, IPC_RMID, NULL);
        printf("[MAIN] Shared memory removed (shmid=%d)\n", shmid);
    }

    for (int i = 0; i < num_flights; i++) {
        sem_unlink(states[i].sem_name);
        printf("[MAIN] Semaphore removed: %s\n", states[i].sem_name);
    }
}

/* -----------------------------------------
 * Step Synchronisation  (US105 / US108)
 *
 * 1. Post the step semaphore of every active flight.
 * 2. Poll shm->steps_completed[i] until the child replies
 *    or shm->active[i] drops to 0 (flight ended).
 * 3. Copy the new position from shm into FlightState.
 * ----------------------------------------- */

int synchronize_step(FlightState *states,
                     int num_flights,
                     int step,
                     SharedSimulationData *shm) {
    /* unblock all active children */
    for (int i = 0; i < num_flights; i++) {
        if (!states[i].active) continue;
        sem_t *sem = sem_open(states[i].sem_name, 0);
        if (sem == SEM_FAILED) {
            perror("[MAIN] sem_open (post)");
            states[i].active = 0;
            continue;
        }
        sem_post(sem);
        sem_close(sem);
    }

    /* collect updated positions */
    int updates = 0;
    for (int i = 0; i < num_flights; i++) {
        if (!states[i].active) continue;

        while (shm->steps_completed[i] <= step && shm->active[i]) {
            struct timespec ts = { 0, 1000000 }; /* 1 ms */
            nanosleep(&ts, NULL);
        }

        if (!shm->active[i] && shm->steps_completed[i] <= step) {
            states[i].active = 0;
            printf("[MAIN] Flight %d finished\n", states[i].flight_id);
            continue;
        }

        if (states[i].history_count < MAX_HISTORY)
            states[i].history[states[i].history_count++] = states[i].current;
        states[i].current = shm->positions[i];
        updates++;
    }

    return updates;
}

/* -----------------------------------------
 * Safety Violation Detection -- US102
 * ----------------------------------------- */

int check_safety_violations(FlightState *states,
                             int num_flights,
                             double threshold_m,
                             SimulationReport *report,
                             double timestamp,
                             SharedSimulationData *shm) {
    int new_violations = 0;

    for (int i = 0; i < num_flights; i++) {
        if (!states[i].active) continue;
        for (int j = i + 1; j < num_flights; j++) {
            if (!states[j].active) continue;

            double dist = compute_distance(
                    &states[i].current, &states[j].current);

            if (dist < threshold_m) {
                printf("[MAIN] VIOLATION: flight %d and flight %d"
                       " -- distance=%.1f m (threshold=%.1f m)\n",
                       states[i].flight_id, states[j].flight_id,
                       dist, threshold_m);

                pthread_mutex_lock(&g_shm_mutex);

                if (report->total_violations < MAX_VIOLATIONS) {
                    ViolationEvent *v =
                            &report->violations[report->total_violations];
                    v->timestamp   = timestamp;
                    v->flight_id_1 = states[i].flight_id;
                    v->flight_id_2 = states[j].flight_id;
                    v->position_1  = states[i].current;
                    v->position_2  = states[j].current;

                    if (states[i].history_count > 0) {
                        Position *prev =
                            &states[i].history[states[i].history_count - 1];
                        v->velocity_1.vx =
                            states[i].current.latitude  - prev->latitude;
                        v->velocity_1.vy =
                            states[i].current.longitude - prev->longitude;
                        v->velocity_1.vz =
                            states[i].current.altitude  - prev->altitude;
                    }
                    if (states[j].history_count > 0) {
                        Position *prev =
                            &states[j].history[states[j].history_count - 1];
                        v->velocity_2.vx =
                            states[j].current.latitude  - prev->latitude;
                        v->velocity_2.vy =
                            states[j].current.longitude - prev->longitude;
                        v->velocity_2.vz =
                            states[j].current.altitude  - prev->altitude;
                    }
                }

                report->total_violations++;
                if (shm) shm->total_violations++;
                report->flight_statuses[i] = 1;
                report->flight_statuses[j] = 1;
                new_violations++;

                pthread_mutex_unlock(&g_shm_mutex);

                kill(states[i].pid, SIGUSR1);
                kill(states[j].pid, SIGUSR1);
            }
        }
    }

    return new_violations;
}

/* -----------------------------------------
 * Predicted Violation Detection -- US101
 * ----------------------------------------- */

int check_predicted_violations(FlightState *states,
                                int num_flights,
                                double threshold_m,
                                SimulationReport *report,
                                double timestamp) {
    (void)report;
    (void)timestamp;
    int predictions = 0;

    for (int i = 0; i < num_flights; i++) {
        if (!states[i].active || states[i].history_count < 1) continue;

        Position *cur_i  = &states[i].current;
        Position *prev_i = &states[i].history[states[i].history_count - 1];
        Position next_i;
        next_i.latitude  = cur_i->latitude  + (cur_i->latitude  - prev_i->latitude);
        next_i.longitude = cur_i->longitude + (cur_i->longitude - prev_i->longitude);
        next_i.altitude  = cur_i->altitude  + (cur_i->altitude  - prev_i->altitude);
        next_i.time_step = 0;

        for (int j = i + 1; j < num_flights; j++) {
            if (!states[j].active || states[j].history_count < 1) continue;

            Position *cur_j  = &states[j].current;
            Position *prev_j = &states[j].history[states[j].history_count - 1];
            Position next_j;
            next_j.latitude  = cur_j->latitude  + (cur_j->latitude  - prev_j->latitude);
            next_j.longitude = cur_j->longitude + (cur_j->longitude - prev_j->longitude);
            next_j.altitude  = cur_j->altitude  + (cur_j->altitude  - prev_j->altitude);
            next_j.time_step = 0;

            double predicted_dist = compute_distance(&next_i, &next_j);

            if (predicted_dist < threshold_m * 2.0) {
                printf("[MAIN] PREDICTED VIOLATION: flights %d and %d"
                       " -- predicted distance=%.1f m (2x threshold=%.1f m)\n",
                       states[i].flight_id, states[j].flight_id,
                       predicted_dist, threshold_m * 2.0);
                predictions++;
            }
        }
    }

    return predictions;
}

/* -----------------------------------------
 * Main Simulation Loop -- US100 / US105
 *
 * Sprint 3 changes vs Sprint 2:
 *   - Pipes replaced by shared memory + named semaphores.
 *   - Three parent threads spawned (US105.1).
 *   - execute_flight() receives shmid instead of pipe fds.
 *   - synchronize_step() posts semaphores and reads shm.
 *   - cleanup_simulation() removes shm and semaphores.
 * ----------------------------------------- */

int start_simulation(const SimulationConfig *config,
                     FlightState *states,
                     SimulationReport *report) {

    memset(report, 0, sizeof(SimulationReport));
    report->total_flights = config->num_flights;
    memset(states, 0, sizeof(FlightState) * config->num_flights);

    /* US105.3: allocate shared memory */
    int shmid;
    SharedSimulationData *shm = init_shared_memory(config->num_flights, &shmid);
    if (!shm) return -1;

    /* US105.4: create per-flight semaphores */
    if (init_semaphores(states, config->num_flights) != 0) {
        shmdt(shm);
        shmctl(shmid, IPC_RMID, NULL);
        return -1;
    }

    /* US105.1 / US105.5: spawn parent threads */
    pthread_t parent_threads[3];
    if (spawn_parent_threads(parent_threads, config, states, report, shm) != 0) {
        shmdt(shm);
        shmctl(shmid, IPC_RMID, NULL);
        for (int i = 0; i < config->num_flights; i++)
            sem_unlink(states[i].sem_name);
        return -1;
    }

    /* US105.2: fork one child per flight */
    for (int i = 0; i < config->num_flights; i++) {
        fflush(stdout);
        fflush(stderr);

        pid_t pid = fork();
        if (pid < 0) {
            perror("[MAIN] fork error");
            cleanup_simulation(shmid, shm, states, config->num_flights,
                               parent_threads);
            return -1;
        }

        if (pid == 0) {
            shmdt(shm);  /* child detaches and re-attaches its own */
            execute_flight(i, shmid,
                           config->total_steps,
                           config->time_step_seconds);
            /* never returns */
        }

        states[i].flight_id     = i;
        states[i].pid           = pid;
        states[i].active        = 1;
        states[i].history_count = 0;
        states[i].violated      = 0;

        printf("[MAIN] Forked flight %d (PID %d) sem=%s\n",
               i, pid, states[i].sem_name);
    }

    /* simulation loop */
    for (int step = 0; step < config->total_steps; step++) {
        printf("[MAIN] --- Time step %d ---\n", step);

        int updates = synchronize_step(states, config->num_flights, step, shm);
        printf("[MAIN] Step %d: received %d position update(s)\n",
               step, updates);
        (void)updates;

        double timestamp = (double)step * config->time_step_seconds;

        int violations = check_safety_violations(
                states, config->num_flights,
                config->safety_threshold_m,
                report, timestamp, shm);

        check_predicted_violations(states, config->num_flights,
                                   config->safety_threshold_m,
                                   report, timestamp);

        pthread_mutex_lock(&g_shm_mutex);
        int do_abort = (report->total_violations >= config->max_violations);
        if (do_abort) shm->abort_flag = 1;
        pthread_mutex_unlock(&g_shm_mutex);

        if (do_abort) {
            printf("[MAIN] Max violations reached -- aborting.\n");
            for (int i = 0; i < config->num_flights; i++) {
                if (states[i].active) {
                    sem_t *sem = sem_open(states[i].sem_name, 0);
                    if (sem != SEM_FAILED) {
                        sem_post(sem);
                        sem_close(sem);
                    }
                    kill(states[i].pid, SIGTERM);
                    report->flight_statuses[i] = 2;
                    states[i].active = 0;
                }
            }
            break;
        }

        (void)violations;
    }

    /* signal still-active children that there are no more steps */
    shm->abort_flag = 1;
    for (int i = 0; i < config->num_flights; i++) {
        if (states[i].active) {
            sem_t *sem = sem_open(states[i].sem_name, 0);
            if (sem != SEM_FAILED) {
                sem_post(sem);
                sem_close(sem);
            }
        }
    }

    for (int i = 0; i < config->num_flights; i++) {
        int status;
        waitpid(states[i].pid, &status, 0);
        printf("[MAIN] Flight %d exited\n", states[i].flight_id);
    }

    report->pass = (report->total_violations < config->max_violations) ? 1 : 0;

    cleanup_simulation(shmid, shm, states, config->num_flights, parent_threads);

    return 0;
}
