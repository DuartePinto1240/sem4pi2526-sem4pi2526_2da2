/*
 * flight_plan_test.c — US085 Flight Plan Feasibility Checker
 *
 * Validates a flight plan against physical constraints not covered by the
 * ANTLR DSL validator (Phase 1). Called by TestFlightPlanController via
 * ProcessBuilder.
 *
 * Usage:
 *   flight_plan_test <num_legs> <max_fuel_capacity> <service_ceiling> <cruise_speed_kmh>
 *       <leg_fuel> <leg_num_segs>
 *           <fromLat> <fromLon> <toLat> <toLon> <minAlt> <maxAlt> <windAngle> <windSpeed> <phase>
 *           ... (repeated for each segment)
 *       ... (repeated for each leg)
 *
 * Parameters from aircraft model:
 *   max_fuel_capacity  — aircraft maximum fuel capacity (kg)
 *   service_ceiling    — aircraft service ceiling (ft)
 *   cruise_speed_kmh   — aircraft cruise speed (km/h), used for wind correction
 *
 * Per-segment parameters:
 *   windAngle — meteorological wind direction (degrees FROM which wind blows, 0=N, 90=E)
 *   windSpeed — wind speed (m/s)
 *   phase     — flight phase: "climb", "cruise", or "descent"
 *
 * Fuel consumption model:
 *   Base rate (cruise): FUEL_RATE_CRUISE_KG_PER_KM  (2.5 kg/km)
 *   Phase multipliers : climb x1.6, cruise x1.0, descent x0.35
 *   Wind correction   : effective_gs = cruise_speed - headwind_component (km/h)
 *                       fuel_rate_adj = base_rate x phase_mult x (cruise_speed / gs)
 *
 * Validations:
 *   1. Fuel capacity    — total declared fuel <= max_fuel_capacity
 *   2. Fuel sufficiency — declared fuel per leg >= estimated burn + reserve
 *   3. Altitude range   — max_alt <= service_ceiling
 *   4. Wind safety      — wind speed <= MAX_WIND_MS per segment
 *
 * Output:
 *   PASS\n<total_dist_km> <total_fuel_kg> <fuel_burn_kg>  (exit 0)
 *   FAIL\n<one error line per violation>                  (exit 1)
 *   usage error on stderr                                 (exit 2)
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

/* ── Constants ─────────────────────────────────────────────────────────── */

#define FUEL_RATE_CRUISE_KG_PER_KM   2.5    /* base fuel rate at cruise (kg/km)    */
#define FUEL_RATE_CLIMB_MULT         1.6    /* climb burns 1.6x cruise rate        */
#define FUEL_RATE_DESCENT_MULT       0.35   /* descent burns 0.35x cruise rate     */
#define FUEL_RESERVE_KG              200.0  /* mandatory reserve per leg (kg)      */
#define MAX_WIND_MS                  30.0   /* maximum safe wind speed (~58 kt)    */
#define EARTH_RADIUS_KM              6371.0
#define MAX_LEGS                     64
#define MAX_SEGS_PER_LEG             64
#define MAX_ERRORS                   128

/* ── Geometry helpers ───────────────────────────────────────────────────── */

static double deg2rad(double deg) { return deg * M_PI / 180.0; }

/* Great-circle distance (km) — Haversine */
static double haversine_km(double lat1, double lon1,
                            double lat2, double lon2) {
    double dlat = deg2rad(lat2 - lat1);
    double dlon = deg2rad(lon2 - lon1);
    double a = sin(dlat / 2) * sin(dlat / 2)
             + cos(deg2rad(lat1)) * cos(deg2rad(lat2))
             * sin(dlon / 2) * sin(dlon / 2);
    return EARTH_RADIUS_KM * 2.0 * atan2(sqrt(a), sqrt(1.0 - a));
}

/* True bearing from point 1 to point 2 (degrees, 0=N clockwise) */
static double bearing_deg(double lat1, double lon1,
                           double lat2, double lon2) {
    double rlat1 = deg2rad(lat1), rlat2 = deg2rad(lat2);
    double dlon  = deg2rad(lon2 - lon1);
    double x = sin(dlon) * cos(rlat2);
    double y = cos(rlat1) * sin(rlat2) - sin(rlat1) * cos(rlat2) * cos(dlon);
    return fmod(atan2(x, y) * 180.0 / M_PI + 360.0, 360.0);
}

/*
 * Headwind component (m/s) along the flight path.
 *   wind_from_deg    : meteorological direction FROM which wind blows
 *   wind_speed_ms    : wind speed (m/s)
 *   flight_bearing   : direction of travel (degrees)
 * Returns: positive = headwind (opposing), negative = tailwind (assisting)
 */
static double headwind_ms(double wind_from_deg, double wind_speed_ms,
                           double flight_bearing) {
    double wind_to = fmod(wind_from_deg + 180.0, 360.0);
    double angle   = deg2rad(wind_to - flight_bearing);
    return wind_speed_ms * cos(angle);
}

/* Phase multiplier for fuel consumption */
static double phase_multiplier(const char *phase) {
    if (strcmp(phase, "climb")   == 0) return FUEL_RATE_CLIMB_MULT;
    if (strcmp(phase, "descent") == 0) return FUEL_RATE_DESCENT_MULT;
    return 1.0; /* cruise (default) */
}

/* ── Main ───────────────────────────────────────────────────────────────── */

int main(int argc, char *argv[]) {

    if (argc < 5) {
        fprintf(stderr,
            "Usage: flight_plan_test <num_legs> <max_fuel_capacity> "
            "<service_ceiling> <cruise_speed_kmh> ...\n");
        return 2;
    }

    int    num_legs          = atoi(argv[1]);
    double max_fuel_capacity = atof(argv[2]);
    double service_ceiling   = atof(argv[3]);
    double cruise_speed_kmh  = atof(argv[4]);

    if (num_legs <= 0 || num_legs > MAX_LEGS) {
        fprintf(stderr, "Invalid number of legs: %d\n", num_legs);
        return 2;
    }
    if (max_fuel_capacity <= 0) {
        fprintf(stderr, "Invalid max fuel capacity: %.1f\n", max_fuel_capacity);
        return 2;
    }
    if (service_ceiling <= 0) {
        fprintf(stderr, "Invalid service ceiling: %.1f\n", service_ceiling);
        return 2;
    }
    if (cruise_speed_kmh <= 0) cruise_speed_kmh = 800.0;

    char   errors[MAX_ERRORS][256];
    int    error_count         = 0;
    int    arg                 = 5;
    double grand_total_dist_km = 0.0;
    double grand_total_fuel_kg = 0.0;
    double grand_total_burn_kg = 0.0;

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

        double total_dist_km = 0.0;
        double total_burn_kg = 0.0;

        for (int s = 0; s < num_segs; s++) {
            /* 9 args per segment: fromLat fromLon toLat toLon minAlt maxAlt windAngle windSpeed phase */
            if (arg + 8 >= argc) {
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
            double wind_angle = atof(argv[arg++]);
            double wind_speed = atof(argv[arg++]);
            const char *phase = argv[arg++];

            /* Distance */
            double dist = haversine_km(from_lat, from_lon, to_lat, to_lon);
            total_dist_km += dist;

            /* ── Validation 3: altitude vs service ceiling ── */
            if (max_alt > service_ceiling) {
                if (error_count < MAX_ERRORS)
                    snprintf(errors[error_count++], 256,
                        "Leg %d, Seg %d: altitude maxima %.0f ft excede o teto de "
                        "servico da aeronave %.0f ft",
                        leg + 1, s + 1, max_alt, service_ceiling);
            }

            /* ── Validation 4: wind safety ── */
            if (wind_speed > MAX_WIND_MS) {
                if (error_count < MAX_ERRORS)
                    snprintf(errors[error_count++], 256,
                        "Leg %d, Seg %d: vento %.1f m/s excede o limite de seguranca "
                        "%.0f m/s (%.0f kt)",
                        leg + 1, s + 1, wind_speed, MAX_WIND_MS,
                        MAX_WIND_MS * 1.944);
            }

            /* ── Fuel burn ── */
            /* Phase multiplier */
            double p_mult = phase_multiplier(phase);

            /* Wind correction: bearing → headwind component → ground speed */
            double bearing = bearing_deg(from_lat, from_lon, to_lat, to_lon);
            double hw_ms   = headwind_ms(wind_angle, wind_speed, bearing);
            double hw_kmh  = hw_ms * 3.6;
            double gs      = cruise_speed_kmh - hw_kmh;
            if (gs < cruise_speed_kmh * 0.10) gs = cruise_speed_kmh * 0.10;

            double wind_factor = cruise_speed_kmh / gs;
            double rate        = FUEL_RATE_CRUISE_KG_PER_KM * p_mult * wind_factor;
            total_burn_kg     += dist * rate;
        }

        /* ── Validation 2: fuel sufficiency per leg ── */
        double min_fuel = total_burn_kg + FUEL_RESERVE_KG;
        if (fuel < min_fuel) {
            if (error_count < MAX_ERRORS)
                snprintf(errors[error_count++], 256,
                    "Leg %d: combustivel declarado %.1f kg abaixo do minimo estimado "
                    "%.1f kg (consumo %.1f kg + reserva %.0f kg)",
                    leg + 1, fuel, min_fuel, total_burn_kg, FUEL_RESERVE_KG);
        }

        grand_total_dist_km += total_dist_km;
        grand_total_fuel_kg += fuel;
        grand_total_burn_kg += total_burn_kg;
    }

    /* ── Validation 1: total fuel vs aircraft capacity ── */
    if (grand_total_fuel_kg > max_fuel_capacity) {
        if (error_count < MAX_ERRORS)
            snprintf(errors[error_count++], 256,
                "Combustivel total declarado %.1f kg excede a capacidade maxima "
                "da aeronave %.1f kg",
                grand_total_fuel_kg, max_fuel_capacity);
    }

    if (error_count == 0) {
        printf("PASS %.2f %.2f %.2f\n",
               grand_total_dist_km,
               grand_total_fuel_kg,
               grand_total_burn_kg);
        return 0;
    }

    printf("FAIL\n");
    for (int i = 0; i < error_count; i++) {
        printf("%s\n", errors[i]);
    }
    return 1;
}
