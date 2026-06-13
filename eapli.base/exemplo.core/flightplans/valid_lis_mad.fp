// TP010 — LIS → MAD (regular, single leg)
// Expected: PASS
flight TP010 {
    type: regular

    leg {
        departure: LIS 2026-09-01 07:00
        arrival:   MAD 08:30
        fuel:      2000 kg

        segment {
            phase: climb
            from: (38.7813, -9.1359)
            to:   (39.2000, -7.0000)
            altitude_range: [0, 33000]
            width: 50 m
            wind:  090 deg 10 m/s
        }
        segment {
            phase: cruise
            from: (39.2000, -7.0000)
            to:   (40.0000, -4.5000)
            altitude_range: [31000, 33000]
            width: 50 m
            wind:  090 deg 15 m/s
        }
        segment {
            phase: descent
            from: (40.0000, -4.5000)
            to:   (40.4937, -3.5670)
            altitude_range: [0, 33000]
            width: 50 m
            wind:  090 deg 8 m/s
        }
    }
}
