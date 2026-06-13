// TP200 — LIS → MAD com vento > 30 m/s num segmento
// Expected: PASS import | FAIL US085 (Phase 2 — wind safety violation)
// O C checker rejeita segmentos com vento >= MAX_WIND_MS (30 m/s).
// Segmento de cruise tem 35 m/s (~68 kt) — acima do limite operacional.
flight TP200 {
    type: regular

    leg {
        departure: LIS 2026-10-10 07:00
        arrival:   MAD 08:30
        fuel:      2500 kg

        segment {
            phase: climb
            from: (38.7813, -9.1359)
            to:   (39.2000, -7.0000)
            altitude_range: [0, 33000]
            width: 50 m
            wind:  270 deg 12 m/s
        }
        segment {
            phase: cruise
            from: (39.2000, -7.0000)
            to:   (40.0000, -4.5000)
            altitude_range: [31000, 33000]
            width: 50 m
            wind:  270 deg 35 m/s
        }
        segment {
            phase: descent
            from: (40.0000, -4.5000)
            to:   (40.4937, -3.5670)
            altitude_range: [0, 33000]
            width: 50 m
            wind:  270 deg 10 m/s
        }
    }
}
