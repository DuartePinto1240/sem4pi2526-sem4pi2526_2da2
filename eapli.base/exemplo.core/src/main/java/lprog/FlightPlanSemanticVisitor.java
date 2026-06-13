package lprog;

import lprog.FlightPlanBaseVisitor;
import lprog.FlightPlanParser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlightPlanSemanticVisitor extends FlightPlanBaseVisitor<Void> {

    private final ErrorCollector errorCollector;
    private final Set<String> flightIds = new HashSet<>();
    private String lastArrivalAirport = null;
    private String lastArrivalTime = null;

    public FlightPlanSemanticVisitor(final ErrorCollector errorCollector) {
        this.errorCollector = errorCollector;
    }

    // ─────────────────────────────────────────
    // Flight level
    // ─────────────────────────────────────────

    @Override
    public Void visitFlight(FlightPlanParser.FlightContext ctx) {
        String flightId = ctx.ID().getText();

        // Flight ID must be unique within the file
        if (flightIds.contains(flightId)) {
            errorCollector.addSemanticError(
                    ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    "Duplicate flight identifier: '" + flightId + "'");
        } else {
            flightIds.add(flightId);
        }

        // reset leg sequence tracking for each flight
        lastArrivalAirport = null;
        lastArrivalTime = null;

        return visitChildren(ctx);
    }

    // ─────────────────────────────────────────
    // Leg level
    // ─────────────────────────────────────────

    @Override
    public Void visitLeg(FlightPlanParser.LegContext ctx) {
        // Check leg sequence coherence
        String departureAirport = ctx.departure().airportCode().getText();
        String departureTime    = ctx.departure().time().getText();

        if (lastArrivalAirport != null) {
            if (!lastArrivalAirport.equals(departureAirport)) {
                errorCollector.addSemanticError(
                        ctx.departure().getStart().getLine(),
                        ctx.departure().getStart().getCharPositionInLine(),
                        "Departure airport of this leg ('" + departureAirport +
                                "') does not match arrival airport of previous leg ('" +
                                lastArrivalAirport + "')");
            }
            if (lastArrivalTime != null &&
                    lastArrivalTime.compareTo(departureTime) >= 0) {
                errorCollector.addSemanticError(
                        ctx.departure().getStart().getLine(),
                        ctx.departure().getStart().getCharPositionInLine(),
                        "Departure time of this leg ('" + departureTime +
                                "') must be after arrival time of previous leg ('" +
                                lastArrivalTime + "')");
            }
        }

        // Validate segment count and phase sequence: climb → cruise(s) → descent
        List<FlightPlanParser.SegmentContext> segs = ctx.segment();
        if (segs.size() < 3) {
            errorCollector.addSemanticError(
                    ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    "Each leg must have at least 3 segments (climb, cruise, descent); found " + segs.size());
        } else {
            String firstPhase = segs.get(0).phaseType().getText().toLowerCase();
            String lastPhase  = segs.get(segs.size() - 1).phaseType().getText().toLowerCase();
            if (!firstPhase.equals("climb")) {
                errorCollector.addSemanticError(
                        segs.get(0).getStart().getLine(),
                        segs.get(0).getStart().getCharPositionInLine(),
                        "First segment of a leg must have phase 'climb' (got '" + firstPhase + "')");
            }
            if (!lastPhase.equals("descent")) {
                errorCollector.addSemanticError(
                        segs.get(segs.size() - 1).getStart().getLine(),
                        segs.get(segs.size() - 1).getStart().getCharPositionInLine(),
                        "Last segment of a leg must have phase 'descent' (got '" + lastPhase + "')");
            }
            for (int i = 1; i < segs.size() - 1; i++) {
                String midPhase = segs.get(i).phaseType().getText().toLowerCase();
                if (!midPhase.equals("cruise")) {
                    errorCollector.addSemanticError(
                            segs.get(i).getStart().getLine(),
                            segs.get(i).getStart().getCharPositionInLine(),
                            "Middle segments must have phase 'cruise' (segment " + (i + 1) + " has '" + midPhase + "')");
                }
            }
        }

        visitChildren(ctx);

        lastArrivalAirport = ctx.arrival().airportCode().getText();
        lastArrivalTime    = ctx.arrival().time().getText();

        return null;
    }

    // ─────────────────────────────────────────
    // Fuel level
    // ─────────────────────────────────────────

    @Override
    public Void visitFuel(FlightPlanParser.FuelContext ctx) {
        double quantity = Double.parseDouble(ctx.NUMBER().getText());

        if (quantity <= 0) {
            errorCollector.addSemanticError(
                    ctx.NUMBER().getSymbol().getLine(),
                    ctx.NUMBER().getSymbol().getCharPositionInLine(),
                    "Fuel quantity must be strictly positive (got: " + quantity + ")");
        }

        return visitChildren(ctx);
    }

    // ─────────────────────────────────────────
    // Segment level
    // ─────────────────────────────────────────

    @Override
    public Void visitSegment(FlightPlanParser.SegmentContext ctx) {
        // Coordinates must be different
        List<FlightPlanParser.CoordinatesContext> coords = ctx.coordinates();
        if (coords.size() == 2) {
            String from = coords.get(0).getText();
            String to   = coords.get(1).getText();
            if (from.equals(to)) {
                errorCollector.addSemanticError(
                        ctx.getStart().getLine(),
                        ctx.getStart().getCharPositionInLine(),
                        "Segment start and end coordinates must be different");
            }
        }

        // Direct NUMBER tokens in segment rule:
        //   [0]=altitudeMin  [1]=altitudeMax  [2]=width  [3]=windAngle  [4]=windSpeed
        double altMin    = Double.parseDouble(ctx.NUMBER(0).getText());
        double altMax    = Double.parseDouble(ctx.NUMBER(1).getText());
        double width     = Double.parseDouble(ctx.NUMBER(2).getText());
        double windSpeed = Double.parseDouble(ctx.NUMBER(4).getText());

        if (altMin < 0) {
            errorCollector.addSemanticError(ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    "Altitude range minimum must be non-negative (got: " + altMin + ")");
        }
        if (altMax <= altMin) {
            errorCollector.addSemanticError(ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    "Altitude range maximum (" + altMax + ") must be greater than minimum (" + altMin + ")");
        }
        if (width <= 0) {
            errorCollector.addSemanticError(ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    "Segment width must be positive (got: " + width + ")");
        }
        if (windSpeed < 0) {
            errorCollector.addSemanticError(ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    "Wind speed must be non-negative (got: " + windSpeed + ")");
        }

        return visitChildren(ctx);
    }

    // ─────────────────────────────────────────
    // Airport code level
    // ─────────────────────────────────────────

    @Override
    public Void visitAirportCode(FlightPlanParser.AirportCodeContext ctx) {
        String code = ctx.ID().getText();

        // IATA: exactly 3 letters, ICAO: exactly 4 letters
        if (!code.matches("[A-Z]{3}") && !code.matches("[A-Z]{4}")) {
            errorCollector.addSemanticError(
                    ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    "Invalid airport code '" + code +
                            "': must be IATA (3 uppercase letters) or ICAO (4 uppercase letters)");
        }

        return visitChildren(ctx);
    }
}