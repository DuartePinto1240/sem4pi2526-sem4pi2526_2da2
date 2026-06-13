// TP011 — LIS → FRA combustível insuficiente
// Expected: FAIL (fase 2 — ~1800km, declara 500kg, precisa ~4700kg)
flight TP011 {
    type: regular

    leg {
        departure: LIS 2026-09-02 06:00
        arrival:   FRA 10:00
        fuel:      500 kg

        segment {
            phase: climb
            from: (38.7813, -9.1359)
            to:   (41.0000, -5.0000)
            altitude_range: [0, 37000]
            width: 50 m
            wind:  270 deg 15 m/s
        }
        segment {
            phase: cruise
            from: (41.0000, -5.0000)
            to:   (47.0000,  5.0000)
            altitude_range: [35000, 37000]
            width: 50 m
            wind:  270 deg 20 m/s
        }
        segment {
            phase: descent
            from: (47.0000,  5.0000)
            to:   (50.0379,  8.5622)
            altitude_range: [0, 37000]
            width: 50 m
            wind:  270 deg 12 m/s
        }
    }
}
