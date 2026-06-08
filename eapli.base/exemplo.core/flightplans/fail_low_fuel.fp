flight TP011 {
    type: regular

    leg {
        departure: LIS 2026-09-02 06:00
        arrival:   FRA 10:00
        fuel:      500 kg

        segment {
            from: (38.7813, -9.1359)
            to:   (50.0379,  8.5622)
            altitude_slots: [33000, 35000, 37000]
            width: 50 m
            wind:  270 deg 20 m/s
        }
    }
}
