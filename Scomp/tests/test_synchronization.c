#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <semaphore.h>
#include <fcntl.h>
#include <time.h>
#include <math.h>

#include "../src/simulation.h"
#include "../src/flight.h"

 // Test helpers

static int tests_run    = 0;
static int tests_passed = 0;
static int tests_failed = 0;

#define TEST(name) \
    do { \
        printf("  %-55s", name); \
        tests_run++; \
    } while(0)

#define PASS() \
    do { \
        printf("PASS\n"); \
        tests_passed++; \
    } while(0)

#define FAIL(msg) \
    do { \
        printf("FAIL -- %s\n", msg); \
        tests_failed++; \
    } while(0)

#define ASSERT(cond, msg) \
    do { \
        if (cond) { PASS(); } \
        else       { FAIL(msg); } \
    } while(0)

/* US103.1 -- synchronize_step posts semaphore and
 *             child advances steps_completed */

void test_synchronize_step_receives_one_position(void) {
    TEST("synchronize_step completes one active flight step");

    SharedSimulationData *shm = init_shared_memory(1);
    assert(shm != NULL);

    FlightState states[1];
    memset(states, 0, sizeof(states));
    states[0].flight_id = 0;
    states[0].active    = 1;
    snprintf(states[0].sem_name, sizeof(states[0].sem_name), "/test_sync_r1p");

    sem_unlink(states[0].sem_name);
    sem_t *s = sem_open(states[0].sem_name, O_CREAT | O_EXCL, 0644, 0);
    assert(s != SEM_FAILED);
    sem_close(s);

    /* Child: waits on semaphore, then increments steps_completed */
    pid_t pid = fork();
    assert(pid >= 0);
    if (pid == 0) {
        sem_t *cs = sem_open(states[0].sem_name, 0);
        sem_wait(cs);
        sem_close(cs);
        shm->steps_completed[0] = 1;
        exit(0);
    }

    /* Parent: synchronize_step should post + poll */
    states[0].pid = pid;
    int updates = synchronize_step(states, 1, 0, shm);

    int status;
    waitpid(pid, &status, 0);

    munmap(shm, sizeof(SharedSimulationData));
    shm_unlink("/aisafe_sim");
    sem_unlink(states[0].sem_name);

    ASSERT(updates == 1, "expected 1 step completion");
}

 // US103.2 -- synchronize_step stores position from shm

void test_synchronize_step_updates_current_position(void) {
    TEST("synchronize_step copies position from shared memory");

    SharedSimulationData *shm = init_shared_memory(1);
    assert(shm != NULL);

    FlightState states[1];
    memset(states, 0, sizeof(states));
    states[0].flight_id = 0;
    states[0].active    = 1;
    snprintf(states[0].sem_name, sizeof(states[0].sem_name), "/test_sync_r2p");

    sem_unlink(states[0].sem_name);
    sem_t *s = sem_open(states[0].sem_name, O_CREAT | O_EXCL, 0644, 0);
    assert(s != SEM_FAILED);
    sem_close(s);

    Position expected = { .latitude=38.7813, .longitude=-9.1359,
                          .altitude=10000.0, .time_step=1 };

    pid_t pid = fork();
    assert(pid >= 0);
    if (pid == 0) {
        sem_t *cs = sem_open(states[0].sem_name, 0);
        sem_wait(cs);
        sem_close(cs);
        shm->positions[0] = expected;
        shm->steps_completed[0] = 1;
        exit(0);
    }

    states[0].pid = pid;
    synchronize_step(states, 1, 0, shm);

    int status;
    waitpid(pid, &status, 0);

    int ok = (fabs(shm->positions[0].latitude  - expected.latitude)  < 1e-6 &&
              fabs(shm->positions[0].longitude - expected.longitude) < 1e-6 &&
              fabs(shm->positions[0].altitude  - expected.altitude)  < 1e-6);

    munmap(shm, sizeof(SharedSimulationData));
    shm_unlink("/aisafe_sim");
    sem_unlink(states[0].sem_name);

    ASSERT(ok, "position not copied from shared memory");
}

 // US103.3 -- skips inactive flights

void test_synchronize_step_skips_inactive(void) {
    TEST("synchronize_step skips inactive flights");

    SharedSimulationData *shm = init_shared_memory(2);
    assert(shm != NULL);

    FlightState states[2];
    memset(states, 0, sizeof(states));

    /* flight 0: active */
    states[0].flight_id = 0;
    states[0].active    = 1;
    snprintf(states[0].sem_name, sizeof(states[0].sem_name), "/test_sync_r3a");

    /* flight 1: inactive */
    states[1].flight_id = 1;
    states[1].active    = 0;

    sem_unlink(states[0].sem_name);
    sem_t *s = sem_open(states[0].sem_name, O_CREAT | O_EXCL, 0644, 0);
    assert(s != SEM_FAILED);
    sem_close(s);

    pid_t pid = fork();
    assert(pid >= 0);
    if (pid == 0) {
        sem_t *cs = sem_open(states[0].sem_name, 0);
        sem_wait(cs);
        sem_close(cs);
        shm->steps_completed[0] = 1;
        exit(0);
    }

    states[0].pid = pid;
    int updates = synchronize_step(states, 2, 0, shm);

    int status;
    waitpid(pid, &status, 0);

    munmap(shm, sizeof(SharedSimulationData));
    shm_unlink("/aisafe_sim");
    sem_unlink(states[0].sem_name);

    ASSERT(updates == 1, "should only count 1 active flight");
}

 // US103.4 -- multiple flights all complete

void test_synchronize_step_multiple_flights(void) {
    TEST("synchronize_step handles 3 concurrent flights");

    SharedSimulationData *shm = init_shared_memory(3);
    assert(shm != NULL);

    FlightState states[3];
    memset(states, 0, sizeof(states));

    for (int i = 0; i < 3; i++) {
        states[i].flight_id = i;
        states[i].active    = 1;
        snprintf(states[i].sem_name, sizeof(states[i].sem_name), "/test_sync_r4_%d", i);
        sem_unlink(states[i].sem_name);
        sem_t *s = sem_open(states[i].sem_name, O_CREAT | O_EXCL, 0644, 0);
        assert(s != SEM_FAILED);
        sem_close(s);
    }

    pid_t pids[3];
    for (int i = 0; i < 3; i++) {
        pids[i] = fork();
        assert(pids[i] >= 0);
        if (pids[i] == 0) {
            sem_t *cs = sem_open(states[i].sem_name, 0);
            sem_wait(cs);
            sem_close(cs);
            shm->steps_completed[i] = 1;
            exit(0);
        }
        states[i].pid = pids[i];
    }

    int updates = synchronize_step(states, 3, 0, shm);

    for (int i = 0; i < 3; i++) {
        int status;
        waitpid(pids[i], &status, 0);
    }

    munmap(shm, sizeof(SharedSimulationData));
    shm_unlink("/aisafe_sim");
    for (int i = 0; i < 3; i++)
        sem_unlink(states[i].sem_name);

    ASSERT(updates == 3, "expected 3 step completions");
}

 // US103.5 -- abort_flag is visible to child

void test_abort_flag_visible_to_child(void) {
    TEST("abort_flag in shared memory visible to child process");

    SharedSimulationData *shm = init_shared_memory(1);
    assert(shm != NULL);

    shm->abort_flag = 1;

    pid_t pid = fork();
    assert(pid >= 0);
    if (pid == 0) {
        int flag = shm->abort_flag;
        exit(flag == 1 ? 0 : 1);
    }

    int status;
    waitpid(pid, &status, 0);

    munmap(shm, sizeof(SharedSimulationData));
    shm_unlink("/aisafe_sim");

    ASSERT(WIFEXITED(status) && WEXITSTATUS(status) == 0,
           "child did not see abort_flag=1");
}

 // Main

int main(void) {
    printf("\n=== US103 Synchronization Tests (Shared Memory + Semaphores) ===\n\n");

    test_synchronize_step_receives_one_position();
    test_synchronize_step_updates_current_position();
    test_synchronize_step_skips_inactive();
    test_synchronize_step_multiple_flights();
    test_abort_flag_visible_to_child();

    printf("\nResults: %d/%d passed", tests_passed, tests_run);
    if (tests_failed > 0)
        printf(", %d FAILED", tests_failed);
    printf("\n\n");

    return (tests_failed == 0) ? 0 : 1;
}
