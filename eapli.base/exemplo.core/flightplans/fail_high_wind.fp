flight TP012 {
    type: regular

    leg {
        departure: OPO 2026-10-01 08:00
        arrival:   LIS 09:00
        fuel:      1200 kg

        segment {
            from: (41.2481, -8.6814)
            to:   (38.7813, -9.1359)
            altitude_slots: [20000, 22000]
            width: 50 m
            wind:  270 deg 75 m/s
        }
    }
}
