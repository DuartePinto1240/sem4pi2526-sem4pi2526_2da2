#include "weather.h"
#include <stdio.h>
#include <string.h>
#include <math.h>

/* Module-level dataset — loaded once at simulation start */
static WeatherDataset g_dataset;
static int            g_loaded = 0;

int weather_load_csv(const char *csv_path) {
    if (!csv_path) return -1;

    FILE *f = fopen(csv_path, "r");
    if (!f) {
        fprintf(stderr, "[ENV] Cannot open weather CSV: %s\n", csv_path);
        return -1;
    }

    g_dataset.count = 0;
    char line[512];

    /* skip header line */
    if (!fgets(line, sizeof(line), f)) {
        fclose(f);
        return -1;
    }

    while (fgets(line, sizeof(line), f)
           && g_dataset.count < MAX_WEATHER_ENTRIES) {

        WeatherEntry *e = &g_dataset.entries[g_dataset.count];

        /* CSV columns:
         * ACA, lat_max, lon_min, lat_min, lon_max,
         * alt_inf_ft, alt_sup_ft, direction_deg, speed_knots,
         * day, start, end */
        char day_str[32] = {0};
        char start_str[16] = {0};
        char end_str[16]   = {0};

        int parsed = sscanf(line,
            "%d,%lf,%lf,%lf,%lf,%lf,%lf,%lf,%lf,%31[^,],%15[^,],%15s",
            &e->aca_id,
            &e->lat_max, &e->lon_min,
            &e->lat_min, &e->lon_max,
            &e->alt_inf_ft, &e->alt_sup_ft,
            &e->direction_deg, &e->speed_knots,
            day_str, start_str, end_str);

        if (parsed >= 9)
            g_dataset.count++;
    }

    fclose(f);

    if (g_dataset.count > 0) {
        g_loaded = 1;
        printf("[ENV] Loaded %d weather entries from %s\n",
               g_dataset.count, csv_path);
        return 0;
    }

    fprintf(stderr, "[ENV] No valid entries in %s\n", csv_path);
    return -1;
}

int weather_lookup(double lat, double lon, double alt_ft, WindData *wind) {
    if (!wind) return 0;
    if (!g_loaded || g_dataset.count == 0) return 0;

    for (int i = 0; i < g_dataset.count; i++) {
        const WeatherEntry *e = &g_dataset.entries[i];

        if (lat <= e->lat_max && lat >= e->lat_min &&
            lon >= e->lon_min && lon <= e->lon_max &&
            alt_ft >= e->alt_inf_ft && alt_ft <= e->alt_sup_ft) {

            wind->speed_ms      = e->speed_knots * KNOTS_TO_MS;
            wind->direction_deg = e->direction_deg;

            /* Meteorological convention: direction = where wind COMES FROM.
             * 0° = North, 90° = East.
             * Convert to velocity components (where wind GOES TO). */
            const double rad = e->direction_deg * M_PI / 180.0;
            wind->vx = wind->speed_ms * sin(rad);  /* East  component */
            wind->vy = wind->speed_ms * cos(rad);  /* North component */

            return 1;
        }
    }

    return 0;
}