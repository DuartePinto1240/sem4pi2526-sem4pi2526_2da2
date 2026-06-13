// Generated from C:/Users/duart/Downloads/sem4pi2526-sem4pi2526_2da2/eapli.base/exemplo.core/src/main/antlr4/lprog/FlightPlan.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link FlightPlanParser}.
 */
public interface FlightPlanListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#flightPlan}.
	 * @param ctx the parse tree
	 */
	void enterFlightPlan(FlightPlanParser.FlightPlanContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#flightPlan}.
	 * @param ctx the parse tree
	 */
	void exitFlightPlan(FlightPlanParser.FlightPlanContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#flight}.
	 * @param ctx the parse tree
	 */
	void enterFlight(FlightPlanParser.FlightContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#flight}.
	 * @param ctx the parse tree
	 */
	void exitFlight(FlightPlanParser.FlightContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#flightType}.
	 * @param ctx the parse tree
	 */
	void enterFlightType(FlightPlanParser.FlightTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#flightType}.
	 * @param ctx the parse tree
	 */
	void exitFlightType(FlightPlanParser.FlightTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#leg}.
	 * @param ctx the parse tree
	 */
	void enterLeg(FlightPlanParser.LegContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#leg}.
	 * @param ctx the parse tree
	 */
	void exitLeg(FlightPlanParser.LegContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#departure}.
	 * @param ctx the parse tree
	 */
	void enterDeparture(FlightPlanParser.DepartureContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#departure}.
	 * @param ctx the parse tree
	 */
	void exitDeparture(FlightPlanParser.DepartureContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#arrival}.
	 * @param ctx the parse tree
	 */
	void enterArrival(FlightPlanParser.ArrivalContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#arrival}.
	 * @param ctx the parse tree
	 */
	void exitArrival(FlightPlanParser.ArrivalContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#fuel}.
	 * @param ctx the parse tree
	 */
	void enterFuel(FlightPlanParser.FuelContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#fuel}.
	 * @param ctx the parse tree
	 */
	void exitFuel(FlightPlanParser.FuelContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#segment}.
	 * @param ctx the parse tree
	 */
	void enterSegment(FlightPlanParser.SegmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#segment}.
	 * @param ctx the parse tree
	 */
	void exitSegment(FlightPlanParser.SegmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#phaseType}.
	 * @param ctx the parse tree
	 */
	void enterPhaseType(FlightPlanParser.PhaseTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#phaseType}.
	 * @param ctx the parse tree
	 */
	void exitPhaseType(FlightPlanParser.PhaseTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#coordinates}.
	 * @param ctx the parse tree
	 */
	void enterCoordinates(FlightPlanParser.CoordinatesContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#coordinates}.
	 * @param ctx the parse tree
	 */
	void exitCoordinates(FlightPlanParser.CoordinatesContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#airportCode}.
	 * @param ctx the parse tree
	 */
	void enterAirportCode(FlightPlanParser.AirportCodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#airportCode}.
	 * @param ctx the parse tree
	 */
	void exitAirportCode(FlightPlanParser.AirportCodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#date}.
	 * @param ctx the parse tree
	 */
	void enterDate(FlightPlanParser.DateContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#date}.
	 * @param ctx the parse tree
	 */
	void exitDate(FlightPlanParser.DateContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#time}.
	 * @param ctx the parse tree
	 */
	void enterTime(FlightPlanParser.TimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#time}.
	 * @param ctx the parse tree
	 */
	void exitTime(FlightPlanParser.TimeContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlightPlanParser#unit}.
	 * @param ctx the parse tree
	 */
	void enterUnit(FlightPlanParser.UnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlightPlanParser#unit}.
	 * @param ctx the parse tree
	 */
	void exitUnit(FlightPlanParser.UnitContext ctx);
}