package lprog;

import lprog.FlightPlanBaseListener;
import lprog.FlightPlanParser;

import java.util.ArrayList;
import java.util.List;

public class FlightPlanBuilderListener extends FlightPlanBaseListener {

    // ─────────────────────────────────────────
    // Internal representation classes
    // ─────────────────────────────────────────

    public static class SegmentData {
        public double fromLat, fromLon;
        public double toLat, toLon;
        public String phase;           // "climb", "cruise", "descent"
        public double altitudeMin;
        public double altitudeMax;
        public double width;
        public double windAngle;
        public double windSpeed;
    }

    public static class LegData {
        public String departureAirport;
        public String departureDate;
        public String departureTime;
        public String arrivalAirport;
        public String arrivalTime;
        public double fuelQuantity;
        public String fuelUnit;
        public List<SegmentData> segments = new ArrayList<>();
    }

    public static class FlightData {
        public String id;
        public String type;
        public List<LegData> legs = new ArrayList<>();
    }

    // ─────────────────────────────────────────
    // State
    // ─────────────────────────────────────────

    private final List<FlightData> flights = new ArrayList<>();
    private FlightData currentFlight;
    private LegData currentLeg;
    private SegmentData currentSegment;

    // ─────────────────────────────────────────
    // Flight
    // ─────────────────────────────────────────

    @Override
    public void enterFlight(FlightPlanParser.FlightContext ctx) {
        currentFlight = new FlightData();
        currentFlight.id   = ctx.ID().getText();
        currentFlight.type = ctx.flightType().getText().toLowerCase();
    }

    @Override
    public void exitFlight(FlightPlanParser.FlightContext ctx) {
        flights.add(currentFlight);
        currentFlight = null;
    }

    // ─────────────────────────────────────────
    // Leg
    // ─────────────────────────────────────────

    @Override
    public void enterLeg(FlightPlanParser.LegContext ctx) {
        currentLeg = new LegData();
    }

    @Override
    public void exitLeg(FlightPlanParser.LegContext ctx) {
        currentFlight.legs.add(currentLeg);
        currentLeg = null;
    }

    // ─────────────────────────────────────────
    // Departure
    // ─────────────────────────────────────────

    @Override
    public void enterDeparture(FlightPlanParser.DepartureContext ctx) {
        currentLeg.departureAirport = ctx.airportCode().getText();
        currentLeg.departureDate    = ctx.date().getText();
        currentLeg.departureTime    = ctx.time().getText();
    }

    // ─────────────────────────────────────────
    // Arrival
    // ─────────────────────────────────────────

    @Override
    public void enterArrival(FlightPlanParser.ArrivalContext ctx) {
        currentLeg.arrivalAirport = ctx.airportCode().getText();
        currentLeg.arrivalTime    = ctx.time().getText();
    }

    // ─────────────────────────────────────────
    // Fuel
    // ─────────────────────────────────────────

    @Override
    public void enterFuel(FlightPlanParser.FuelContext ctx) {
        currentLeg.fuelQuantity = Double.parseDouble(ctx.NUMBER().getText());
        currentLeg.fuelUnit     = ctx.unit().getText();
    }

    // ─────────────────────────────────────────
    // Segment
    // ─────────────────────────────────────────

    @Override
    public void enterSegment(FlightPlanParser.SegmentContext ctx) {
        currentSegment = new SegmentData();

        // phase
        currentSegment.phase = ctx.phaseType().getText().toLowerCase();

        // from coordinates
        FlightPlanParser.CoordinatesContext from = ctx.coordinates(0);
        currentSegment.fromLat = Double.parseDouble(from.NUMBER(0).getText());
        currentSegment.fromLon = Double.parseDouble(from.NUMBER(1).getText());

        // to coordinates
        FlightPlanParser.CoordinatesContext to = ctx.coordinates(1);
        currentSegment.toLat = Double.parseDouble(to.NUMBER(0).getText());
        currentSegment.toLon = Double.parseDouble(to.NUMBER(1).getText());

        // ctx.NUMBER() contains ONLY the direct NUMBER tokens of the segment rule
        // (coordinates are in sub-rules and not included here).
        // Layout with altitude_range [min, max]:
        //   index 0 → altitudeMin
        //   index 1 → altitudeMax
        //   index 2 → width
        //   index 3 → windAngle
        //   index 4 → windSpeed
        currentSegment.altitudeMin = Double.parseDouble(ctx.NUMBER(0).getText());
        currentSegment.altitudeMax = Double.parseDouble(ctx.NUMBER(1).getText());
        currentSegment.width       = Double.parseDouble(ctx.NUMBER(2).getText());
        currentSegment.windAngle   = Double.parseDouble(ctx.NUMBER(3).getText());
        currentSegment.windSpeed   = Double.parseDouble(ctx.NUMBER(4).getText());
    }

    @Override
    public void exitSegment(FlightPlanParser.SegmentContext ctx) {
        currentLeg.segments.add(currentSegment);
        currentSegment = null;
    }

    // ─────────────────────────────────────────
    // Result
    // ─────────────────────────────────────────

    public List<FlightData> getFlights() {
        return flights;
    }
}