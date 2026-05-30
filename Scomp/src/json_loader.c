#include "json_loader.h"
#include "flight.h"    /* KNOT_TO_MS, AC_M_FUEL, AC_CRUISE_ALT */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

/* ================================================================
 * Minimal recursive-descent JSON tree
 *
 * Supports: null · bool · number · string · array · object.
 * Object members are kept as a linked list; arrays likewise.
 * Keys with spaces (e.g. "Flight Profile") are handled naturally
 * because we parse arbitrary quoted strings as keys.
 * ================================================================ */

typedef enum {
    JT_NULL, JT_BOOL, JT_NUMBER, JT_STRING, JT_ARRAY, JT_OBJECT
} JType;

typedef struct JNode {
    JType        type;
    char        *key;       /* set when this node is an object member  */
    double       dval;      /* JT_NUMBER                               */
    char        *sval;      /* JT_STRING                               */
    int          bval;      /* JT_BOOL                                 */
    struct JNode *child;    /* first child  (object / array)           */
    struct JNode *next;     /* next sibling                            */
} JNode;

/* ── Free a whole subtree ── */
static void jfree(JNode *n) {
    if (!n) return;
    jfree(n->child);
    jfree(n->next);
    free(n->key);
    free(n->sval);
    free(n);
}

/* ── Parser state ── */
typedef struct { const char *p; int err; } PS;

static void ws(PS *s) {
    while (*s->p && isspace((unsigned char)*s->p)) s->p++;
}

static JNode *pval(PS *s);   /* forward */

/* Parse a quoted string, return heap-allocated copy (NULL on error). */
static char *pstr_raw(PS *s) {
    if (*s->p != '"') { s->err = 1; return NULL; }
    s->p++;
    int cap = 128, len = 0;
    char *buf = malloc(cap);
    while (*s->p && *s->p != '"') {
        char c;
        if (*s->p == '\\') {
            s->p++;
            switch (*s->p) {
                case '"':  c = '"';  break;
                case '\\': c = '\\'; break;
                case '/':  c = '/';  break;
                case 'n':  c = '\n'; break;
                case 'r':  c = '\r'; break;
                case 't':  c = '\t'; break;
                default:   c = *s->p; break;
            }
        } else {
            c = *s->p;
        }
        if (len >= cap - 1) { cap *= 2; buf = realloc(buf, cap); }
        buf[len++] = c;
        s->p++;
    }
    if (*s->p != '"') { s->err = 1; free(buf); return NULL; }
    s->p++;
    buf[len] = '\0';
    return buf;
}

static JNode *pstring(PS *s) {
    char *str = pstr_raw(s);
    if (!str) return NULL;
    JNode *n = calloc(1, sizeof(JNode));
    n->type = JT_STRING;
    n->sval = str;
    return n;
}

static JNode *pnumber(PS *s) {
    char *end;
    double d = strtod(s->p, &end);
    if (end == s->p) { s->err = 1; return NULL; }
    s->p = end;
    JNode *n = calloc(1, sizeof(JNode));
    n->type = JT_NUMBER;
    n->dval = d;
    return n;
}

static JNode *parray(PS *s) {
    s->p++;   /* skip '[' */
    JNode *arr = calloc(1, sizeof(JNode));
    arr->type = JT_ARRAY;
    JNode *last = NULL;
    ws(s);
    if (*s->p == ']') { s->p++; return arr; }
    while (*s->p) {
        JNode *v = pval(s);
        if (!v || s->err) { jfree(arr); return NULL; }
        if (!arr->child) arr->child = v; else last->next = v;
        last = v;
        ws(s);
        if (*s->p == ',') { s->p++; ws(s); }
        else if (*s->p == ']') { s->p++; break; }
        else { s->err = 1; jfree(arr); return NULL; }
    }
    return arr;
}

static JNode *pobject(PS *s) {
    s->p++;   /* skip '{' */
    JNode *obj = calloc(1, sizeof(JNode));
    obj->type = JT_OBJECT;
    JNode *last = NULL;
    ws(s);
    if (*s->p == '}') { s->p++; return obj; }
    while (*s->p) {
        ws(s);
        char *key = pstr_raw(s);
        if (!key || s->err) { jfree(obj); return NULL; }
        ws(s);
        if (*s->p != ':') { s->err = 1; free(key); jfree(obj); return NULL; }
        s->p++;
        ws(s);
        JNode *v = pval(s);
        if (!v || s->err) { free(key); jfree(obj); return NULL; }
        v->key = key;
        if (!obj->child) obj->child = v; else last->next = v;
        last = v;
        ws(s);
        if (*s->p == ',') { s->p++; }
        else if (*s->p == '}') { s->p++; break; }
        else { s->err = 1; jfree(obj); return NULL; }
    }
    return obj;
}

static JNode *pval(PS *s) {
    ws(s);
    char c = *s->p;
    if (c == '"') return pstring(s);
    if (c == '{') return pobject(s);
    if (c == '[') return parray(s);
    if (c == '-' || isdigit((unsigned char)c)) return pnumber(s);
    if (strncmp(s->p, "true",  4) == 0) { s->p += 4; JNode *n = calloc(1,sizeof(JNode)); n->type=JT_BOOL; n->bval=1; return n; }
    if (strncmp(s->p, "false", 5) == 0) { s->p += 5; JNode *n = calloc(1,sizeof(JNode)); n->type=JT_BOOL; n->bval=0; return n; }
    if (strncmp(s->p, "null",  4) == 0) { s->p += 4; JNode *n = calloc(1,sizeof(JNode)); n->type=JT_NULL;            return n; }
    s->err = 1;
    return NULL;
}

static JNode *json_parse(const char *text) {
    PS ps = { text, 0 };
    JNode *root = pval(&ps);
    if (ps.err) { jfree(root); return NULL; }
    return root;
}

/* ── Navigation helpers ── */

/* Look up an object child by key. */
static JNode *jget(JNode *obj, const char *key) {
    if (!obj || obj->type != JT_OBJECT) return NULL;
    for (JNode *c = obj->child; c; c = c->next)
        if (c->key && strcmp(c->key, key) == 0) return c;
    return NULL;
}

/* Get array element by index. */
static JNode *jidx(JNode *arr, int i) {
    if (!arr || arr->type != JT_ARRAY) return NULL;
    int k = 0;
    for (JNode *c = arr->child; c; c = c->next, k++)
        if (k == i) return c;
    return NULL;
}

/* Array length. */
static int jlen(JNode *arr) {
    if (!arr || arr->type != JT_ARRAY) return 0;
    int n = 0;
    for (JNode *c = arr->child; c; c = c->next) n++;
    return n;
}

/* Safe numeric getter with default. */
static double jnum(JNode *n, double def) {
    if (!n || n->type != JT_NUMBER) return def;
    return n->dval;
}

/* Safe string getter with default. */
static const char *jstr(JNode *n, const char *def) {
    if (!n || n->type != JT_STRING) return def;
    return n->sval;
}

/* ================================================================
 * parse_flight_node  (internal helper)
 *
 * Populates a FlightConfig from a JNode that is already the root
 * of a single flight plan (the object with "ID", "Type", "Leg"…).
 * Called by both load_flight_from_json and load_scenario_from_json.
 * ================================================================ */

static int parse_flight_node(JNode *root, FlightConfig *cfg) {
    memset(cfg, 0, sizeof(FlightConfig));

    cfg->json_id = (int)jnum(jget(root, "ID"), 0);
    strncpy(cfg->type,           jstr(jget(root, "Type"),          "regular"), sizeof(cfg->type)           - 1);
    strncpy(cfg->route,          jstr(jget(root, "Route"),         ""),        sizeof(cfg->route)          - 1);
    strncpy(cfg->departure_time, jstr(jget(root, "DepartureTime"), ""),        sizeof(cfg->departure_time) - 1);

    JNode *legs = jget(root, "Leg");
    if (!legs || jlen(legs) == 0) return -1;
    JNode *leg = jidx(legs, 0);

    strncpy(cfg->departure, jstr(jget(leg, "Departure"), "???"), sizeof(cfg->departure) - 1);
    strncpy(cfg->arrival,   jstr(jget(leg, "Arrival"),   "???"), sizeof(cfg->arrival)   - 1);

    JNode *fuel = jget(leg, "Fuel");
    cfg->fuel_kg = jnum(jget(fuel, "Quantity"), AC_M_FUEL);

    JNode *prof = jget(leg, "Flight Profile");

    /* Climb */
    JNode *climb_arr = jget(prof, "Climb");
    cfg->climb_count = jlen(climb_arr);
    if (cfg->climb_count > MAX_PROFILE_ENTRIES) cfg->climb_count = MAX_PROFILE_ENTRIES;
    for (int i = 0; i < cfg->climb_count; i++) {
        JNode *e = jidx(climb_arr, i);
        cfg->climb[i].altitude_m = jnum(jget(jget(e, "Altitude"), "Value"), 0.0);
        cfg->climb[i].speed_ms   = jnum(jget(jget(e, "Speed"),    "Value"), 210.0) * KNOT_TO_MS;
        cfg->climb[i].rate_ms    = jnum(jget(jget(e, "RateClimb"), "Value"), 5.0);
    }

    /* Descend */
    JNode *desc_arr = jget(prof, "Descend");
    cfg->descend_count = jlen(desc_arr);
    if (cfg->descend_count > MAX_PROFILE_ENTRIES) cfg->descend_count = MAX_PROFILE_ENTRIES;
    for (int i = 0; i < cfg->descend_count; i++) {
        JNode *e = jidx(desc_arr, i);
        cfg->descend[i].altitude_m = jnum(jget(jget(e, "Altitude"),    "Value"),  0.0);
        cfg->descend[i].speed_ms   = jnum(jget(jget(e, "Speed"),       "Value"),  250.0) * KNOT_TO_MS;
        cfg->descend[i].rate_ms    = jnum(jget(jget(e, "RateDescent"), "Value"), -5.0);
    }

    /* Cruise */
    JNode *cruise = jget(prof, "Cruise");
    cfg->cruise_speed_ms = jnum(jget(jget(cruise, "Speed"), "Value"), 460.0) * KNOT_TO_MS;

    /* Segments */
    JNode *segs = jget(leg, "Segments");
    cfg->segment_count = jlen(segs);
    if (cfg->segment_count > MAX_SEGMENTS) cfg->segment_count = MAX_SEGMENTS;
    for (int i = 0; i < cfg->segment_count; i++) {
        JNode *s  = jidx(segs, i);
        strncpy(cfg->segments[i].mode, jstr(jget(s, "Mode"), "cruise"), sizeof(cfg->segments[i].mode) - 1);
        JNode *st = jget(s, "Start");
        cfg->segments[i].start.lat   = jnum(jget(st, "Latitude"),  0.0);
        cfg->segments[i].start.lon   = jnum(jget(st, "Longitude"), 0.0);
        cfg->segments[i].start.alt_m = jnum(jget(jget(st, "Altitude"), "Quantity"), 0.0);
        JNode *en = jget(s, "End");
        cfg->segments[i].end.lat   = jnum(jget(en, "Latitude"),  0.0);
        cfg->segments[i].end.lon   = jnum(jget(en, "Longitude"), 0.0);
        cfg->segments[i].end.alt_m = jnum(jget(jget(en, "Altitude"), "Quantity"), 0.0);
    }

    /* Derive origin / destination / cruise altitude */
    if (cfg->segment_count > 0) {
        cfg->origin      = cfg->segments[0].start;
        cfg->destination = cfg->segments[cfg->segment_count - 1].end;
        cfg->cruise_alt_m = AC_CRUISE_ALT;
        for (int i = 0; i < cfg->segment_count; i++) {
            if (strcmp(cfg->segments[i].mode, "climb") == 0) {
                cfg->cruise_alt_m = cfg->segments[i].end.alt_m;
                break;
            }
        }
    } else {
        cfg->cruise_alt_m = AC_CRUISE_ALT;
    }

    return 0;
}

/* ── Read a file into a heap-allocated buffer (caller must free). ── */
static char *read_file(const char *filename) {
    FILE *f = fopen(filename, "r");
    if (!f) { fprintf(stderr, "[JSON] Cannot open '%s'\n", filename); return NULL; }
    fseek(f, 0, SEEK_END);
    long sz = ftell(f); rewind(f);
    char *buf = malloc(sz + 1);
    fread(buf, 1, sz, f); fclose(f);
    buf[sz] = '\0';
    return buf;
}

/* ================================================================
 * load_flight_from_json  — single-flight JSON file
 * ================================================================ */

int load_flight_from_json(const char *filename, FlightConfig *cfg) {
    char *text = read_file(filename);
    if (!text) return -1;
    JNode *root = json_parse(text);
    free(text);
    if (!root) { fprintf(stderr, "[JSON] Parse error in '%s'\n", filename); return -1; }
    int rc = parse_flight_node(root, cfg);
    if (rc != 0) fprintf(stderr, "[JSON] Missing 'Leg' in '%s'\n", filename);
    jfree(root);
    return rc;
}

/* ================================================================
 * load_scenario_from_json  — all-in-one scenario JSON
 *
 * Expected format:
 *   {
 *     "SimulationConfig": {
 *       "TotalSteps":       100,
 *       "TimeStepSeconds":  60,
 *       "SafetyThresholdM": 5000,
 *       "MaxViolations":    3
 *     },
 *     "Flights": [ { ...flight plan 1... }, { ...flight plan 2... } ]
 *   }
 * ================================================================ */

int load_scenario_from_json(const char *filename,
                             SimulationConfig *sim_cfg,
                             FlightConfig     *flights,
                             int              *flight_count) {
    char *text = read_file(filename);
    if (!text) return -1;
    JNode *root = json_parse(text);
    free(text);
    if (!root) { fprintf(stderr, "[JSON] Parse error in '%s'\n", filename); return -1; }

    /* ── SimulationConfig block ── */
    JNode *sc = jget(root, "SimulationConfig");
    if (!sc) {
        fprintf(stderr, "[JSON] Missing 'SimulationConfig' in '%s'\n", filename);
        jfree(root); return -1;
    }
    sim_cfg->total_steps        = (int)jnum(jget(sc, "TotalSteps"),       100);
    sim_cfg->time_step_seconds  = (int)jnum(jget(sc, "TimeStepSeconds"),   60);
    sim_cfg->safety_threshold_m =      jnum(jget(sc, "SafetyThresholdM"), 5000.0);
    sim_cfg->max_violations     = (int)jnum(jget(sc, "MaxViolations"),      3);

    /* ── Flights array ── */
    JNode *farr = jget(root, "Flights");
    if (!farr || jlen(farr) == 0) {
        fprintf(stderr, "[JSON] Missing or empty 'Flights' in '%s'\n", filename);
        jfree(root); return -1;
    }
    int n = jlen(farr);
    if (n > MAX_FLIGHTS) n = MAX_FLIGHTS;
    *flight_count = 0;
    for (int i = 0; i < n; i++) {
        JNode *fnode = jidx(farr, i);
        if (parse_flight_node(fnode, &flights[i]) != 0) {
            fprintf(stderr, "[JSON] Could not parse flight %d in '%s'\n", i, filename);
            jfree(root); return -1;
        }
        (*flight_count)++;
    }

    jfree(root);
    return 0;
}

/* ================================================================
 * print_flight_config — one-line summary
 * ================================================================ */

void print_flight_config(const FlightConfig *cfg) {
    printf("  [JSON] ID=%-6d  %s → %s  Route=%-8s  Fuel=%.0f kg  "
           "CruiseAlt=%.0f m  CruiseTAS=%.0f kt  "
           "Climb entries=%d  Descend entries=%d  Segments=%d\n",
           cfg->json_id, cfg->departure, cfg->arrival, cfg->route,
           cfg->fuel_kg, cfg->cruise_alt_m,
           cfg->cruise_speed_ms / KNOT_TO_MS,
           cfg->climb_count, cfg->descend_count, cfg->segment_count);
}
