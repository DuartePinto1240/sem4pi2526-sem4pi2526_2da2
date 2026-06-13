// TP101 — Erro semântico: sequência de fases incorreta
// Expected: FAIL import (Phase 1 — SEMANTIC errors)
// Erros detetados pelo FlightPlanSemanticVisitor:
//   1. Primeiro segmento tem fase 'cruise' em vez de 'climb'
//   2. Segmento do meio tem fase 'descent' em vez de 'cruise'
//   3. Último segmento tem fase 'climb' em vez de 'descent'
flight TP101 {
    type: regular

    leg {
        departure: LIS 2026-10-05 09:00
        arrival:   OPO 10:00
        fuel:      1500 kg

        segment {
            phase: cruise
            from: (38.7813, -9.1359)
            to:   (39.5000, -8.8000)
            altitude_range: [0, 12000]
            width: 50 m
            wind:  180 deg 10 m/s
        }
        segment {
            phase: descent
            from: (39.5000, -8.8000)
            to:   (40.5000, -8.8000)
            altitude_range: [10000, 12000]
            width: 50 m
            wind:  180 deg 10 m/s
        }
        segment {
            phase: climb
            from: (40.5000, -8.8000)
            to:   (41.2481, -8.6814)
            altitude_range: [0, 12000]
            width: 50 m
            wind:  180 deg 8 m/s
        }
    }
}
