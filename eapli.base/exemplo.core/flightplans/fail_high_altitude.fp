// TP014 — LIS → MAD altitude acima do service ceiling
// Expected: FAIL (fase 2 — max 52000ft excede service ceiling padrão 45000ft)
flight TP014 {
    type: regular

    leg {
        departure: LIS 2026-09-10 08:00
        arrival:   MAD 09:30
        fuel:      2000 kg

        segment {
            phase: climb
            from: (38.7813, -9.1359)
            to:   (39.5000, -6.5000)
            altitude_range: [0, 52000]
            width: 50 m
            wind:  090 deg 10 m/s
        }
        segment {
            phase: cruise
            from: (39.5000, -6.5000)
            to:   (40.2000, -4.8000)
            altitude_range: [50000, 52000]
            width: 50 m
            wind:  090 deg 10 m/s
        }
        segment {
            phase: descent
            from: (40.2000, -4.8000)
            to:   (40.4937, -3.5670)
            altitude_range: [0, 52000]
            width: 50 m
            wind:  090 deg 8 m/s
        }
    }
}
