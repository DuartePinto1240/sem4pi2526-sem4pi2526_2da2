#ifndef REPORT_H
#define REPORT_H

#include "simulation.h"

/* Generate a timestamped report filename.
 * buf must be at least 64 bytes. */
void make_report_filename(char *buf, size_t size);

/* Write the complete simulation report to a file.
 * Includes summary, flight statuses, violation events
 * with timestamps, positions and velocity vectors. */
void write_report(const SimulationReport *report, const char *filename);

/* Print a summary of the report to stdout. */
void print_report_summary(const SimulationReport *report);

#endif