// TP013 — OPO → LIS combustível insuficiente para a distância (~280 km)
// Expected: FAIL (fuel declared 200 kg < required ~900 kg)
flight TP013 {
    type: regular

    leg {
        departure: OPO 2026-09-05 09:00
        arrival:   LIS 10:00
        fuel:      200 kg

        segment {
            phase: climb
            from: (41.2481, -8.6814)
            to:   (40.5000, -8.9000)
            altitude_range: [0, 10000]
            width: 50 m
            wind:  180 deg 10 m/s
        }
        segment {
            phase: cruise
            from: (40.5000, -8.9000)
            to:   (39.3000, -9.0000)
            altitude_range: [8000, 10000]
            width: 50 m
            wind:  180 deg 10 m/s
        }
        segment {
            phase: descent
            from: (39.3000, -9.0000)
            to:   (38.7813, -9.1359)
            altitude_range: [0, 10000]
            width: 50 m
            wind:  180 deg 8 m/s
        }
    }
}
