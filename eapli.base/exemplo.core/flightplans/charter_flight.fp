flight TP003 {
    type: charter

    leg {
        departure: LIS 2026-07-01 10:00
        arrival:   LPMA 11:30
        fuel:      8000 kg

        segment {
            from: (38.7813, -9.1359)
            to:   (32.6978, -16.7782)
            altitude_slots: [10000, 12000, 14000]
            width: 60 m
            wind:  310 deg 20 m/s
        }
    }
}
