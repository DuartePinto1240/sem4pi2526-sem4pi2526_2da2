// Generated from C:/Users/duart/Downloads/sem4pi2526-sem4pi2526_2da2/eapli.base/exemplo.core/src/main/antlr4/lprog/FlightPlan.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link FlightPlanParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface FlightPlanVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#flightPlan}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlightPlan(FlightPlanParser.FlightPlanContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#flight}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlight(FlightPlanParser.FlightContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#flightType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlightType(FlightPlanParser.FlightTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#leg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLeg(FlightPlanParser.LegContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#departure}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeparture(FlightPlanParser.DepartureContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#arrival}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrival(FlightPlanParser.ArrivalContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#fuel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuel(FlightPlanParser.FuelContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#segment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSegment(FlightPlanParser.SegmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#phaseType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPhaseType(FlightPlanParser.PhaseTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#coordinates}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoordinates(FlightPlanParser.CoordinatesContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#airportCode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAirportCode(FlightPlanParser.AirportCodeContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#date}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDate(FlightPlanParser.DateContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#time}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTime(FlightPlanParser.TimeContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlightPlanParser#unit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnit(FlightPlanParser.UnitContext ctx);
}