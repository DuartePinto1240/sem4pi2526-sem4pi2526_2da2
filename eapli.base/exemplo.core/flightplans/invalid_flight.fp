// TP_INVALID — combustível negativo
// Expected: FAIL (fase 1 — erro DSL)
flight TP_INVALID {
    type: regular

    leg {
        departure: OPO 2026-06-01 08:00
        arrival:   LIS 09:00
        fuel:      -500 kg

        segment {
            phase: climb
            from: (41.2481, -8.6814)
            to:   (40.5000, -8.9000)
            altitude_range: [0, 12000]
            width: 50 m
            wind:  270 deg 10 m/s
        }
        segment {
            phase: cruise
            from: (40.5000, -8.9000)
            to:   (39.3000, -9.0000)
            altitude_range: [10000, 12000]
            width: 50 m
            wind:  270 deg 10 m/s
        }
        segment {
            phase: descent
            from: (39.3000, -9.0000)
            to:   (38.7813, -9.1359)
            altitude_range: [0, 12000]
            width: 50 m
            wind:  270 deg 8 m/s
        }
    }
}
