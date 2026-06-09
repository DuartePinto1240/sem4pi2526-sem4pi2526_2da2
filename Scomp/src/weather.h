#ifndef WEATHER_H
#define WEATHER_H

#include "simulation.h"

#define MAX_WEATHER_ENTRIES 100
#define KNOTS_TO_MS         0.514444   /* 1 knot = 0.514444 m/s */
#define METERS_TO_FEET      3.28084

/* One wind entry parsed from the CSV */
typedef struct {
    int    aca_id;
    double lat_max;
    double lon_min;
    double lat_min;
    double lon_max;
    double alt_inf_ft;
    double alt_sup_ft;
    double direction_deg;
    double speed_knots;
} WeatherEntry;

/* Loaded weather dataset */
typedef struct {
    WeatherEntry entries[MAX_WEATHER_ENTRIES];
    int          count;
} WeatherDataset;

/* Load weather data from a CSV file.
 * Returns 0 on success, -1 on error. */
int weather_load_csv(const char *csv_path);

/* Find the wind for a given position and altitude.
 * lat/lon in degrees, alt_ft in feet.
 * Fills wind and returns 1 if a match was found, 0 otherwise. */
int weather_lookup(double lat, double lon, double alt_ft, WindData *wind);

#endif /* WEATHER_H */