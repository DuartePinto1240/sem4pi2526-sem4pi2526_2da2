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
            // arrival of leg n must match departure of leg n+1
            if (!lastArrivalAirport.equals(departureAirport)) {
                errorCollector.addSemanticError(
                        ctx.departure().getStart().getLine(),
                        ctx.departure().getStart().getCharPositionInLine(),
                        "Departure airport of this leg ('" + departureAirport +
                                "') does not match arrival airport of previous leg ('" +
                                lastArrivalAirport + "')");
            }

            // arrival time of leg n must precede departure time of leg n+1
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

        // Visit children to process segments and fuel
        visitChildren(ctx);

        // Update last arrival for next leg check
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
        List<FlightPlanParser.CoordinatesContext> coords = ctx.coordinates();

        if (coords.size() == 2) {
            String from = coords.get(0).getText();
            String to   = coords.get(1).getText();

            // start and end coordinates must be different
            if (from.equals(to)) {
                errorCollector.addSemanticError(
                        ctx.getStart().getLine(),
                        ctx.getStart().getCharPositionInLine(),
                        "Segment start and end coordinates must be different");
            }
        }

        // width must be positive
        double width = Double.parseDouble(ctx.NUMBER(0).getText());
        if (width <= 0) {
            errorCollector.addSemanticError(
                    ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    "Segment width must be positive (got: " + width + ")");
        }

        // wind speed must be positive
        double windSpeed = Double.parseDouble(ctx.NUMBER(1).getText());
        if (windSpeed < 0) {
            errorCollector.addSemanticError(
                    ctx.getStart().getLine(),
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