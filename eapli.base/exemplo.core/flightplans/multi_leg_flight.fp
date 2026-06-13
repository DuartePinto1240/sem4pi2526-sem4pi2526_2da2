// TP002 — OPO → LIS → FAO (regular, 2 legs)
// Expected: PASS
flight TP002 {
    type: regular

    leg {
        departure: OPO 2026-06-15 07:00
        arrival:   LIS 08:00
        fuel:      4500 kg

        segment {
            phase: climb
            from: (41.2481, -8.6814)
            to:   (40.5000, -8.8000)
            altitude_range: [0, 12000]
            width: 50 m
            wind:  270 deg 10 m/s
        }
        segment {
            phase: cruise
            from: (40.5000, -8.8000)
            to:   (39.3000, -9.0000)
            altitude_range: [10000, 12000]
            width: 50 m
            wind:  270 deg 12 m/s
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

    leg {
        departure: LIS 2026-06-15 09:00
        arrival:   FAO 09:45
        fuel:      3000 kg

        segment {
            phase: climb
            from: (38.7813, -9.1359)
            to:   (38.2000, -8.5000)
            altitude_range: [0, 8000]
            width: 40 m
            wind:  200 deg 6 m/s
        }
        segment {
            phase: cruise
            from: (38.2000, -8.5000)
            to:   (37.4000, -8.2000)
            altitude_range: [6000, 8000]
            width: 40 m
            wind:  200 deg 8 m/s
        }
        segment {
            phase: descent
            from: (37.4000, -8.2000)
            to:   (37.0144, -7.9659)
            altitude_range: [0, 8000]
            width: 40 m
            wind:  200 deg 5 m/s
        }
    }
}
