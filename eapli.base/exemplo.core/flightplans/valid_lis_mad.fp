flight TP010 {
    type: regular

    leg {
        departure: LIS 2026-09-01 07:00
        arrival:   MAD 08:30
        fuel:      1600 kg

        segment {
            from: (38.7813, -9.1359)
            to:   (40.4937, -3.5670)
            altitude_slots: [28000, 30000, 32000]
            width: 50 m
            wind:  090 deg 15 m/s
        }
    }
}
