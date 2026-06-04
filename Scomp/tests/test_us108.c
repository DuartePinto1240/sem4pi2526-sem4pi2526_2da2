#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <semaphore.h>
#include <fcntl.h>

#include "../src/simulation.h"

static int tests_passed = 0;
static int tests_failed = 0;

#define PASS(name) do { printf("PASS: %s\n", name); tests_passed++; } while(0)
#define FAIL(name, msg) do { printf("FAIL: %s -- %s\n", name, msg); tests_failed++; } while(0)

 /* Test 1 -- US108.1
 * Todos os voos chegam ao barrier antes de qualquer avançar.
 * O pai (SimulationController) só acorda depois de nproc == total.*/

void test_barrier_enforces_step_ordering(void) {
    const char *name = "barrier_enforces_step_ordering";

    SharedSimulationData *shm = init_shared_memory(2);
    assert(shm != NULL);

    sem_unlink(SEM_NPROC_NAME);
    sem_unlink(SEM_BARRIER_NAME);
    sem_t *sem_nproc   = sem_open(SEM_NPROC_NAME,   O_CREAT | O_EXCL, 0644, 1);
    sem_t *sem_barrier = sem_open(SEM_BARRIER_NAME, O_CREAT | O_EXCL, 0644, 0);
    assert(sem_nproc != SEM_FAILED && sem_barrier != SEM_FAILED);
    sem_close(sem_nproc);
    sem_close(sem_barrier);

    /* 2 filhos entram no barrier */
    for (int i = 0; i < 2; i++) {
        if (fork() == 0) {
            sem_t *sn = sem_open(SEM_NPROC_NAME,   0);
            sem_t *sb = sem_open(SEM_BARRIER_NAME, 0);
            sem_wait(sn);
            shm->nproc_at_barrier++;
            if (shm->nproc_at_barrier == shm->num_active_flights)
                sem_post(sb);
            sem_post(sn);
            sem_wait(sb);
            sem_post(sb);
            sem_close(sn);
            sem_close(sb);
            exit(0);
        }
    }

    /* pai simula SimulationController */
    sem_t *sb = sem_open(SEM_BARRIER_NAME, 0);
    sem_wait(sb);
    int count = shm->nproc_at_barrier;
    sem_post(sb);
    sem_close(sb);

    for (int i = 0; i < 2; i++) waitpid(-1, NULL, 0);

    sem_unlink(SEM_NPROC_NAME);
    sem_unlink(SEM_BARRIER_NAME);
    munmap(shm, sizeof(SharedSimulationData));
    shm_unlink("/aisafe_sim");

    if (count == 2) PASS(name);
    else            FAIL(name, "pai acordou antes de todos os voos chegarem");
}

 /* Test 2 -- US108.3
 * O barrier repõe-se correctamente entre steps consecutivos.*/

void test_barrier_resets_between_steps(void) {
    const char *name = "barrier_resets_between_steps";

    SharedSimulationData *shm = init_shared_memory(1);
    assert(shm != NULL);

    sem_unlink(SEM_NPROC_NAME);
    sem_unlink(SEM_BARRIER_NAME);
    sem_t *sem_nproc   = sem_open(SEM_NPROC_NAME,   O_CREAT | O_EXCL, 0644, 1);
    sem_t *sem_barrier = sem_open(SEM_BARRIER_NAME, O_CREAT | O_EXCL, 0644, 0);
    assert(sem_nproc != SEM_FAILED && sem_barrier != SEM_FAILED);
    sem_close(sem_nproc);
    sem_close(sem_barrier);

    int ok = 1;
    for (int step = 0; step < 2; step++) {
        /* reset (feito pelo SimulationController no início de cada step) */
        sem_t *sn = sem_open(SEM_NPROC_NAME, 0);
        sem_wait(sn);
        shm->nproc_at_barrier = 0;
        sem_post(sn);
        sem_close(sn);

        if (fork() == 0) {
            sem_t *sn2 = sem_open(SEM_NPROC_NAME,   0);
            sem_t *sb2 = sem_open(SEM_BARRIER_NAME, 0);
            sem_wait(sn2);
            shm->nproc_at_barrier++;
            if (shm->nproc_at_barrier == shm->num_active_flights)
                sem_post(sb2);
            sem_post(sn2);
            sem_wait(sb2);
            sem_post(sb2);
            sem_close(sn2);
            sem_close(sb2);
            exit(0);
        }

        sem_t *sb = sem_open(SEM_BARRIER_NAME, 0);
        sem_wait(sb);
        if (shm->nproc_at_barrier != 1) ok = 0;
        sem_post(sb);
        sem_close(sb);
        waitpid(-1, NULL, 0);
    }

    sem_unlink(SEM_NPROC_NAME);
    sem_unlink(SEM_BARRIER_NAME);
    munmap(shm, sizeof(SharedSimulationData));
    shm_unlink("/aisafe_sim");

    if (ok) PASS(name);
    else    FAIL(name, "contador nao era 1 em algum dos steps");
}

/* Test 3 -- US108.4
 * Saída antecipada de um voo não causa deadlock.
 * O voo que sai decrementa num_active_flights;
 * o barrier deve abrir com apenas 1 voo activo.*/

void test_barrier_no_deadlock_on_early_exit(void) {
    const char *name = "barrier_no_deadlock_on_early_exit";

    SharedSimulationData *shm = init_shared_memory(2);
    assert(shm != NULL);

    sem_unlink(SEM_NPROC_NAME);
    sem_unlink(SEM_BARRIER_NAME);
    sem_t *sem_nproc   = sem_open(SEM_NPROC_NAME,   O_CREAT | O_EXCL, 0644, 1);
    sem_t *sem_barrier = sem_open(SEM_BARRIER_NAME, O_CREAT | O_EXCL, 0644, 0);
    assert(sem_nproc != SEM_FAILED && sem_barrier != SEM_FAILED);
    sem_close(sem_nproc);
    sem_close(sem_barrier);

    /* voo 0: sai sem entrar no barrier -- decrementa num_active_flights */
    if (fork() == 0) {
        sem_t *sn = sem_open(SEM_NPROC_NAME, 0);
        sem_t *sb = sem_open(SEM_BARRIER_NAME, 0);
        sem_wait(sn);
        shm->num_active_flights--;
        if (shm->nproc_at_barrier >= shm->num_active_flights
                && shm->num_active_flights > 0)
            sem_post(sb);
        sem_post(sn);
        sem_close(sn);
        sem_close(sb);
        exit(0);
    }

    /* voo 1: entra no barrier normalmente */
    if (fork() == 0) {
        sem_t *sn = sem_open(SEM_NPROC_NAME,   0);
        sem_t *sb = sem_open(SEM_BARRIER_NAME, 0);
        sem_wait(sn);
        shm->nproc_at_barrier++;
        if (shm->nproc_at_barrier == shm->num_active_flights)
            sem_post(sb);
        sem_post(sn);
        sem_wait(sb);
        sem_post(sb);
        sem_close(sn);
        sem_close(sb);
        exit(0);
    }

    /* pai espera no barrier -- não deve bloquear */
    sem_t *sb = sem_open(SEM_BARRIER_NAME, 0);
    sem_wait(sb);
    sem_post(sb);
    sem_close(sb);

    for (int i = 0; i < 2; i++) waitpid(-1, NULL, 0);

    sem_unlink(SEM_NPROC_NAME);
    sem_unlink(SEM_BARRIER_NAME);
    munmap(shm, sizeof(SharedSimulationData));
    shm_unlink("/aisafe_sim");

    PASS(name); /* se chegou aqui não houve deadlock */
}

int main(void) {
    printf("=== US108 Acceptance Tests ===\n\n");

    test_barrier_enforces_step_ordering();
    test_barrier_resets_between_steps();
    test_barrier_no_deadlock_on_early_exit();

    printf("\nResults: %d/%d passed\n",
           tests_passed, tests_passed + tests_failed);
    return (tests_failed == 0) ? 0 : 1;
}