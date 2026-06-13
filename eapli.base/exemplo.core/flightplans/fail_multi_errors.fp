// TP201 — LIS → FRA com múltiplas falhas no C checker
// Expected: PASS import | FAIL US085 (Phase 2 — múltiplos erros)
// Erros esperados:
//   1. Altitude máxima 55000 ft excede service ceiling (~45000 ft)
//   2. Combustível declarado 300 kg muito abaixo do mínimo (~5900+ kg para ~1800 km)
//   3. Vento de 32 m/s no segmento de climb excede limite de 30 m/s
flight TP201 {
    type: regular

    leg {
        departure: LIS 2026-10-15 06:00
        arrival:   FRA 10:00
        fuel:      300 kg

        segment {
            phase: climb
            from: (38.7813, -9.1359)
            to:   (41.0000, -5.0000)
            altitude_range: [0, 55000]
            width: 50 m
            wind:  270 deg 32 m/s
        }
        segment {
            phase: cruise
            from: (41.0000, -5.0000)
            to:   (47.0000,  5.0000)
            altitude_range: [53000, 55000]
            width: 50 m
            wind:  270 deg 20 m/s
        }
        segment {
            phase: descent
            from: (47.0000,  5.0000)
            to:   (50.0379,  8.5622)
            altitude_range: [0, 55000]
            width: 50 m
            wind:  270 deg 15 m/s
        }
    }
}
