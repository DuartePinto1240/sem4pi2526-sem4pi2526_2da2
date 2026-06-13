// TP003 — LIS → LPMA Madeira (charter)
// Expected: PASS
flight TP003 {
    type: charter

    leg {
        departure: LIS 2026-07-01 10:00
        arrival:   LPMA 11:30
        fuel:      8000 kg

        segment {
            phase: climb
            from: (38.7813, -9.1359)
            to:   (37.5000, -11.0000)
            altitude_range: [0, 35000]
            width: 60 m
            wind:  310 deg 15 m/s
        }
        segment {
            phase: cruise
            from: (37.5000, -11.0000)
            to:   (34.0000, -14.0000)
            altitude_range: [33000, 35000]
            width: 60 m
            wind:  310 deg 20 m/s
        }
        segment {
            phase: descent
            from: (34.0000, -14.0000)
            to:   (32.6978, -16.7782)
            altitude_range: [0, 35000]
            width: 60 m
            wind:  310 deg 12 m/s
        }
    }
}
