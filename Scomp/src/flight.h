#ifndef FLIGHT_H
#define FLIGHT_H

#include "simulation.h"
#include "json_loader.h"

/* ================================================================
 * ISA Standard Atmosphere Constants  (ISO 2533 / Excel simulator)
 * ================================================================ */
#define ISA_T0          288.2
#define ISA_LAMBDA      (-6.5e-3)
#define ISA_RHO0        1.225
#define ISA_P0          101325.0
#define ISA_A0          340.3
#define ISA_R           287.0
#define ISA_G           9.81
#define ISA_GAMMA       1.4
#define ISA_TROPO_H     11000.0
#define ISA_T_TROPO     216.7

/* ================================================================
 * Aircraft Parameters  (A380-like)
 * ================================================================ */
#define AC_WING_AREA    858.0
#define AC_AR           9.0
#define AC_E            0.84
#define AC_CD0          0.02
#define AC_T_MAX_SL     338000.0
#define AC_T_MIN_SL     180000.0
#define AC_M_REF        0.9
#define AC_LAPSE        0.96
#define AC_TSFC         1.6e-4
#define AC_N_ENG        4
#define AC_M_EMPTY      276800.0
#define AC_M_PAYLOAD    42800.0
#define AC_M_FUEL       40200.0
#define AC_CRUISE_ALT   9249.0

/* ================================================================
 * Navigation
 * ================================================================ */
#define R_EARTH         6371000.0
#define KNOT_TO_MS      0.514444

/* ================================================================
 * Flight Phase
 * ================================================================ */
typedef enum {
    PHASE_CLIMB   = 0,
    PHASE_CRUISE  = 1,
    PHASE_DESCEND = 2,
    PHASE_LANDED  = 3
} FlightPhase;

/* ================================================================
 * Atmospheric Conditions at a Given Altitude
 * ================================================================ */
typedef struct {
    double T;   /* temperature,    K     */
    double rho; /* air density,    kg/m3 */
    double p;   /* pressure,       Pa    */
    double a;   /* speed of sound, m/s   */
} Atmosphere;

/* ================================================================
 * Per-Flight Physics State
 * ================================================================ */
typedef struct {
    double      lat;
    double      lon;
    double      alt;
    double      mass;
    double      fuel_kg;
    double      bearing;
    FlightPhase phase;
} FlightPhysics;

/* ================================================================
 * Function Declarations
 * ================================================================ */

void set_flight_configs(const FlightConfig *configs, int count);

Atmosphere isa_atmosphere(double h);

double ias_climb_ms(double h);

double descent_rate_ms(double h);

double compute_bearing_deg(double lat1, double lon1,
                            double lat2, double lon2);

void move_along_bearing(double lat_deg, double lon_deg,
                        double bearing_deg, double dist_m,
                        double *out_lat, double *out_lon);

void init_flight_physics(FlightPhysics *ph, int flight_id, int total_steps);

Position advance_physics(FlightPhysics *ph, int flight_id,
                          int step, int total_steps, double dt);

Position compute_next_position(int flight_id, int step, int total_steps);

void handle_sigusr1(int sig);

void handle_sigterm(int sig);

/* Entry point for a child flight process (US105).
 * shmid -- shared memory segment ID; child calls shmat(shmid).
 * Step semaphore name derived as "/flight_<flight_id>". */
void execute_flight(int flight_id,
                    int shmid,
                    int total_steps,
                    int time_step_seconds);

#endif /* FLIGHT_H */
