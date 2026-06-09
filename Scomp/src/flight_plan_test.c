/*
 * flight_plan_test.c — US085 Flight Plan Feasibility Checker
 *
 * Validates a flight plan against physical constraints not covered by the
 * ANTLR DSL validator (Phase 1). Called by TestFlightPlanController via
 * ProcessBuilder.
 *
 * Usage:
 *   flight_plan_test <num_legs>
 *       <leg_fuel> <leg_num_segs>
 *           <fromLat> <fromLon> <toLat> <toLon> <minAlt> <maxAlt> <windSpeed>
 *           ... (repeated for each segment)
 *       ... (repeated for each leg)
 *
 * Validations performed:
 *   1. Fuel sufficiency  — declared fuel >= estimated consumption (Haversine
 *                          distance × FUEL_RATE_KG_PER_KM) + reserve
 *   2. Altitude range    — all altitude slots in [MIN_ALT_FT, MAX_ALT_FT]
 *   3. Wind safety       — wind speed <= MAX_WIND_MS per segment
 *
 * Output:
 *   PASS                                    (exit 0)
 *   FAIL\n<one error line per violation>    (exit 1)
 *   usage error message on stderr           (exit 2)
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

/* ── Constants ─────────────────────────────────────────────────────────── */

#define FUEL_RATE_KG_PER_KM  2.5      /* estimated fuel consumption per km    */
#define FUEL_RESERVE_KG      200.0    /* mandatory reserve fuel per leg       */
#define MIN_ALT_FT           500.0    /* minimum safe cruise altitude (ft)    */
#define MAX_ALT_FT           45000.0  /* commercial service ceiling (ft)      */
#define MAX_WIND_MS          50.0     /* max safe wind speed (m/s ≈ 97 knots) */
#define EARTH_RADIUS_KM      6371.0
#define MAX_LEGS             64
#define MAX_SEGS_PER_LEG     64
#define MAX_ERRORS           128

/* ── Haversine distance (km) ────────────────────────────────────────────── */

static double deg2rad(double deg) { return deg * M_PI / 180.0; }

static double haversine_km(double lat1, double lon1,
                            double lat2, double lon2) {
    double dlat = deg2rad(lat2 - lat1);
    double dlon = deg2rad(lon2 - lon1);
    double a = sin(dlat / 2) * sin(dlat / 2)
             + cos(deg2rad(lat1)) * cos(deg2rad(lat2))
             * sin(dlon / 2) * sin(dlon / 2);
    double c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a));
    return EARTH_RADIUS_KM * c;
}

/* ── Main ───────────────────────────────────────────────────────────────── */

int main(int argc, char *argv[]) {

    if (argc < 2) {
        fprintf(stderr, "Usage: flight_plan_test <num_legs> ...\n");
        return 2;
    }

    int num_legs = atoi(argv[1]);
    if (num_legs <= 0 || num_legs > MAX_LEGS) {
        fprintf(stderr, "Invalid number of legs: %d\n", num_legs);
        return 2;
    }

    char  errors[MAX_ERRORS][256];
    int   error_count = 0;
    int   arg = 2;   /* current position in argv */

    for (int leg = 0; leg < num_legs; leg++) {

        if (arg + 1 >= argc) {
            fprintf(stderr, "Not enough arguments for leg %d\n", leg + 1);
            return 2;
        }

        double fuel     = atof(argv[arg++]);
        int    num_segs = atoi(argv[arg++]);

        if (num_segs <= 0 || num_segs > MAX_SEGS_PER_LEG) {
            fprintf(stderr, "Invalid segment count %d for leg %d\n",
                    num_segs, leg + 1);
            return 2;
        }

        /* ── Fuel estimation (Haversine) ── */
        double total_dist_km  = 0.0;

        for (int s = 0; s < num_segs; s++) {
            if (arg + 6 >= argc) {
                fprintf(stderr, "Not enough arguments for segment %d of leg %d\n",
                        s + 1, leg + 1);
                return 2;
            }

            double from_lat   = atof(argv[arg++]);
            double from_lon   = atof(argv[arg++]);
            double to_lat     = atof(argv[arg++]);
            double to_lon     = atof(argv[arg++]);
            double min_alt    = atof(argv[arg++]);
            double max_alt    = atof(argv[arg++]);
            double wind_speed = atof(argv[arg++]);

            /* Distance */
            double dist = haversine_km(from_lat, from_lon, to_lat, to_lon);
            total_dist_km += dist;

            /* Validation 2 — Altitude range */
            if (min_alt < MIN_ALT_FT) {
                snprintf(errors[error_count++], 256,
                    "Leg %d, Seg %d: minimum altitude %.0f ft is below safe "
                    "minimum %.0f ft",
                    leg + 1, s + 1, min_alt, MIN_ALT_FT);
            }
            if (max_alt > MAX_ALT_FT) {
                snprintf(errors[error_count++], 256,
                    "Leg %d, Seg %d: maximum altitude %.0f ft exceeds service "
                    "ceiling %.0f ft",
                    leg + 1, s + 1, max_alt, MAX_ALT_FT);
            }

            /* Validation 3 — Wind speed */
            if (wind_speed > MAX_WIND_MS) {
                snprintf(errors[error_count++], 256,
                    "Leg %d, Seg %d: wind speed %.1f m/s exceeds safety limit "
                    "%.0f m/s",
                    leg + 1, s + 1, wind_speed, MAX_WIND_MS);
            }

            if (error_count >= MAX_ERRORS) break;
        }

        /* Validation 1 — Fuel sufficiency */
        double min_fuel = total_dist_km * FUEL_RATE_KG_PER_KM + FUEL_RESERVE_KG;
        if (fuel < min_fuel) {
            snprintf(errors[error_count++], 256,
                "Leg %d: declared fuel %.1f kg is below estimated minimum "
                "%.1f kg (%.0f km x %.1f kg/km + %.0f kg reserve)",
                leg + 1, fuel, min_fuel,
                total_dist_km, FUEL_RATE_KG_PER_KM, FUEL_RESERVE_KG);
        }

        if (error_count >= MAX_ERRORS) break;
    }

    if (error_count == 0) {
        printf("PASS\n");
        return 0;
    }

    printf("FAIL\n");
    for (int i = 0; i < error_count; i++) {
        printf("%s\n", errors[i]);
    }
    return 1;
}
