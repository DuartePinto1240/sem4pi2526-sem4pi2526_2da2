#ifndef JSON_LOADER_H
#define JSON_LOADER_H

#include "simulation.h"   /* SimulationConfig, MAX_FLIGHTS */

/* ================================================================
 * JSON Flight Config Loader
 *
 * Two JSON formats are supported:
 *
 * 1. Single-flight plan  (original format):
 *      { "ID":…, "Type":…, "Route":…, "Leg":[{…}] }
 *    → load_flight_from_json()
 *
 * 2. Simulation scenario  (all-in-one):
 *      { "SimulationConfig":{ "TotalSteps":…, "TimeStepSeconds":…,
 *                             "SafetyThresholdM":…, "MaxViolations":… },
 *        "Flights":[ {…flight plan 1…}, {…flight plan 2…}, … ] }
 *    → load_scenario_from_json()
 *    Usage: ./simulation scenario.json
 * ================================================================ */

#define MAX_PROFILE_ENTRIES 20
#define MAX_SEGMENTS        10

/* ── Speed / rate entry from Climb or Descend table ── */
typedef struct {
    double altitude_m;   /* breakpoint altitude, metres               */
    double speed_ms;     /* IAS in m/s (converted from knots on load) */
    double rate_ms;      /* climb rate (>0) or descent rate (<0), m/s */
} ProfileEntry;

/* ── Geographic waypoint ── */
typedef struct {
    double lat;
    double lon;
    double alt_m;
} GeoPoint;

/* ── One segment (climb / cruise / descend) ── */
typedef struct {
    char     mode[16];   /* "climb", "cruise", "descend"              */
    GeoPoint start;
    GeoPoint end;
} FlightSegment;

/* ── Full per-flight configuration loaded from JSON ── */
typedef struct {
    int    json_id;
    char   type[32];
    char   route[32];
    char   departure_time[16];
    char   departure[8];
    char   arrival[8];
    double fuel_kg;

    ProfileEntry climb[MAX_PROFILE_ENTRIES];
    int          climb_count;

    ProfileEntry descend[MAX_PROFILE_ENTRIES];
    int          descend_count;

    double cruise_speed_ms;   /* TAS in m/s */

    FlightSegment segments[MAX_SEGMENTS];
    int           segment_count;

    GeoPoint origin;
    GeoPoint destination;
    double   cruise_alt_m;
} FlightConfig;

/* ── Load a single flight plan from a JSON file ── */
int  load_flight_from_json(const char *filename, FlightConfig *cfg);

/* ── Load a complete scenario (SimulationConfig + all Flights) ──
 * Returns 0 on success, -1 on failure.
 * flight_count is set to the number of flights loaded. */
int  load_scenario_from_json(const char *filename,
                              SimulationConfig *sim_cfg,
                              FlightConfig     *flights,
                              int              *flight_count);

/* ── Print a one-line summary of the loaded flight config ── */
void print_flight_config(const FlightConfig *cfg);

#endif /* JSON_LOADER_H */
