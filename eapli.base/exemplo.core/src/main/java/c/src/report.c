#include "report.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

/* ─────────────────────────────────────────
 * Filename generation
 * ───────────────────────────────────────── */

void make_report_filename(char *buf, size_t size) {
    time_t now = time(NULL);
    struct tm *t = localtime(&now);
    snprintf(buf, size,
             "simulation_report_%04d%02d%02d_%02d%02d%02d.txt",
             t->tm_year + 1900,
             t->tm_mon  + 1,
             t->tm_mday,
             t->tm_hour,
             t->tm_min,
             t->tm_sec);
}

/* ─────────────────────────────────────────
 * Report writing — US109
 * ───────────────────────────────────────── */

void write_report(const SimulationReport *report,
                  const char *filename) {
    FILE *f = fopen(filename, "w");
    if (!f) {
        perror("[REPORT] Cannot open report file");
        return;
    }

    time_t now = time(NULL);

    fprintf(f, "========================================\n");
    fprintf(f, "  AISafe Simulation Report\n");
    fprintf(f, "  Generated: %s", ctime(&now));
    fprintf(f, "========================================\n\n");

    /* Summary */
    fprintf(f, "--- Summary ---\n");
    fprintf(f, "Total flights    : %d\n", report->total_flights);
    fprintf(f, "Total violations : %d\n", report->total_violations);
    fprintf(f, "Result           : %s\n\n",
            report->pass ? "PASS" : "FAIL");

    /* Flight statuses */
    fprintf(f, "--- Flight Statuses ---\n");
    for (int i = 0; i < report->total_flights; i++) {
        const char *status;
        switch (report->flight_statuses[i]) {
            case 0:  status = "OK";       break;
            case 1:  status = "VIOLATED"; break;
            case 2:  status = "ABORTED";  break;
            default: status = "UNKNOWN";  break;
        }
        fprintf(f, "Flight %d : %s\n", i, status);
    }

    /* Violation events */
    fprintf(f, "\n--- Safety Violation Events ---\n");
    int max = report->total_violations < MAX_VIOLATIONS
              ? report->total_violations : MAX_VIOLATIONS;

    if (max == 0) {
        fprintf(f, "No violations recorded.\n");
    } else {
        for (int i = 0; i < max; i++) {
            const ViolationEvent *v = &report->violations[i];
            fprintf(f, "\nViolation #%d\n",       i + 1);
            fprintf(f, "  Timestamp : %.2f s\n",  v->timestamp);
            fprintf(f, "  Flight 1  : %d\n",       v->flight_id_1);
            fprintf(f, "    Lat=%.4f Lon=%.4f Alt=%.1f\n",
                    v->position_1.latitude,
                    v->position_1.longitude,
                    v->position_1.altitude);
            fprintf(f, "    Velocity : vx=%.4f vy=%.4f vz=%.4f\n",
                    v->velocity_1.vx,
                    v->velocity_1.vy,
                    v->velocity_1.vz);
            fprintf(f, "  Flight 2  : %d\n",       v->flight_id_2);
            fprintf(f, "    Lat=%.4f Lon=%.4f Alt=%.1f\n",
                    v->position_2.latitude,
                    v->position_2.longitude,
                    v->position_2.altitude);
            fprintf(f, "    Velocity : vx=%.4f vy=%.4f vz=%.4f\n",
                    v->velocity_2.vx,
                    v->velocity_2.vy,
                    v->velocity_2.vz);
        }
    }

    fprintf(f, "\n========================================\n");
    fprintf(f, "  End of Report\n");
    fprintf(f, "========================================\n");

    fclose(f);
    printf("[REPORT] Report saved to: %s\n", filename);
}

/* ─────────────────────────────────────────
 * Console summary
 * ───────────────────────────────────────── */

void print_report_summary(const SimulationReport *report) {
    printf("\n========================================\n");
    printf("  Simulation Complete\n");
    printf("========================================\n");
    printf("  Total flights    : %d\n", report->total_flights);
    printf("  Total violations : %d\n", report->total_violations);
    printf("  Result           : %s\n",
           report->pass ? "PASS" : "FAIL");
    printf("========================================\n\n");
}