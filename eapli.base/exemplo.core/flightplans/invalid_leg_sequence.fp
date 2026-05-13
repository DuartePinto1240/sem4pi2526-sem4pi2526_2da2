flight TP_MISMATCH {
    type: regular

    leg {
        departure: OPO 2026-06-01 08:00
        arrival:   LIS 09:00
        fuel:      5000 kg

        segment {
            from: (41.2481, -8.6814)
            to:   (38.7813, -9.1359)
            altitude_slots: [8000]
            width: 50 m
            wind:  270 deg 10 m/s
        }
    }

    leg {
        departure: FAO 2026-06-01 10:00
        arrival:   OPO 11:00
        fuel:      3000 kg

        segment {
            from: (37.0144, -7.9659)
            to:   (41.2481, -8.6814)
            altitude_slots: [8000]
            width: 50 m
            wind:  90 deg 15 m/s
        }
    }
}
