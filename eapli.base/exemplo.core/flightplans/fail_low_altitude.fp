flight TP013 {
    type: regular

    leg {
        departure: OPO 2026-09-05 09:00
        arrival:   LIS 10:00
        fuel:      1200 kg

        segment {
            from: (41.2481, -8.6814)
            to:   (38.7813, -9.1359)
            altitude_slots: [200, 300]
            width: 50 m
            wind:  180 deg 10 m/s
        }
    }
}
