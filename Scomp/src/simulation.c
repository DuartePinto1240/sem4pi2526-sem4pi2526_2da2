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
#include <sys/mman.h>
#include <semaphore.h>
#include <pthread.h>
#include <fcntl.h>
#include <time.h>
#include <errno.h>

/* -----------------------------------------
 * Mutexes and condition variables (US106)
 * ----------------------------------------- */

static pthread_mutex_t g_shm_mutex    = PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t g_report_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  g_report_cond  = PTHREAD_COND_INITIALIZER;
static pthread_mutex_t g_done_mutex   = PTHREAD_MUTEX_INITIALIZER;
static volatile int    g_sim_done     = 0;

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
 * ----------------------------------------- */

SharedSimulationData *init_shared_memory(int num_flights) {
    shm_unlink("/aisafe_sim"); /* remove stale segment if present */

    int fd = shm_open("/aisafe_sim", O_CREAT | O_EXCL | O_RDWR, 0644);
    if (fd == -1) {
        perror("[MAIN] shm_open");
        return NULL;
    }

    if (ftruncate(fd, sizeof(SharedSimulationData)) == -1) {
        perror("[MAIN] ftruncate");
        close(fd);
        shm_unlink("/aisafe_sim");
        return NULL;
    }

    SharedSimulationData *shm = mmap(NULL, sizeof(SharedSimulationData),
                                      PROT_READ | PROT_WRITE,
                                      MAP_SHARED, fd, 0);
    close(fd); /* fd já não é necessário após mmap */

    if (shm == MAP_FAILED) {
        perror("[MAIN] mmap");
        shm_unlink("/aisafe_sim");
        return NULL;
    }

    memset(shm, 0, sizeof(SharedSimulationData));
        for (int i = 0; i < num_flights; i++)
            shm->active[i] = 1;
        shm->num_active_flights = num_flights;
        shm->nproc_at_barrier   = 0;

    printf("[MAIN] Shared memory allocated (/aisafe_sim, size=%zu bytes)\n",
           sizeof(SharedSimulationData));
    return shm;
}

/* -----------------------------------------
 * US105 -- Per-Flight Semaphore Initialisation
 * ----------------------------------------- */

int init_semaphores(FlightState *states, int num_flights) {
    for (int i = 0; i < num_flights; i++) {
        snprintf(states[i].sem_name, sizeof(states[i].sem_name),
                 "/flight_%d", i);

        sem_unlink(states[i].sem_name);

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

    /* US108 -- barrier semaphores */
    sem_unlink(SEM_NPROC_NAME);
    sem_unlink(SEM_BARRIER1_NAME);
    sem_unlink(SEM_BARRIER2_NAME);

    sem_t *sem_nproc = sem_open(SEM_NPROC_NAME, O_CREAT | O_EXCL, 0644, 1);
    if (sem_nproc == SEM_FAILED) {
        perror("[MAIN] sem_open nproc");
        return -1;
    }
    sem_close(sem_nproc);
    printf("[MAIN] Semaphore created: %s\n", SEM_NPROC_NAME);

    sem_t *sem_b1 = sem_open(SEM_BARRIER1_NAME, O_CREAT | O_EXCL, 0644, 0);
    if (sem_b1 == SEM_FAILED) {
        perror("[MAIN] sem_open barrier1");
        sem_unlink(SEM_NPROC_NAME);
        return -1;
    }
    sem_close(sem_b1);
    printf("[MAIN] Semaphore created: %s\n", SEM_BARRIER1_NAME);

    sem_t *sem_b2 = sem_open(SEM_BARRIER2_NAME, O_CREAT | O_EXCL, 0644, 0);
    if (sem_b2 == SEM_FAILED) {
        perror("[MAIN] sem_open barrier2");
        sem_unlink(SEM_NPROC_NAME);
        sem_unlink(SEM_BARRIER1_NAME);
        return -1;
    }
    sem_close(sem_b2);
    printf("[MAIN] Semaphore created: %s\n", SEM_BARRIER2_NAME);

    return 0;
}

/* -----------------------------------------
 * Thread Args struct
 * ----------------------------------------- */

typedef struct {
    const SimulationConfig *config;
    FlightState            *states;
    SimulationReport       *report;
    SharedSimulationData   *shm;
} ThreadArgs;

/* -----------------------------------------
 * US106.1 -- Violation Detector Thread
 *
 * Scans shm->positions[] for pairs of flights
 * closer than the safety threshold.
 * On detection: increments counters, sends
 * SIGUSR1 to both processes, signals the
 * report thread via pthread_cond_signal (US107).
 * ----------------------------------------- */

static void *violation_detector_thread(void *arg) {
    ThreadArgs *args = (ThreadArgs *)arg;
    printf("[THREAD] ViolationDetector started (tid=%lu)\n",
           (unsigned long)pthread_self());

    while (!g_sim_done) {
        pthread_mutex_lock(&g_shm_mutex);

        for (int i = 0; i < args->config->num_flights; i++) {
            if (!args->shm->active[i]) continue;
            if (args->shm->steps_completed[i] == 0) continue;
            for (int j = i + 1; j < args->config->num_flights; j++) {
                if (!args->shm->active[j]) continue;

                double dist = compute_distance(
                        &args->shm->positions[i],
                        &args->shm->positions[j]);

                if (dist < args->config->safety_threshold_m) {
                    args->shm->total_violations++;
                    args->report->total_violations++;
                    args->report->flight_statuses[i] = 1;
                    args->report->flight_statuses[j] = 1;

                    if (args->report->total_violations <= MAX_VIOLATIONS) {
                        ViolationEvent *v =
                            &args->report->violations[
                                args->report->total_violations - 1];
                        v->timestamp   = (double)args->shm->steps_completed[i];
                        v->flight_id_1 = i;
                        v->flight_id_2 = j;
                        v->position_1  = args->shm->positions[i];
                        v->position_2  = args->shm->positions[j];
                    }

                    printf("[VD] Violation: flight %d and flight %d"
                           " -- dist=%.1f m\n", i, j, dist);

                    kill(args->states[i].pid, SIGUSR1);
                    kill(args->states[j].pid, SIGUSR1);

                    /* US107 -- signal report generator */
                    pthread_mutex_lock(&g_report_mutex);
                    pthread_cond_signal(&g_report_cond);
                    pthread_mutex_unlock(&g_report_mutex);

                    if (args->shm->total_violations >=
                            args->config->max_violations) {
                        args->shm->abort_flag = 1;
                        printf("[VD] Max violations reached"
                               " -- setting abort flag\n");
                        for (int k = 0; k < args->config->num_flights; k++) {
                            if (args->shm->active[k])
                                kill(args->states[k].pid, SIGTERM);
                        }
                    }
                }
            }
        }

        pthread_mutex_unlock(&g_shm_mutex);
        usleep(10000); /* 10 ms */
    }

    printf("[THREAD] ViolationDetector exiting\n");
    return NULL;
}

/* -----------------------------------------
 * US106.2 -- Report Generator Thread
 *
 * Waits on g_report_cond (US107 condition variable).
 * Logs each violation event as it arrives.
 * When g_sim_done is set, writes the final
 * report file (US109).
 * ----------------------------------------- */

static void *report_generator_thread(void *arg) {
    ThreadArgs *args = (ThreadArgs *)arg;
    printf("[THREAD] ReportGenerator started (tid=%lu)\n",
           (unsigned long)pthread_self());

    pthread_mutex_lock(&g_report_mutex);

    while (!g_sim_done) {
        /* US107 -- block until violation detected */
        pthread_cond_wait(&g_report_cond, &g_report_mutex);

        if (args->report->total_violations > 0) {
            printf("[RG] Violation event received"
                   " -- total so far: %d\n",
                   args->report->total_violations);
        }
    }

    pthread_mutex_unlock(&g_report_mutex);

    /* US109 -- write final report */
    printf("[RG] Simulation done -- writing final report\n");
    write_report(args->report, NULL);

    printf("[THREAD] ReportGenerator exiting\n");
    return NULL;
}

/* -----------------------------------------
 * US106.3 -- Simulation Controller Thread
 *
 * Posts step semaphores to unblock each flight
 * process, then polls shared memory until all
 * active flights complete the step.
 * Sets g_sim_done and broadcasts when done.
 * ----------------------------------------- */

static void *simulation_controller_thread(void *arg) {
    ThreadArgs *args = (ThreadArgs *)arg;
    printf("[THREAD] SimulationController started (tid=%lu)\n",
           (unsigned long)pthread_self());

    // US108 -- O SC não participa no barrier como processo igual.
    // Apenas posta os semáforos individuais e faz polling em steps_completed.

    for (int step = 0; step < args->config->total_steps; step++) {

        if (args->shm->abort_flag) {
            printf("[SC] Abort flag at step %d -- stopping\n", step);
            break;
        }

        printf("[SC] Posting step %d semaphores\n", step);

        for (int i = 0; i < args->config->num_flights; i++) {
            if (!args->states[i].active) continue;
            sem_t *sem = sem_open(args->states[i].sem_name, 0);
            if (sem != SEM_FAILED) {
                sem_post(sem);
                sem_close(sem);
            }
        }

        /* Espera que todos os voos activos completem este step via polling */
        for (int i = 0; i < args->config->num_flights; i++) {
            if (!args->states[i].active) continue;
            while (args->shm->steps_completed[i] <= step
                   && args->shm->active[i]
                   && !args->shm->abort_flag) {
                struct timespec ts = {0, 1000000}; /* 1 ms */
                nanosleep(&ts, NULL);
            }
            if (!args->shm->active[i]) {
                args->states[i].active = 0;
                printf("[SC] Flight %d inactive at step %d\n", i, step);
            } else {
                args->states[i].current = args->shm->positions[i];
            }
        }

        if (args->config->time_step_seconds > 0)
            sleep(args->config->time_step_seconds);
    }

    /* signal simulation done */
    pthread_mutex_lock(&g_done_mutex);
    g_sim_done = 1;
    pthread_mutex_unlock(&g_done_mutex);

    pthread_mutex_lock(&g_report_mutex);
    pthread_cond_broadcast(&g_report_cond);
    pthread_mutex_unlock(&g_report_mutex);

    printf("[THREAD] SimulationController exiting\n");
    return NULL;

}

/* -----------------------------------------
 * US105 -- Spawn Parent Threads
 * ----------------------------------------- */

int spawn_parent_threads(pthread_t *out_tids,
                         const SimulationConfig *config,
                         FlightState *states,
                         SimulationReport *report,
                         SharedSimulationData *shm) {
    /* reset done flag for this run */
    g_sim_done = 0;

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
 * ----------------------------------------- */

void cleanup_simulation(SharedSimulationData *shm,
                        FlightState *states,
                        int num_flights,
                        pthread_t *tids) {
    for (int i = 0; i < 3; i++)
        pthread_join(tids[i], NULL);

    if (shm && shm != MAP_FAILED) {
        munmap(shm, sizeof(SharedSimulationData));
        shm_unlink("/aisafe_sim");
        printf("[MAIN] Shared memory removed (/aisafe_sim)\n");
    }

    for (int i = 0; i < num_flights; i++) {
            sem_unlink(states[i].sem_name);
            printf("[MAIN] Semaphore removed: %s\n", states[i].sem_name);
        }

        /* US108 -- barrier semaphores */
        sem_unlink(SEM_NPROC_NAME);
        sem_unlink(SEM_BARRIER1_NAME);
        sem_unlink(SEM_BARRIER2_NAME);
        printf("[MAIN] Barrier semaphores removed\n");
}

/* -----------------------------------------
 * Step Synchronisation (US105 / US108)
 * Used by the main loop fallback only.
 * In US106 the SimulationController thread
 * handles step coordination directly.
 * ----------------------------------------- */

int synchronize_step(FlightState *states,
                     int num_flights,
                     int step,
                     SharedSimulationData *shm) {
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

    int updates = 0;
    for (int i = 0; i < num_flights; i++) {
        if (!states[i].active) continue;

        while (shm->steps_completed[i] <= step && shm->active[i]) {
            struct timespec ts = { 0, 1000000 };
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
 * Safety Violation Detection (US102)
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
 * Predicted Violation Detection (US101)
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
 * Main Simulation Loop (US100 / US105 / US106)
 *
 * US106: simulation loop is handled by the
 * SimulationController thread. Main process
 * forks children and waits for them to exit.
 * ----------------------------------------- */

int start_simulation(const SimulationConfig *config,
                     FlightState *states,
                     SimulationReport *report) {

    memset(report, 0, sizeof(SimulationReport));
    report->total_flights = config->num_flights;
    memset(states, 0, sizeof(FlightState) * config->num_flights);

    signal(SIGUSR1, SIG_IGN);


    /* US105.3 -- allocate shared memory */
    SharedSimulationData *shm = init_shared_memory(config->num_flights);
        if (!shm) return -1;

        /* US105.4 -- create per-flight semaphores */
        if (init_semaphores(states, config->num_flights) != 0) {
            munmap(shm, sizeof(SharedSimulationData));
            shm_unlink("/aisafe_sim");
            return -1;
        }

        /* US105.2 -- fork one child per flight ANTES de criar as threads,
         * para que states[i].active já esteja a 1 quando o SC arrancar   */
        for (int i = 0; i < config->num_flights; i++) {
            fflush(stdout);
            fflush(stderr);

            pid_t pid = fork();
            if (pid < 0) {
                perror("[MAIN] fork error");
                munmap(shm, sizeof(SharedSimulationData));
                shm_unlink("/aisafe_sim");
                for (int j = 0; j < config->num_flights; j++)
                    sem_unlink(states[j].sem_name);
                return -1;
            }

            if (pid == 0) {
                execute_flight(i, shm,
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

        /* US105.1 / US105.5 -- spawn parent threads (US106) APÓS os forks */
        pthread_t parent_threads[3];
        if (spawn_parent_threads(parent_threads, config, states, report, shm) != 0) {
            munmap(shm, sizeof(SharedSimulationData));
            shm_unlink("/aisafe_sim");
            for (int i = 0; i < config->num_flights; i++)
                sem_unlink(states[i].sem_name);
            return -1;
        }

    /* US106: simulation loop runs in SimulationController thread.
     * Main process waits for all children to exit. */
    for (int i = 0; i < config->num_flights; i++) {
        int status;
        waitpid(states[i].pid, &status, 0);
        printf("[MAIN] Flight %d exited\n", states[i].flight_id);
    }

    /* ensure abort flag is set so threads exit cleanly */
    shm->abort_flag = 1;

    report->pass = (report->total_violations < config->max_violations) ? 1 : 0;

    cleanup_simulation(shm, states, config->num_flights, parent_threads);

    return 0;
}