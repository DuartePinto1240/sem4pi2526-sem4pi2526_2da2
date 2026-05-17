#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include <sys/wait.h>
#include <math.h>

#include "../src/simulation.h"
#include "../src/flight.h"

/* ─────────────────────────────────────────
 * Test helpers
 * ───────────────────────────────────────── */

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
        printf("FAIL — %s\n", msg); \
        tests_failed++; \
    } while(0)

#define ASSERT(cond, msg) \
    do { \
        if (cond) { PASS(); } \
        else       { FAIL(msg); } \
    } while(0)

/* ─────────────────────────────────────────
 * US103.1 — synchronize_step sends a token
 *            and receives a position
 * ───────────────────────────────────────── */

void test_synchronize_step_receives_one_position() {
    TEST("synchronize_step receives one position per active flight");

    /* Set up a pipe pair and a child that mimics a flight process */
    int pos_pipe[2], step_pipe[2];
    assert(pipe(pos_pipe)  == 0);
    assert(pipe(step_pipe) == 0);

    pid_t pid = fork();
    assert(pid >= 0);

    if (pid == 0) {
        /* child — waits for token then sends a position */
        close(pos_pipe[0]);
        close(step_pipe[1]);

        char token;
        read(step_pipe[0], &token, 1);

        Position pos = {
            .latitude  = 41.2481,
            .longitude = -8.6814,
            .altitude  = 5000.0,
            .time_step = 0
        };
        write(pos_pipe[1], &pos, sizeof(Position));
        close(pos_pipe[1]);
        close(step_pipe[0]);
        exit(0);
    }

    /* parent */
    close(pos_pipe[1]);
    close(step_pipe[0]);

    FlightState states[1];
    memset(states, 0, sizeof(states));
    states[0].flight_id  = 0;
    states[0].pid        = pid;
    states[0].pipe_read  = pos_pipe[0];
    states[0].pipe_step  = step_pipe[1];
    states[0].active     = 1;

    int updates = synchronize_step(states, 1, 0);

    int status;
    waitpid(pid, &status, 0);

    ASSERT(updates == 1, "expected 1 position update");
}

void test_synchronize_step_updates_current_position() {
    TEST("synchronize_step stores received position in state");

    int pos_pipe[2], step_pipe[2];
    assert(pipe(pos_pipe)  == 0);
    assert(pipe(step_pipe) == 0);

    pid_t pid = fork();
    assert(pid >= 0);

    if (pid == 0) {
        close(pos_pipe[0]);
        close(step_pipe[1]);

        char token;
        read(step_pipe[0], &token, 1);

        Position pos = {
            .latitude  = 38.7813,
            .longitude = -9.1359,
            .altitude  = 10000.0,
            .time_step = 1
        };
        write(pos_pipe[1], &pos, sizeof(Position));
        close(pos_pipe[1]);
        close(step_pipe[0]);
        exit(0);
    }

    close(pos_pipe[1]);
    close(step_pipe[0]);

    FlightState states[1];
    memset(states, 0, sizeof(states));
    states[0].flight_id  = 0;
    states[0].pid        = pid;
    states[0].pipe_read  = pos_pipe[0];
    states[0].pipe_step  = step_pipe[1];
    states[0].active     = 1;

    synchronize_step(states, 1, 1);

    int status;
    waitpid(pid, &status, 0);

    int ok = fabs(states[0].current.latitude  - 38.7813) < 1e-4 &&
             fabs(states[0].current.longitude - (-9.1359)) < 1e-4 &&
             fabs(states[0].current.altitude  - 10000.0)   < 1e-1 &&
             states[0].current.time_step == 1;

    ASSERT(ok, "current position not updated correctly");
}

/* ─────────────────────────────────────────
 * US103.2 — Child does not send before
 *            receiving the step token
 * ───────────────────────────────────────── */

void test_child_blocks_until_token_received() {
    TEST("child blocks until step token is received");

    /*
     * The parent creates the pipes but deliberately delays
     * sending the token by 50ms. We verify that reading from
     * the position pipe before sending the token yields no data
     * (EAGAIN with O_NONBLOCK), proving the child is blocked.
     */
    int pos_pipe[2], step_pipe[2];
    assert(pipe(pos_pipe)  == 0);
    assert(pipe(step_pipe) == 0);

    pid_t pid = fork();
    assert(pid >= 0);

    if (pid == 0) {
        /* child — mimics execute_flight: wait for token, then send */
        close(pos_pipe[0]);
        close(step_pipe[1]);

        char token;
        read(step_pipe[0], &token, 1); /* blocks here */

        Position pos = { .latitude = 41.0, .longitude = -8.0,
                         .altitude = 3000.0, .time_step = 0 };
        write(pos_pipe[1], &pos, sizeof(Position));
        close(pos_pipe[1]);
        close(step_pipe[0]);
        exit(0);
    }

    close(pos_pipe[1]);
    close(step_pipe[0]);

    /* try a non-blocking read before sending the token */
    Position dummy;
    ssize_t n;

    /* set pos_pipe[0] non-blocking */
    int flags;
    #include <fcntl.h>
    flags = fcntl(pos_pipe[0], F_GETFL, 0);
    fcntl(pos_pipe[0], F_SETFL, flags | O_NONBLOCK);

    n = read(pos_pipe[0], &dummy, sizeof(Position));
    int no_data_yet = (n < 0); /* EAGAIN expected — no data without token */

    /* now send the token and verify the child replies */
    char token = 0;
    fcntl(pos_pipe[0], F_SETFL, flags); /* restore blocking */
    write(step_pipe[1], &token, 1);
    n = read(pos_pipe[0], &dummy, sizeof(Position));
    int got_data = (n == (ssize_t)sizeof(Position));

    close(pos_pipe[0]);
    close(step_pipe[1]);

    int status;
    waitpid(pid, &status, 0);

    ASSERT(no_data_yet && got_data,
           "child sent data before receiving step token");
}

/* ─────────────────────────────────────────
 * US103.3 — All flights advance together
 *            (two-flight lockstep)
 * ───────────────────────────────────────── */

void test_two_flights_advance_in_lockstep() {
    TEST("two flights both reply before parent advances");

    int pos0[2], step0[2];
    int pos1[2], step1[2];
    assert(pipe(pos0)  == 0);
    assert(pipe(step0) == 0);
    assert(pipe(pos1)  == 0);
    assert(pipe(step1) == 0);

    /* child 0 */
    pid_t pid0 = fork();
    assert(pid0 >= 0);
    if (pid0 == 0) {
        close(pos0[0]); close(step0[1]);
        close(pos1[0]); close(pos1[1]);
        close(step1[0]); close(step1[1]);
        char tok; read(step0[0], &tok, 1);
        Position p = {41.0, -8.0, 5000.0, 0};
        write(pos0[1], &p, sizeof(p));
        close(pos0[1]); close(step0[0]);
        exit(0);
    }

    /* child 1 */
    pid_t pid1 = fork();
    assert(pid1 >= 0);
    if (pid1 == 0) {
        close(pos0[0]); close(pos0[1]);
        close(step0[0]); close(step0[1]);
        close(pos1[0]); close(step1[1]);
        char tok; read(step1[0], &tok, 1);
        Position p = {42.0, -7.0, 6000.0, 0};
        write(pos1[1], &p, sizeof(p));
        close(pos1[1]); close(step1[0]);
        exit(0);
    }

    close(pos0[1]); close(step0[0]);
    close(pos1[1]); close(step1[0]);

    FlightState states[2];
    memset(states, 0, sizeof(states));
    states[0].flight_id = 0; states[0].pid = pid0;
    states[0].pipe_read = pos0[0]; states[0].pipe_step = step0[1];
    states[0].active    = 1;
    states[1].flight_id = 1; states[1].pid = pid1;
    states[1].pipe_read = pos1[0]; states[1].pipe_step = step1[1];
    states[1].active    = 1;

    int updates = synchronize_step(states, 2, 0);

    int s0, s1;
    waitpid(pid0, &s0, 0);
    waitpid(pid1, &s1, 0);

    ASSERT(updates == 2, "expected 2 updates from 2 flights");
}

/* ─────────────────────────────────────────
 * US103.4 — History is stored per step
 * ───────────────────────────────────────── */

void test_history_is_accumulated_over_steps() {
    TEST("position history accumulates over multiple steps");

    int pos_pipe[2], step_pipe[2];
    assert(pipe(pos_pipe)  == 0);
    assert(pipe(step_pipe) == 0);

    pid_t pid = fork();
    assert(pid >= 0);

    if (pid == 0) {
        close(pos_pipe[0]);
        close(step_pipe[1]);

        for (int s = 0; s < 3; s++) {
            char tok; read(step_pipe[0], &tok, 1);
            Position p = {41.0 + s, -8.0, 5000.0, s};
            write(pos_pipe[1], &p, sizeof(p));
        }
        close(pos_pipe[1]);
        close(step_pipe[0]);
        exit(0);
    }

    close(pos_pipe[1]);
    close(step_pipe[0]);

    FlightState states[1];
    memset(states, 0, sizeof(states));
    states[0].flight_id = 0; states[0].pid = pid;
    states[0].pipe_read = pos_pipe[0];
    states[0].pipe_step = step_pipe[1];
    states[0].active    = 1;

    synchronize_step(states, 1, 0);
    synchronize_step(states, 1, 1);
    synchronize_step(states, 1, 2);

    close(step_pipe[1]);

    int status;
    waitpid(pid, &status, 0);

    /* after 3 steps: history should have 2 entries
     * (step 0 and step 1 positions were pushed to history
     *  as step 1 and step 2 arrived) */
    ASSERT(states[0].history_count == 3,
           "expected 3 history entries after 3 steps");
}

/* ─────────────────────────────────────────
 * US103.5 — Inactive flights are skipped
 * ───────────────────────────────────────── */

void test_inactive_flights_are_skipped() {
    TEST("inactive flights are skipped in synchronize_step");

    int pos_pipe[2], step_pipe[2];
    assert(pipe(pos_pipe)  == 0);
    assert(pipe(step_pipe) == 0);

    FlightState states[2];
    memset(states, 0, sizeof(states));

    /* flight 0 — active, backed by real pipes */
    pid_t pid = fork();
    assert(pid >= 0);
    if (pid == 0) {
        close(pos_pipe[0]); close(step_pipe[1]);
        char tok; read(step_pipe[0], &tok, 1);
        Position p = {41.0, -8.0, 5000.0, 0};
        write(pos_pipe[1], &p, sizeof(p));
        close(pos_pipe[1]); close(step_pipe[0]);
        exit(0);
    }
    close(pos_pipe[1]); close(step_pipe[0]);

    states[0].flight_id = 0; states[0].pid = pid;
    states[0].pipe_read = pos_pipe[0];
    states[0].pipe_step = step_pipe[1];
    states[0].active    = 1;

    /* flight 1 — inactive, no pipes needed */
    states[1].flight_id = 1;
    states[1].active    = 0;

    int updates = synchronize_step(states, 2, 0);

    int status;
    waitpid(pid, &status, 0);

    ASSERT(updates == 1,
           "expected 1 update (inactive flight should be skipped)");
}

/* ─────────────────────────────────────────
 * Main
 * ───────────────────────────────────────── */

int main(void) {
    printf("\n========================================\n");
    printf("  AISafe US103 — Synchronization Tests\n");
    printf("========================================\n\n");

    printf("[ US103.1 — Step token + position exchange ]\n");
    test_synchronize_step_receives_one_position();
    test_synchronize_step_updates_current_position();

    printf("\n[ US103.2 — Child blocks until token ]\n");
    test_child_blocks_until_token_received();

    printf("\n[ US103.3 — Two-flight lockstep ]\n");
    test_two_flights_advance_in_lockstep();

    printf("\n[ US103.4 — History accumulation ]\n");
    test_history_is_accumulated_over_steps();

    printf("\n[ US103.5 — Inactive flights skipped ]\n");
    test_inactive_flights_are_skipped();

    printf("\n========================================\n");
    printf("  Results: %d/%d passed", tests_passed, tests_run);
    if (tests_failed > 0)
        printf(" (%d FAILED)", tests_failed);
    printf("\n========================================\n\n");

    return tests_failed > 0 ? EXIT_FAILURE : EXIT_SUCCESS;
}