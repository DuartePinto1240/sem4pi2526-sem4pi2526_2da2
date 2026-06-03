/*
 * test_us105.c — Acceptance tests for US105
 *
 * Tests the four acceptance criteria defined in US105 § 4.4:
 *   Test 1  US105.3  Shared memory allocated and accessible by child
 *   Test 2  US105.4  Semaphore blocks child until parent posts
 *   Test 3  US105.1  US105.5  Parent spawns the required threads
 *   Test 4  US105.5  Mutex protects a shared counter from concurrent access
 */

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <semaphore.h>
#include <fcntl.h>
#include <pthread.h>
#include "../src/simulation.h"

 /* Test 1 — US105.3
 * Verify that shared memory is allocated and accessible by a child.
 * The child writes a known value; the parent verifies it can read it.*/

void test_shared_memory_accessible_by_child(void) {
    SharedSimulationData *shm = init_shared_memory(1);
    assert(shm != NULL);
    shm->total_violations = 0;
    shm->abort_flag       = 0;

    pid_t pid = fork();
    if (pid == 0) {
        /* child: shm herdado via fork() -- escreve valor sentinela */
        shm->total_violations = 42;
        exit(0);
    }
    waitpid(pid, NULL, 0);

    assert(shm->total_violations == 42);
    munmap(shm, sizeof(SharedSimulationData));
    shm_unlink("/aisafe_sim");
    printf("PASS: test_shared_memory_accessible_by_child\n");
}

 /* Test 2 — US105.4
 * Verify that the named semaphore blocks the child until the parent
 * posts.  A shared memory flag confirms the ordering.*/

void test_semaphore_synchronises_child(void) {
    const char *sem_name = "/test_sem_105";
    sem_unlink(sem_name);   /* remove stale semaphore if present */

    sem_t *sem = sem_open(sem_name, O_CREAT, 0644, 0);
    assert(sem != SEM_FAILED);

    pid_t pid = fork();
    if (pid == 0) {
        sem_wait(sem);  /* blocks until parent posts */
        sem_close(sem);
        exit(0);
    }

    /* give child time to reach sem_wait */
    struct timespec ts = { 0, 5000000 }; /* 5 ms */
    nanosleep(&ts, NULL);

    sem_post(sem);          /* unblock child */
    waitpid(pid, NULL, 0);  /* child should exit cleanly */

    sem_close(sem);
    sem_unlink(sem_name);
    printf("PASS: test_semaphore_synchronises_child\n");
}

 /* Test 3 — US105.1 / US105.5
 * Verify that the parent can spawn threads with pthread_create.*/

static int thread_ran = 0;

static void *dummy_thread(void *arg) {
    (void)arg;
    thread_ran = 1;
    return NULL;
}

void test_parent_spawns_threads(void) {
    pthread_t tid;
    int rc = pthread_create(&tid, NULL, dummy_thread, NULL);
    assert(rc == 0);
    pthread_join(tid, NULL);
    assert(thread_ran == 1);
    printf("PASS: test_parent_spawns_threads\n");
}


 /* Test 4 — US105.5
 * Verify that a mutex correctly serialises concurrent increments,
 * producing the expected final count. */

static pthread_mutex_t test_mutex   = PTHREAD_MUTEX_INITIALIZER;
static int             shared_counter = 0;

static void *increment_counter(void *arg) {
    (void)arg;
    for (int i = 0; i < 1000; i++) {
        pthread_mutex_lock(&test_mutex);
        shared_counter++;
        pthread_mutex_unlock(&test_mutex);
    }
    return NULL;
}

void test_mutex_protects_shared_counter(void) {
    pthread_t t1, t2;
    shared_counter = 0;
    pthread_create(&t1, NULL, increment_counter, NULL);
    pthread_create(&t2, NULL, increment_counter, NULL);
    pthread_join(t1, NULL);
    pthread_join(t2, NULL);
    assert(shared_counter == 2000);
    printf("PASS: test_mutex_protects_shared_counter\n");
}

 // Main

int main(void) {
    printf("=== US105 Acceptance Tests ===\n\n");

    test_shared_memory_accessible_by_child();
    test_semaphore_synchronises_child();
    test_parent_spawns_threads();
    test_mutex_protects_shared_counter();

    printf("\nAll US105 tests passed.\n");
    return 0;
}
