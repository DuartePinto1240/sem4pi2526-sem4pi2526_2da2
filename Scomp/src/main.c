#include "simulation.h"
#include "flight.h"
#include "report.h"
#include "json_loader.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* ─────────────────────────────────────────────────────────────
 * Usage
 * ───────────────────────────────────────────────────────────── */

static void print_usage(const char *prog) {
    fprintf(stderr,
        "Modo cenário  (recomendado — tudo num JSON):\n"
        "  %s cenario.json\n"
        "\n"
        "Modo multi-voo  (parâmetros + JSONs individuais):\n"
        "  %s <steps> <timestep_s> <threshold_m> <max_viol> voo1.json [voo2.json ...]\n"
        "\n"
        "Modo legado  (sem JSON):\n"
        "  %s <num_flights> <steps> <timestep_s> <threshold_m> <max_viol>\n"
        "\n"
        "Exemplos:\n"
        "  %s scenarios/single/OPO-MAD.json\n"
        "  %s 10 1 5000 3 flights/OPO-MAD.json flights/LIS-BCN.json\n"
        "  %s 3 10 1 50000 5\n",
        prog, prog, prog, prog, prog, prog);
}

static int ends_with_json(const char *s) {
    size_t n = strlen(s);
    return n > 5 && strcmp(s + n - 5, ".json") == 0;
}

/* ─────────────────────────────────────────────────────────────
 * Entry point
 * ───────────────────────────────────────────────────────────── */

int main(int argc, char *argv[]) {

    if (argc < 2) { print_usage(argv[0]); return EXIT_FAILURE; }

    SimulationConfig config;
    memset(&config, 0, sizeof(config));

    FlightConfig json_configs[MAX_FLIGHTS];
    int          json_count = 0;

    /* ── Detect mode ──────────────────────────────────────────── */
    int scenario_mode  = (argc == 2 && ends_with_json(argv[1]));
    int json_mode      = 0;
    if (!scenario_mode)
        for (int i = 1; i < argc; i++)
            if (ends_with_json(argv[i])) { json_mode = 1; break; }

    /* ── Parse ──────────────────────────────────────────────────── */
    if (scenario_mode) {
        /* ── Cenário: um único JSON contém tudo ── */
        if (load_scenario_from_json(argv[1], &config, json_configs, &json_count) != 0) {
            fprintf(stderr, "[ERROR] Falha ao carregar cenário '%s'\n", argv[1]);
            return EXIT_FAILURE;
        }
        config.num_flights = json_count;
        set_flight_configs(json_configs, json_count);

    } else if (json_mode) {
        /* ── Multi-voo: parâmetros + JSON individuais ── */
        if (argc < 6) { print_usage(argv[0]); return EXIT_FAILURE; }
        config.total_steps        = atoi(argv[1]);
        config.time_step_seconds  = atoi(argv[2]);
        config.safety_threshold_m = atof(argv[3]);
        config.max_violations     = atoi(argv[4]);
        for (int i = 5; i < argc && json_count < MAX_FLIGHTS; i++) {
            if (!ends_with_json(argv[i])) continue;
            if (load_flight_from_json(argv[i], &json_configs[json_count]) != 0) {
                fprintf(stderr, "[ERROR] Falha ao carregar '%s'\n", argv[i]);
                return EXIT_FAILURE;
            }
            json_count++;
        }
        if (json_count == 0) {
            fprintf(stderr, "[ERROR] Nenhum ficheiro JSON válido.\n");
            return EXIT_FAILURE;
        }
        config.num_flights = json_count;
        set_flight_configs(json_configs, json_count);

    } else {
        /* ── Legado: 5 números ── */
        if (argc != 6) { print_usage(argv[0]); return EXIT_FAILURE; }
        config.num_flights        = atoi(argv[1]);
        config.total_steps        = atoi(argv[2]);
        config.time_step_seconds  = atoi(argv[3]);
        config.safety_threshold_m = atof(argv[4]);
        config.max_violations     = atoi(argv[5]);
    }

    /* ── Banner ── */
    const char *mode_str = scenario_mode ? "Cenário JSON" : (json_mode ? "Multi-voo JSON" : "Legado");
    printf("========================================\n");
    printf("  AISafe Flight Simulation\n");
    printf("========================================\n");
    printf("  Modo           : %s\n", mode_str);
    printf("  Voos           : %d\n", config.num_flights);
    printf("  Steps          : %d\n", config.total_steps);
    printf("  Time step      : %d s\n", config.time_step_seconds);
    printf("  Threshold      : %.0f m\n", config.safety_threshold_m);
    printf("  Max violações  : %d\n", config.max_violations);

    if (scenario_mode || json_mode) {
        printf("  ----------------------------------------\n");
        for (int i = 0; i < json_count; i++) {
            printf("  [%d] %s → %s  (%s)  %.0f kg combustível\n",
                   i, json_configs[i].departure, json_configs[i].arrival,
                   json_configs[i].route, json_configs[i].fuel_kg);
        }
    }
    printf("========================================\n\n");

    /* ── Validar ── */
    if (validate_parameters(&config) != 0) {
        fprintf(stderr, "[ERROR] Parâmetros inválidos.\n");
        return EXIT_FAILURE;
    }

    /* ── Simular ── */
    FlightState    states[MAX_FLIGHTS];
    SimulationReport report;
    memset(states,  0, sizeof(states));
    memset(&report, 0, sizeof(report));

    printf("[MAIN] A iniciar simulação...\n\n");
    if (start_simulation(&config, states, &report) != 0) {
        fprintf(stderr, "[ERROR] Simulação falhou.\n");
        return EXIT_FAILURE;
    }

    /* ── Relatório ── */
    char filename[128];
    make_report_filename(filename, sizeof(filename));
    print_report_summary(&report);
    write_report(&report, filename);

    return report.pass ? EXIT_SUCCESS : EXIT_FAILURE;
}
