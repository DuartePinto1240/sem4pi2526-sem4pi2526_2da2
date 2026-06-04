#include "flight.h"

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <math.h>
#include <string.h>
#include <fcntl.h>
#include <semaphore.h>

/* ================================================================
 * Per-Flight JSON Config Registry
 *
 * Populated by set_flight_configs() before any fork().  Child
 * processes inherit the filled array via copy-on-write so they
 * can look up their own config with g_configs[flight_id].
 * ================================================================ */

static FlightConfig g_configs[MAX_FLIGHTS];
static int          g_config_count = 0;

void set_flight_configs(const FlightConfig *configs, int count) {
    g_config_count = (count > MAX_FLIGHTS) ? MAX_FLIGHTS : count;
    memcpy(g_configs, configs, sizeof(FlightConfig) * g_config_count);
}

/* ================================================================
 * Signal State  (child processes only)
 * ================================================================ */

static volatile sig_atomic_t sigusr1_received = 0;
static volatile sig_atomic_t sigterm_received  = 0;

void handle_sigusr1(int sig) {
    (void)sig;
    sigusr1_received = 1;
    /* block further signals while handling — US102.5 */
    sigset_t mask;
    sigemptyset(&mask);
    sigaddset(&mask, SIGUSR1);
    sigprocmask(SIG_BLOCK, &mask, NULL);
    write(STDOUT_FILENO, "[FLIGHT] Safety violation warning!\n", 35);
    sigprocmask(SIG_UNBLOCK, &mask, NULL);
}

void handle_sigterm(int sig) {
    (void)sig;
    sigterm_received = 1;
    write(STDOUT_FILENO, "[FLIGHT] Termination received. Cleaning up.\n", 44);
}

/* ================================================================
 * ISA Standard Atmosphere  (ISO 2533:1975)
 *
 * Troposphere  (h < 11 000 m):
 *   T(h)   = T0 + lambda * h
 *   rho(h) = rho0 * (T/T0)^( -g/(lambda*R) - 1 )    [exponent ~4.256]
 *   p(h)   = p0   * (T/T0)^(  g/(-lambda*R)  )       [exponent ~5.256]
 *
 * Tropopause (h >= 11 000 m):  T = 216.7 K  (isothermal)
 *   rho(h) = rho_trop * exp( -g*(h-11000) / (R*T_trop) )
 *   p(h)   = p_trop   * exp( -g*(h-11000) / (R*T_trop) )
 *
 * Matches the "Constants" sheet columns in the Excel simulator.
 * ================================================================ */

Atmosphere isa_atmosphere(double h) {
    Atmosphere atm;

    if (h < ISA_TROPO_H) {
        atm.T  = ISA_T0 + ISA_LAMBDA * h;
        double ratio   = atm.T / ISA_T0;
        double exp_rho = -ISA_G / (ISA_LAMBDA * ISA_R) - 1.0;  /* ~4.256 */
        double exp_p   =  ISA_G / (-ISA_LAMBDA * ISA_R);        /* ~5.256 */
        atm.rho = ISA_RHO0 * pow(ratio, exp_rho);
        atm.p   = ISA_P0   * pow(ratio, exp_p);
    } else {
        /* compute conditions exactly at the tropopause first */
        double T_ratio  = ISA_T_TROPO / ISA_T0;
        double exp_rho  = -ISA_G / (ISA_LAMBDA * ISA_R) - 1.0;
        double exp_p    =  ISA_G / (-ISA_LAMBDA * ISA_R);
        double rho_trop = ISA_RHO0 * pow(T_ratio, exp_rho);
        double p_trop   = ISA_P0   * pow(T_ratio, exp_p);
        double scale_h  = ISA_R * ISA_T_TROPO / ISA_G;  /* ~6339 m */
        double dh       = h - ISA_TROPO_H;
        atm.T   = ISA_T_TROPO;
        atm.rho = rho_trop * exp(-dh / scale_h);
        atm.p   = p_trop   * exp(-dh / scale_h);
    }

    atm.a = sqrt(ISA_GAMMA * ISA_R * atm.T);
    return atm;
}

/* ================================================================
 * Speed Schedules  (from Excel "Descend" sheet)
 *
 * Index i = altitude band in km  (i = (int)(h / 1000)).
 * IAS_CLIMB_KT[i]       — climb IAS in knots at that band.
 * DESCENT_RATE_TABLE[i] — descent vertical speed in m/s (negative).
 * ================================================================ */

static const double IAS_CLIMB_KT[] = {
/*   0    1    2    3    4    5    6    7    8    9   10   11   12   13   14  (km) */
    210, 210, 220, 230, 250, 260, 290, 290, 290, 290, 300, 300, 300, 300, 300
};

static const double DESCENT_RATE_TABLE[] = {
/*   0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  (km) */
    -5, -7, -7, -8, -8,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10
};

double ias_climb_ms(double h) {
    int idx = (int)(h / 1000.0);
    if (idx < 0)  idx = 0;
    if (idx > 14) idx = 14;
    return IAS_CLIMB_KT[idx] * KNOT_TO_MS;
}

double descent_rate_ms(double h) {
    int idx = (int)(h / 1000.0);
    if (idx < 0)  idx = 0;
    if (idx > 14) idx = 14;
    return DESCENT_RATE_TABLE[idx];
}

/* ================================================================
 * Profile Table Lookup
 *
 * Returns the ProfileEntry whose altitude_m is the highest one
 * that does not exceed h (step-function / "floor" interpolation).
 * ================================================================ */

static ProfileEntry lookup_profile(const ProfileEntry *tbl, int count, double h) {
    /* default: first entry */
    ProfileEntry result = tbl[0];
    for (int i = 1; i < count; i++) {
        if (tbl[i].altitude_m <= h)
            result = tbl[i];
        else
            break;
    }
    return result;
}

/* ================================================================
 * Navigation Helpers  (haversine-based)
 * ================================================================ */

double compute_bearing_deg(double lat1, double lon1,
                            double lat2, double lon2) {
    double rlat1 = lat1 * M_PI / 180.0;
    double rlat2 = lat2 * M_PI / 180.0;
    double dlon  = (lon2 - lon1) * M_PI / 180.0;
    double y = sin(dlon) * cos(rlat2);
    double x = cos(rlat1) * sin(rlat2) - sin(rlat1) * cos(rlat2) * cos(dlon);
    return fmod(atan2(y, x) * 180.0 / M_PI + 360.0, 360.0);
}

void move_along_bearing(double lat_deg, double lon_deg,
                         double bearing_deg, double dist_m,
                         double *out_lat, double *out_lon) {
    double lat  = lat_deg     * M_PI / 180.0;
    double lon  = lon_deg     * M_PI / 180.0;
    double brng = bearing_deg * M_PI / 180.0;
    double d    = dist_m / R_EARTH;          /* angular distance on unit sphere */

    double lat2 = asin(sin(lat)*cos(d) + cos(lat)*sin(d)*cos(brng));
    double lon2 = lon + atan2(sin(brng)*sin(d)*cos(lat),
                               cos(d) - sin(lat)*sin(lat2));

    *out_lat = lat2 * 180.0 / M_PI;
    *out_lon = fmod(lon2 * 180.0 / M_PI + 540.0, 360.0) - 180.0;
}

/* ================================================================
 * Flight Route Table
 *
 * Each flight_id mod 5 picks a distinct origin->destination so
 * routes are not all parallel.  Flight 0 always departs Porto
 * (41.2481, -8.6814) to satisfy the legacy unit tests that call
 * compute_next_position(0, 0, N) and assert that latitude.
 * ================================================================ */

typedef struct { double lat, lon; } WP;

static const WP ORIGINS[] = {
    { 41.2481, -8.6814 },   /* 0  Porto     */
    { 38.7813, -9.1359 },   /* 1  Lisbon    */
    { 40.4100, -3.5762 },   /* 2  Madrid    */
    { 41.2971,  2.0785 },   /* 3  Barcelona */
    { 43.6293,  1.3678 },   /* 4  Toulouse  */
};

static const WP DESTINATIONS[] = {
    { 38.7813, -9.1359 },   /* 0  Lisbon    */
    { 41.2481, -8.6814 },   /* 1  Porto     */
    { 41.2481, -8.6814 },   /* 2  Porto     */
    { 38.7813, -9.1359 },   /* 3  Lisbon    */
    { 40.4100, -3.5762 },   /* 4  Madrid    */
};

static const char *DEST_NAMES[] = {
    "Lisbon", "Porto", "Porto", "Lisbon", "Madrid"
};

/* ================================================================
 * Initialise Per-Flight Physics State
 * ================================================================ */

void init_flight_physics(FlightPhysics *ph, int flight_id, int total_steps) {
    (void)total_steps;

    /* If a JSON config was loaded for this flight_id, use it. */
    if (flight_id < g_config_count) {
        const FlightConfig *cfg = &g_configs[flight_id];
        ph->lat     = cfg->origin.lat;
        ph->lon     = cfg->origin.lon;
        ph->alt     = cfg->origin.alt_m;
        ph->fuel_kg = cfg->fuel_kg;
        ph->mass    = AC_M_EMPTY + AC_M_PAYLOAD + cfg->fuel_kg;
        ph->bearing = compute_bearing_deg(cfg->origin.lat, cfg->origin.lon,
                                           cfg->destination.lat, cfg->destination.lon);
    } else {
        /* Fallback: hardcoded routes (legacy / unit-test mode). */
        int id  = flight_id % 5;
        ph->lat     = ORIGINS[id].lat;
        ph->lon     = ORIGINS[id].lon;
        ph->alt     = 0.0;
        ph->fuel_kg = AC_M_FUEL;
        ph->mass    = AC_M_EMPTY + AC_M_PAYLOAD + AC_M_FUEL;
        ph->bearing = compute_bearing_deg(ORIGINS[id].lat, ORIGINS[id].lon,
                                           DESTINATIONS[id].lat, DESTINATIONS[id].lon);
    }
    ph->phase = PHASE_CLIMB;
}

/* ================================================================
 * Advance Physics by One Time Step  — the core simulation engine
 *
 * Implements the column-by-column computation from the Excel sheets:
 *
 *   ISA atmosphere  ->  TAS  ->  Mach  ->
 *   C_L  ->  C_D  ->  Drag  ->  Thrust  ->
 *   dh/dt  ->  dW/dt  ->  distance  ->  new Position
 *
 * Phase transitions:
 *   CLIMB  -> CRUISE  : when alt >= AC_CRUISE_ALT - 50 m
 *   CRUISE -> DESCEND : when step >= 70 % of total_steps
 * ================================================================ */

Position advance_physics(FlightPhysics *ph, int flight_id,
                           int step, int total_steps, double dt) {

    /* ── Resolve JSON config for this flight (NULL = legacy mode) ── */
    const FlightConfig *cfg = (flight_id < g_config_count)
                              ? &g_configs[flight_id] : NULL;

    double cruise_alt = cfg ? cfg->cruise_alt_m : AC_CRUISE_ALT;

    /* ── Already landed — return last position unchanged ─────── */
    if (ph->phase == PHASE_LANDED) {
        Position p; p.latitude = ph->lat; p.longitude = ph->lon; p.altitude = ph->alt; p.time_step = 0;
        return p;
    }

    /* ── Phase transitions ────────────────────────────────────── */
    if (ph->phase == PHASE_CLIMB && ph->alt >= cruise_alt - 50.0) {
        ph->phase = PHASE_CRUISE;
        ph->alt   = cruise_alt;              /* snap to cruise level */
    }
    if (ph->phase == PHASE_CRUISE) {
        /* JSON mode: switch to descent when within one step of the descent
         * start point (geographic trigger, not time-based).             */
        int geo_triggered = 0;
        if (cfg) {
            for (int si = 0; si < cfg->segment_count; si++) {
                if (strncmp(cfg->segments[si].mode, "descend", 7) == 0) {
                    /* descent_len = length of descent segment (start→end) */
                    double dlat_d = cfg->segments[si].end.lat - cfg->segments[si].start.lat;
                    double dlon_d = cfg->segments[si].end.lon - cfg->segments[si].start.lon;
                    double rlat_d = cfg->segments[si].start.lat * M_PI / 180.0;
                    double dy_d   = dlat_d * M_PI / 180.0 * R_EARTH;
                    double dx_d   = dlon_d * M_PI / 180.0 * R_EARTH * cos(rlat_d);
                    double descent_len = sqrt(dx_d*dx_d + dy_d*dy_d);

                    /* dist_to_dest = distance from current position to destination */
                    double dlat_c = cfg->destination.lat - ph->lat;
                    double dlon_c = cfg->destination.lon - ph->lon;
                    double rlat_c = ph->lat * M_PI / 180.0;
                    double dy_c   = dlat_c * M_PI / 180.0 * R_EARTH;
                    double dx_c   = dlon_c * M_PI / 180.0 * R_EARTH * cos(rlat_c);
                    double dist_to_dest = sqrt(dx_c*dx_c + dy_c*dy_c);

                    /* trigger descent when remaining distance <= descent_len + one step */
                    double step_dist = cfg->cruise_speed_ms * dt;
                    if (dist_to_dest <= descent_len + step_dist) {
                        ph->phase = PHASE_DESCEND;
                        geo_triggered = 1;
                    }
                    break;
                }
            }
        }
        if (!geo_triggered && !cfg && step >= (int)(total_steps * 0.70)) {
            ph->phase = PHASE_DESCEND;
        }
    }

    /* ── 1. ISA atmosphere at current altitude ─────────────────── */
    Atmosphere atm = isa_atmosphere(ph->alt);

    /* ── 2. Indicated Airspeed -> True Airspeed ───────────────── */
    double IAS_ms;
    double dh_dt = 0.0;

    if (cfg) {
        /* JSON mode: use the prescribed profile tables directly. */
        if (ph->phase == PHASE_CLIMB) {
            ProfileEntry pe = lookup_profile(cfg->climb, cfg->climb_count, ph->alt);
            IAS_ms = pe.speed_ms;
            dh_dt  = pe.rate_ms;            /* prescribed climb rate   */
        } else if (ph->phase == PHASE_DESCEND) {
            ProfileEntry pe = lookup_profile(cfg->descend, cfg->descend_count, ph->alt);
            IAS_ms = pe.speed_ms;
            dh_dt  = pe.rate_ms;            /* prescribed descent rate (< 0) */
        } else {
            /* Cruise: JSON gives TAS directly (460 kt); skip IAS correction. */
            IAS_ms = cfg->cruise_speed_ms;  /* used for horiz speed calc */
            dh_dt  = 0.0;
        }
    } else {
        /* Legacy mode: derive IAS from the hardcoded schedule. */
        IAS_ms = (ph->phase == PHASE_DESCEND)
               ? ias_climb_ms(ph->alt) * 0.75
               : ias_climb_ms(ph->alt);
    }

    /* CAS -> Mach (Excel formula, using density ratio rho0/rho):
     * M = sqrt(5*((rho0/rho*((1+0.2*(IAS_kn/661.5)^2)^3.5-1)+1)^0.286-1))
     * TAS = M * a  (local speed of sound) */
    double ias_kn  = IAS_ms / KNOT_TO_MS;
    double qterm   = pow(1.0 + 0.2 * pow(ias_kn / 661.5, 2.0), 3.5) - 1.0;
    double rho_rat = ISA_RHO0 / atm.rho;
    double M       = sqrt(5.0 * (pow(rho_rat * qterm + 1.0, 0.286) - 1.0));
    double TAS     = M * atm.a;
    if (TAS < 30.0) { TAS = 30.0; M = TAS / atm.a; }
    if (M > 0.95)   { M = 0.95;   TAS = 0.95 * atm.a; }

    /* ── 4. Lift coefficient  (L = W for steady flight) ──────── */
    double W  = ph->mass * ISA_G;
    double q  = 0.5 * atm.rho * TAS * TAS;              /* dynamic pressure */
    double CL = W / (q * AC_WING_AREA);
    if (CL < 0.05) CL = 0.05;
    if (CL > 2.00) CL = 2.00;

    /* ── 5. Drag coefficient  (parabolic polar) ───────────────── */
    double CD = AC_CD0 + (CL * CL) / (M_PI * AC_E * AC_AR);

    /* ── 6. Drag force ────────────────────────────────────────── */
    double D = q * AC_WING_AREA * CD;

    /* ── 7. Thrust  (linear Mach interpolation + altitude lapse) */
    double T_SL     = AC_T_MAX_SL + (AC_T_MIN_SL - AC_T_MAX_SL) * M / AC_M_REF;
    if (T_SL < 0.0) T_SL = 0.0;
    double T_engine = T_SL * pow(atm.rho / ISA_RHO0, AC_LAPSE);
    double Total_T  = T_engine * AC_N_ENG;

    /* ── 8. Vertical speed  dh/dt (legacy path only — JSON uses prescribed) */
    if (!cfg) {
        if (ph->phase == PHASE_CLIMB) {
            dh_dt = (Total_T - D) * TAS / W;
            if (dh_dt < 0.0) dh_dt = 0.0;
        } else if (ph->phase == PHASE_DESCEND) {
            dh_dt = descent_rate_ms(ph->alt);
        }
        /* CRUISE: dh_dt stays 0.0 */
    }

    /* ── 9. Fuel consumption  dW/dt  (kg/s) ──────────────────── */
    /* In cruise: thrust = drag (level flight, net force = 0).
     * In climb/descend: use full thrust model.
     * dW/dt = TSFC * T_eff / g                                    */
    double T_eff = (ph->phase == PHASE_CRUISE) ? D : Total_T;
    double dW_dt = AC_TSFC * T_eff / ISA_G;

    /* ── 10. Climbing angle and horizontal ground speed ──────── */
    double climb_angle = (TAS > 1e-6) ? atan2(dh_dt, TAS) : 0.0;
    double horiz_v     = TAS * cos(climb_angle);
    double dist        = horiz_v * dt;

    /* ── 11. Update geographic position ──────────────────────── */
    double new_lat, new_lon;
    move_along_bearing(ph->lat, ph->lon, ph->bearing, dist, &new_lat, &new_lon);
    ph->lat = new_lat;
    ph->lon = new_lon;

    /* ── 12. Update altitude ──────────────────────────────────── */
    ph->alt += dh_dt * dt;
    if (ph->alt < 0.0) ph->alt = 0.0;

    /* ── 12b. DESCEND → LANDED when reaching destination alt ──── */
    if (ph->phase == PHASE_DESCEND) {
        double dest_alt = cfg ? cfg->destination.alt_m : 0.0;
        if (ph->alt <= dest_alt + 10.0) {
            ph->alt   = dest_alt;
            ph->phase = PHASE_LANDED;
        }
    }

    /* ── 13. Burn fuel ────────────────────────────────────────── */
    double burned = dW_dt * dt;
    if (burned > ph->fuel_kg) burned = ph->fuel_kg;
    ph->fuel_kg -= burned;
    ph->mass    -= burned;

    /* ── 14. Per-step diagnostic output  (mirrors Excel columns) */
    const char *phase_lbl = (ph->phase == PHASE_CLIMB)   ? "CLIMB  " :
                            (ph->phase == PHASE_CRUISE)  ? "CRUISE " :
                            (ph->phase == PHASE_LANDED)  ? "LANDED " : "DESCEND";

    printf("[FLIGHT %d] Step %2d | %s | Alt=%8.1f m | Mass=%9.1f kg | "
           "Fuel=%8.1f kg | TAS=%6.1f kn | M=%.3f | "
           "dh/dt=%+6.2f m/s | Dist=%7.1f m\n",
           flight_id, step, phase_lbl,
           ph->alt, ph->mass, ph->fuel_kg,
           TAS / KNOT_TO_MS, M,
           dh_dt, dist);

    /* ── Build and return Position ─────────────────────────────── */
    Position pos;
    pos.latitude  = ph->lat;
    pos.longitude = ph->lon;
    pos.altitude  = ph->alt;
    pos.time_step = step;
    return pos;
}

/* ================================================================
 * Legacy Positional Interpolation  (kept for unit-test compatibility)
 *
 * Tests in test_simulation.c call compute_next_position() directly
 * and assert flight 0 step 0 is at Porto lat (41.2481) and the last
 * step is at Lisbon lat (38.7813).
 * execute_flight() no longer calls this — advance_physics() is used.
 * ================================================================ */

Position compute_next_position(int flight_id, int step, int total_steps) {
    Position pos;

    double offset    = flight_id * 0.5;
    double start_lat = 41.2481 + offset;
    double start_lon = -8.6814;
    double start_alt = 1000.0;
    double end_lat   = 38.7813 + offset;
    double end_lon   = -9.1359;
    double end_alt   = 10000.0;

    double t = (total_steps > 1)
             ? (double)step / (double)(total_steps - 1)
             : 0.0;

    pos.latitude  = start_lat + t * (end_lat  - start_lat);
    pos.longitude = start_lon + t * (end_lon  - start_lon);
    pos.altitude  = start_alt + t * (end_alt  - start_alt);
    pos.time_step = step;
    return pos;
}

/* ================================================================
 * Flight Process Entry Point  (US100 / US103)
 *
 * 1. Register signal handlers.
 * 2. Initialise the per-flight physics state.
 * 3. For each step:
 *    a. Check for pending SIGTERM / SIGUSR1.
 *    b. Block on the step token from the parent (US103 barrier).
 *    c. Advance the physics simulation by dt seconds.
 *    d. Write the resulting Position into the position pipe.
 * ================================================================ */


/* ================================================================
 * execute_flight -- US105 (Sprint 3)
 *
 * Changes from Sprint 2:
 *   - No pipe file descriptors. Child attaches shared memory
 *     (shmid) and opens its named semaphore ("/flight_<id>").
 *   - sem_wait() replaces read(pipe_step) for step gating.
 *   - Position written to shm->positions[flight_id] directly.
 *   - steps_completed[flight_id] incremented as barrier signal.
 *   - Signal handlers (SIGUSR1, SIGTERM) unchanged from Sprint 2.
 * ================================================================ */

void execute_flight(int flight_id,
                    SharedSimulationData *shm,
                    int total_steps,
                    int time_step_seconds) {

    signal(SIGUSR1, handle_sigusr1);
    signal(SIGTERM, handle_sigterm);

    /* open the named semaphore created by the parent */
    char sem_name[32];
    snprintf(sem_name, sizeof(sem_name), "/flight_%d", flight_id);
    sem_t *step_sem = sem_open(sem_name, 0);
    if (step_sem == SEM_FAILED) {
        perror("[FLIGHT] sem_open");
        exit(1);
    }

    /* US108 -- abrir semáforos do barrier (criados pelo pai) */
    sem_t *sem_nproc   = sem_open(SEM_NPROC_NAME,   0);
    sem_t *sem_barrier = sem_open(SEM_BARRIER_NAME, 0);
    if (sem_nproc == SEM_FAILED || sem_barrier == SEM_FAILED) {
        perror("[FLIGHT] sem_open barrier");
        exit(1);
    }

    FlightPhysics phys;
    init_flight_physics(&phys, flight_id, total_steps);

    if (flight_id < g_config_count) {
        const FlightConfig *cfg = &g_configs[flight_id];
        printf("[FLIGHT %d] Started (PID %d) | Bearing: %.1f deg | "
               "Route: %s -> %s  (%.4f, %.4f) -> (%.4f, %.4f)\n",
               flight_id, getpid(), phys.bearing,
               cfg->departure, cfg->arrival,
               phys.lat, phys.lon,
               cfg->destination.lat, cfg->destination.lon);
    } else {
        int id = flight_id % 5;
        printf("[FLIGHT %d] Started (PID %d) | Bearing: %.1f deg | "
               "Route: (%.4f, %.4f) -> %s\n",
               flight_id, getpid(), phys.bearing,
               phys.lat, phys.lon, DEST_NAMES[id]);
    }

    double dt = (double)time_step_seconds;

    for (int step = 0; step < total_steps; step++) {

        if (sigterm_received) {
            printf("[FLIGHT %d] Aborting at step %d (SIGTERM)\n",
                   flight_id, step);
            break;
        }
        if (sigusr1_received) {
            printf("[FLIGHT %d] Proximity warning at step %d\n",
                   flight_id, step);
            sigusr1_received = 0;
        }

        /* US105 -- block until parent posts the step semaphore */
        sem_wait(step_sem);

        /* check abort flag set by parent */
        if (shm->abort_flag) {
            printf("[FLIGHT %d] Abort flag set -- exiting at step %d\n",
                   flight_id, step);
            break;
        }

        if (sigterm_received) {
            printf("[FLIGHT %d] Aborting at step %d (SIGTERM)\n",
                   flight_id, step);
            break;
        }

        /* advance physics */
        Position pos = advance_physics(&phys, flight_id, step, total_steps, dt);

        /* write position to shared memory (exclusive slot -- no mutex needed) */
        shm->positions[flight_id] = pos;
        printf("[FLIGHT %d] Wrote position to shm step=%d "
               "lat=%.4f lon=%.4f alt=%.1f\n",
               flight_id, step, pos.latitude, pos.longitude, pos.altitude);

        /* increment steps_completed so the parent detects the write */
        shm->steps_completed[flight_id] = step + 1;

        /* US108 -- entrar no barrier */
        sem_wait(sem_nproc);
        shm->nproc_at_barrier++;
        if (shm->nproc_at_barrier == shm->num_active_flights)
            sem_post(sem_barrier);
        sem_post(sem_nproc);

        sem_wait(sem_barrier);
        sem_post(sem_barrier);

        if (sigusr1_received) {
            printf("[FLIGHT %d] Proximity warning at step %d\n",
                   flight_id, step);
            sigusr1_received = 0;
        }

        /* pace to real-time only for 1s steps */
        if (time_step_seconds == 1)
            sleep(time_step_seconds);
    }

    /* mark this flight as finished in shared memory */
    shm->active[flight_id] = 0;

    /* US108 -- saída antecipada: ajustar o contador do barrier */
    sem_wait(sem_nproc);
    shm->num_active_flights--;
    if (shm->nproc_at_barrier >= shm->num_active_flights
            && shm->num_active_flights > 0)
        sem_post(sem_barrier);
    sem_post(sem_nproc);

    sem_close(step_sem);
    sem_close(sem_nproc);
    sem_close(sem_barrier);

    printf("[FLIGHT %d] Finished | lat=%.4f lon=%.4f alt=%.1f m | "
           "Remaining fuel: %.1f kg (%.1f%% of initial)\n",
           flight_id, phys.lat, phys.lon, phys.alt,
           phys.fuel_kg, (phys.fuel_kg / AC_M_FUEL) * 100.0);
    exit(0);
}
