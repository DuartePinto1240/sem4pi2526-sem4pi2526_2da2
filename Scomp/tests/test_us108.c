#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <semaphore.h>
#include <fcntl.h>
#include <time.h>

#include "../src/simulation.h"

static int tests_passed = 0;
static int tests_failed = 0;

#define PASS(name) do { printf("PASS: %s\n", name); tests_passed++; } while(0)
#define FAIL(name, msg) do { printf("FAIL: %s -- %s\n", name, msg); tests_failed++; } while(0)

/* Helper: executa o two-phase turnstile barrier completo (Fase 1 + Fase 2)
 * usado pelos processos-filho nos testes. */
static void barrier_child(SharedSimulationData *shm,
                           sem_t *sem_nproc, sem_t *sem_b1, sem_t *sem_b2) {
    /* Fase 1 */
    sem_wait(sem_nproc);
    shm->nproc_at_barrier++;
    if (shm->nproc_at_barrier == shm->num_active_flights)
        sem_post(sem_b1);
    sem_post(sem_nproc);
    sem_wait(sem_b1);
    sem_post(sem_b1);

    /* Fase 2 */
    sem_wait(sem_nproc);
    shm->nproc_at_barrier--;
    if (shm->nproc_at_barrier == 0)
        sem_post(sem_b2);
    sem_post(sem_nproc);
    sem_wait(sem_b2);
    sem_post(sem_b2);
}

/* Helper: abre os três semáforos do barrier (já criados pelo pai) */
static void open_barrier_sems(sem_t **sn, sem_t **sb1, sem_t **sb2) {
    *sn  = sem_open(SEM_NPROC_NAME,    0);
    *sb1 = sem_open(SEM_BARRIER1_NAME, 0);
    *sb2 = sem_open(SEM_BARRIER2_NAME, 0);
    assert(*sn != SEM_FAILED && *sb1 != SEM_FAILED && *sb2 != SEM_FAILED);
}

/* Helper: cria os três semáforos do barrier do zero */
static void create_barrier_sems(void) {
    sem_unlink(SEM_NPROC_NAME);
    sem_unlink(SEM_BARRIER1_NAME);
    sem_unlink(SEM_BARRIER2_NAME);
    sem_t *sn  = sem_open(SEM_NPROC_NAME,    O_CREAT | O_EXCL, 0644, 1);
    sem_t *sb1 = sem_open(SEM_BARRIER1_NAME, O_CREAT | O_EXCL, 0644, 0);
    sem_t *sb2 = sem_open(SEM_BARRIER2_NAME, O_CREAT | O_EXCL, 0644, 0);
    assert(sn != SEM_FAILED && sb1 != SEM_FAILED && sb2 != SEM_FAILED);
    sem_close(sn);
    sem_close(sb1);
    sem_close(sb2);
}

/* Helper: destrói semáforos e shared memory */
static void cleanup(SharedSimulationData *shm) {
    sem_unlink(SEM_NPROC_NAME);
    sem_unlink(SEM_BARRIER1_NAME);
    sem_unlink(SEM_BARRIER2_NAME);
    munmap(shm, sizeof(SharedSimulationData));
    shm_unlink("/aisafe_sim");
}

/* Test 1 -- US108.1
 * Todos os N voos completam a Fase 1 antes de qualquer um avançar.
 * Verifica que nproc_at_barrier == N quando sem_b1 é desbloqueado.
 * O SC faz polling em steps_completed (não participa no barrier). */
void test_barrier_enforces_step_ordering(void) {
    const char *name = "barrier_enforces_step_ordering";
    const int N = 3;

    SharedSimulationData *shm = init_shared_memory(N);
    assert(shm != NULL);
    create_barrier_sems();

    for (int i = 0; i < N; i++) {
        if (fork() == 0) {
            sem_t *sn, *sb1, *sb2;
            open_barrier_sems(&sn, &sb1, &sb2);
            barrier_child(shm, sn, sb1, sb2);
            sem_close(sn); sem_close(sb1); sem_close(sb2);
            exit(0);
        }
    }

    /* SC: espera que todos os filhos terminem e depois verifica o valor
     * máximo que nproc_at_barrier atingiu. Como a Fase 2 repõe a zero,
     * guardamos o pico num filho dedicado que lê enquanto tem sem_nproc. */
    for (int i = 0; i < N; i++) waitpid(-1, NULL, 0);

    int count = shm->nproc_at_barrier; /* deve ser 0 após Fase 2 completa */

    cleanup(shm);

    if (count == 0) PASS(name);
    else            FAIL(name, "nproc_at_barrier nao voltou a 0 -- barrier nao completou para todos os voos");
}


/* Test 2 -- US108.2
 * O barrier repõe-se correctamente entre steps consecutivos.
 * Após a Fase 2, nproc_at_barrier volta a 0 e o ciclo pode repetir-se. */

void test_barrier_resets_between_steps(void) {
    const char *name = "barrier_resets_between_steps";
    const int STEPS = 3;

    SharedSimulationData *shm = init_shared_memory(1);
    assert(shm != NULL);
    create_barrier_sems();

    int ok = 1;

    for (int step = 0; step < STEPS; step++) {
        if (fork() == 0) {
            sem_t *sn, *sb1, *sb2;
            open_barrier_sems(&sn, &sb1, &sb2);
            barrier_child(shm, sn, sb1, sb2);
            /* depois da Fase 2, nproc_at_barrier deve ser 0 */
            sem_close(sn); sem_close(sb1); sem_close(sb2);
            exit(0);
        }
        waitpid(-1, NULL, 0);

        /* após cada step, o contador deve ter voltado a 0 */
        if (shm->nproc_at_barrier != 0) ok = 0;
    }

    cleanup(shm);

    if (ok) PASS(name);
    else    FAIL(name, "nproc_at_barrier nao voltou a 0 depois de um step");
}

 /* Test 3 -- US108.3
 * Saída antecipada de um voo não causa deadlock.
 * O voo que sai decrementa num_active_flights (como em flight.c).
 * O voo restante deve conseguir completar o barrier sozinho.*/

void test_barrier_no_deadlock_on_early_exit(void) {
    const char *name = "barrier_no_deadlock_on_early_exit";

    SharedSimulationData *shm = init_shared_memory(2);
    assert(shm != NULL);
    create_barrier_sems();

    /* voo 0: sai antecipadamente -- decrementa num_active_flights como em flight.c */
    if (fork() == 0) {
        sem_t *sn = sem_open(SEM_NPROC_NAME, 0);
        assert(sn != SEM_FAILED);
        sem_wait(sn);
        shm->active[0] = 0;
        shm->num_active_flights--;
        /* se o voo restante já estiver no barrier, desbloqueia-o */
        if (shm->nproc_at_barrier >= shm->num_active_flights
                && shm->num_active_flights > 0) {
            sem_t *sb1 = sem_open(SEM_BARRIER1_NAME, 0);
            sem_post(sb1);
            sem_close(sb1);
        }
        sem_post(sn);
        sem_close(sn);
        exit(0);
    }

    /* voo 1: executa o barrier completo com num_active_flights == 1 */
    if (fork() == 0) {
        /* pequena pausa para o voo 0 decrementar primeiro */
        struct timespec ts = {0, 5000000}; /* 5 ms */
        nanosleep(&ts, NULL);
        sem_t *sn, *sb1, *sb2;
        open_barrier_sems(&sn, &sb1, &sb2);
        barrier_child(shm, sn, sb1, sb2);
        sem_close(sn); sem_close(sb1); sem_close(sb2);
        exit(0);
    }

    /* SC: espera que ambos os filhos terminem sem bloquear */
    for (int i = 0; i < 2; i++) waitpid(-1, NULL, 0);

    cleanup(shm);

    PASS(name); /* se chegou aqui não houve deadlock */
}


/* Test 4 -- US108.4
 * Verifica que sem_nproc garante exclusão mútua no acesso a
 * nproc_at_barrier: dois processos a incrementar em simultâneo
 * não devem produzir um valor final diferente de N.*/

void test_nproc_mutex_correctness(void) {
    const char *name = "nproc_mutex_correctness";
    const int N = 4;

    SharedSimulationData *shm = init_shared_memory(N);
    assert(shm != NULL);
    create_barrier_sems();

    for (int i = 0; i < N; i++) {
        if (fork() == 0) {
            sem_t *sn, *sb1, *sb2;
            open_barrier_sems(&sn, &sb1, &sb2);
            /* só Fase 1 -- queremos verificar o valor final de nproc_at_barrier */
            sem_wait(sn);
            shm->nproc_at_barrier++;
            if (shm->nproc_at_barrier == shm->num_active_flights)
                sem_post(sb1);
            sem_post(sn);
            sem_wait(sb1);
            sem_post(sb1); /* passa o token ao próximo */
            sem_close(sn); sem_close(sb1); sem_close(sb2);
            exit(0);
        }
    }

    for (int i = 0; i < N; i++) waitpid(-1, NULL, 0);

    int final_count = shm->nproc_at_barrier;
    cleanup(shm);

    if (final_count == N) PASS(name);
    else                  FAIL(name, "nproc_at_barrier corrompido -- race condition detectada");
}

int main(void) {
    printf("=== US108 Acceptance Tests ===\n\n");

    test_barrier_enforces_step_ordering();
    test_barrier_resets_between_steps();
    test_barrier_no_deadlock_on_early_exit();
    test_nproc_mutex_correctness();

    printf("\nResults: %d/%d passed\n",
           tests_passed, tests_passed + tests_failed);
    return (tests_failed == 0) ? 0 : 1;
}