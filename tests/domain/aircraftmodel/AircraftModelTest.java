package domain.aircraftmodel;

import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AircraftModelTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private EngineModel turbofanEngine() {
        return new EngineModel("LEAP-1B", "CFM International",
                MotorizationType.TURBOFAN,
                new Thrust(101.8, 121.4), new TSFC(0.0157));
    }

    private EngineModel anotherTurbofanEngine() {
        return new EngineModel("Trent 900", "Rolls-Royce",
                MotorizationType.TURBOFAN,
                new Thrust(311.0, 340.0), new TSFC(0.0152));
    }

    private EngineModel turbopropEngine() {
        return new EngineModel("PW127", "Pratt & Whitney Canada",
                MotorizationType.TURBOPROP,
                new Thrust(15.0, 20.5), new TSFC(0.0300));
    }
}