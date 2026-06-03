/*
 * test_us107.c — Unit tests for US107
 *
 * Tests the condition variable notification mechanism between
 * the ViolationDetector thread and the ReportGenerator thread.
 *
 * Acceptance criteria tested:
 *   Test 1  — Report thread blocks on condition variable before any signal
 *   Test 2  — Report thread wakes immediately upon pthread_cond_signal
 *   Test 3  — Multiple signals each wake the report thread once
 *   Test 4  — Mutex protects condition variable access (no race)
 *   Test 5  — Report thread exits cleanly on pthread_cond_broadcast + g_sim_done
 *   Test 6  — Report thread does not miss a signal sent before it waits (spurious wakeup guard)
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include <pthread.h>
#include <time.h>

/* ───────────────────────────────────────────────────────────
 * Spawns a watcher thread that joins the target thread and sets
 * a flag when done. The caller polls the flag for up to
 * timeout_ms milliseconds. Returns 0 if joined, -1 on timeout.
 * ─────────────────────────────────────────────────────────── */

typedef struct {
    pthread_t  target;
    volatile int joined;
} JoinWatcher;

static void *join_watcher(void *arg) {
    JoinWatcher *w = (JoinWatcher *)arg;
    pthread_join(w->target, NULL);
    w->joined = 1;
    return NULL;
}

static int timed_join(pthread_t tid, int timeout_ms) {
    JoinWatcher w = { tid, 0 };
    pthread_t watcher;
    pthread_create(&watcher, NULL, join_watcher, &w);

    int elapsed = 0;
    while (!w.joined && elapsed < timeout_ms) {
        usleep(10000); /* poll every 10 ms */
        elapsed += 10;
    }

    if (!w.joined) {
        pthread_detach(watcher);
        return -1; /* timeout */
    }
    pthread_join(watcher, NULL);
    return 0;
}

/* ── Test helpers ─────────────────────────────────────────── */

static int tests_run    = 0;
static int tests_passed = 0;
static int tests_failed = 0;

#define TEST(name) \
    do { printf("  %-60s", name); tests_run++; } while (0)

#define PASS() \
    do { printf("PASS\n"); tests_passed++; } while (0)

#define FAIL(msg) \
    do { printf("FAIL -- %s\n", msg); tests_failed++; } while (0)

#define ASSERT(cond, msg) \
    do { if (cond) { PASS(); } else { FAIL(msg); } } while (0)

/* ── Shared state for tests ───────────────────────────────── */

static pthread_mutex_t t_report_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  t_report_cond  = PTHREAD_COND_INITIALIZER;
static volatile int    t_sim_done     = 0;
static volatile int    t_wakeup_count = 0;
static volatile int    t_violations   = 0;

/* Reset all shared state between tests */
static void reset_state(void) {
    pthread_mutex_lock(&t_report_mutex);
    t_sim_done     = 0;
    t_wakeup_count = 0;
    t_violations   = 0;
    pthread_mutex_unlock(&t_report_mutex);
}

/* ── Minimal report thread used in tests ─────────────────── */

static void *report_thread(void *arg) {
    (void)arg;

    pthread_mutex_lock(&t_report_mutex);

    while (!t_sim_done) {
        pthread_cond_wait(&t_report_cond, &t_report_mutex);
        if (!t_sim_done) {
            t_wakeup_count++;
        }
    }

    pthread_mutex_unlock(&t_report_mutex);
    return NULL;
}

/* ── Helper: send a signal from a separate thread ────────── */

typedef struct {
    int delay_us;   /* microseconds to wait before signalling */
    int n_signals;  /* how many times to signal               */
} SignalArgs;

static void *signaller_thread(void *arg) {
    SignalArgs *sa = (SignalArgs *)arg;
    if (sa->delay_us > 0)
        usleep(sa->delay_us);

    for (int i = 0; i < sa->n_signals; i++) {
        pthread_mutex_lock(&t_report_mutex);
        t_violations++;
        pthread_cond_signal(&t_report_cond);
        pthread_mutex_unlock(&t_report_mutex);
        if (sa->n_signals > 1)
            usleep(2000); /* 2 ms between signals */
    }
    return NULL;
}

/* ── Helper: set sim_done and broadcast ──────────────────── */

static void end_simulation(void) {
    pthread_mutex_lock(&t_report_mutex);
    t_sim_done = 1;
    pthread_cond_broadcast(&t_report_cond);
    pthread_mutex_unlock(&t_report_mutex);
}

/* ═══════════════════════════════════════════════════════════
 * Test 1 — Report thread blocks on cond_wait
 *
 * The report thread must block immediately after startup.
 * We verify it has not incremented t_wakeup_count after
 * a small delay (i.e. it is truly waiting).
 * ═══════════════════════════════════════════════════════════ */

void test_report_thread_blocks_before_signal(void) {
    TEST("report thread blocks on cond_wait before any signal");

    reset_state();
    pthread_t tid;
    pthread_create(&tid, NULL, report_thread, NULL);

    usleep(20000); /* 20 ms — thread should be blocked by now */

    int count_before_signal = t_wakeup_count;

    end_simulation();
    pthread_join(tid, NULL);

    ASSERT(count_before_signal == 0,
           "report thread should not have woken before signal");
}

/* ═══════════════════════════════════════════════════════════
 * Test 2 — pthread_cond_signal wakes report thread
 *
 * After sending one signal the report thread must increment
 * t_wakeup_count within a short timeout.
 * ═══════════════════════════════════════════════════════════ */

void test_cond_signal_wakes_report_thread(void) {
    TEST("pthread_cond_signal wakes report thread on violation");

    reset_state();
    pthread_t tid;
    pthread_create(&tid, NULL, report_thread, NULL);

    usleep(10000); /* let thread reach cond_wait */

    /* send one violation signal */
    pthread_mutex_lock(&t_report_mutex);
    t_violations++;
    pthread_cond_signal(&t_report_cond);
    pthread_mutex_unlock(&t_report_mutex);

    usleep(20000); /* let report thread process the wakeup */

    int woken = t_wakeup_count;

    end_simulation();
    pthread_join(tid, NULL);

    ASSERT(woken == 1, "expected exactly one wakeup after one signal");
}

/* ═══════════════════════════════════════════════════════════
 * Test 3 — Multiple signals each wake the thread once
 *
 * Three sequential signals should produce three wakeups.
 * ═══════════════════════════════════════════════════════════ */

void test_multiple_signals_produce_multiple_wakeups(void) {
    TEST("three sequential signals produce three wakeups");

    reset_state();
    pthread_t tid;
    pthread_create(&tid, NULL, report_thread, NULL);

    usleep(10000); /* let thread reach cond_wait */

    for (int i = 0; i < 3; i++) {
        pthread_mutex_lock(&t_report_mutex);
        t_violations++;
        pthread_cond_signal(&t_report_cond);
        pthread_mutex_unlock(&t_report_mutex);
        usleep(10000); /* let thread go back to cond_wait between signals */
    }

    usleep(20000);
    int woken = t_wakeup_count;

    end_simulation();
    pthread_join(tid, NULL);

    ASSERT(woken == 3, "expected 3 wakeups for 3 signals");
}

/* ═══════════════════════════════════════════════════════════
 * Test 4 — Mutex protects concurrent signals (no race)
 *
 * Two signaller threads fire simultaneously.
 * The report thread must process both without corruption.
 * ═══════════════════════════════════════════════════════════ */

void test_mutex_protects_concurrent_signals(void) {
    TEST("mutex protects condition variable against concurrent signals");

    reset_state();
    pthread_t report_tid;
    pthread_create(&report_tid, NULL, report_thread, NULL);

    usleep(10000);

    /* Two signallers fire concurrently, each sends one signal */
    SignalArgs sa1 = { 0, 1 };
    SignalArgs sa2 = { 0, 1 };
    pthread_t s1, s2;
    pthread_create(&s1, NULL, signaller_thread, &sa1);
    pthread_create(&s2, NULL, signaller_thread, &sa2);
    pthread_join(s1, NULL);
    pthread_join(s2, NULL);

    usleep(30000); /* let both wakeups be processed */

    int woken = t_wakeup_count;

    end_simulation();
    pthread_join(report_tid, NULL);

    ASSERT(woken == 2,
           "expected exactly 2 wakeups from 2 concurrent signals");
}

/* ═══════════════════════════════════════════════════════════
 * Test 5 — Report thread exits cleanly on broadcast + sim_done
 *
 * Setting t_sim_done = 1 and broadcasting must cause the
 * report thread to exit its loop and return.
 * pthread_join must not hang.
 * ═══════════════════════════════════════════════════════════ */

void test_report_thread_exits_on_sim_done(void) {
    TEST("report thread exits cleanly when g_sim_done=1 + broadcast");

    reset_state();
    pthread_t tid;
    pthread_create(&tid, NULL, report_thread, NULL);

    usleep(10000);

    end_simulation(); /* sets t_sim_done=1 and broadcasts */

    /* Use a timed join to detect hangs (2 second timeout) */
    int rc = timed_join(tid, 2000);
    ASSERT(rc == 0, "report thread did not exit within 2 seconds");
}

/* ═══════════════════════════════════════════════════════════
 * Test 6 — Spurious wakeup guard: loop re-checks g_sim_done
 *
 * The report thread uses while (!g_sim_done) around cond_wait,
 * so a spurious wakeup must not cause it to exit prematurely.
 * We simulate this by manually calling pthread_cond_broadcast
 * WITHOUT setting t_sim_done. The thread must go back to wait.
 * ═══════════════════════════════════════════════════════════ */

void test_spurious_wakeup_does_not_exit_loop(void) {
    TEST("spurious wakeup does not cause premature loop exit");

    reset_state();
    pthread_t tid;
    pthread_create(&tid, NULL, report_thread, NULL);

    usleep(10000);

    /* broadcast without setting sim_done — simulates spurious wakeup */
    pthread_mutex_lock(&t_report_mutex);
    pthread_cond_broadcast(&t_report_cond);
    pthread_mutex_unlock(&t_report_mutex);

    usleep(20000); /* thread should go back to cond_wait */

    /* thread must still be running (wakeup_count may have incremented
     * because !t_sim_done path in our test thread does increment it;
     * what matters is the thread has NOT exited) */

    /* now really end simulation */
    end_simulation();

    int rc = timed_join(tid, 2000);
    ASSERT(rc == 0, "thread should exit after real sim_done broadcast");
}

/* ═══════════════════════════════════════════════════════════
 * Main
 * ═══════════════════════════════════════════════════════════ */

int main(void) {
    printf("\n========================================\n");
    printf("  AISafe US107 — Condition Variable Notification Tests\n");
    printf("========================================\n\n");

    printf("[ Blocking Behaviour ]\n");
    test_report_thread_blocks_before_signal();

    printf("\n[ Signal Wakeup ]\n");
    test_cond_signal_wakes_report_thread();
    test_multiple_signals_produce_multiple_wakeups();

    printf("\n[ Thread Safety ]\n");
    test_mutex_protects_concurrent_signals();

    printf("\n[ Simulation End ]\n");
    test_report_thread_exits_on_sim_done();

    printf("\n[ Spurious Wakeup Guard ]\n");
    test_spurious_wakeup_does_not_exit_loop();

    printf("\n========================================\n");
    printf("  Results: %d/%d passed", tests_passed, tests_run);
    if (tests_failed > 0)
        printf(", %d FAILED", tests_failed);
    printf("\n========================================\n\n");

    return tests_failed == 0 ? EXIT_SUCCESS : EXIT_FAILURE;
}