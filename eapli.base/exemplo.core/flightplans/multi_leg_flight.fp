flight TP002 {
    type: regular

    leg {
        departure: OPO 2026-06-15 07:00
        arrival:   LIS 08:00
        fuel:      4500 kg

        segment {
            from: (41.2481, -8.6814)
            to:   (38.7813, -9.1359)
            altitude_slots: [8000, 10000, 12000]
            width: 50 m
            wind:  270 deg 12 m/s
        }
    }

    leg {
        departure: LIS 2026-06-15 09:00
        arrival:   FAO 09:45
        fuel:      3000 kg

        segment {
            from: (38.7813, -9.1359)
            to:   (37.0144, -7.9659)
            altitude_slots: [6000, 8000]
            width: 40 m
            wind:  200 deg 8 m/s
        }
    }
}
