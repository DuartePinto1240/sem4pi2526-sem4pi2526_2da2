// Generated from C:/Users/duart/Downloads/sem4pi2526-sem4pi2526_2da2/eapli.base/exemplo.core/src/main/antlr4/lprog/FlightPlan.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class FlightPlanParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		FLIGHT=1, LEG=2, DEPARTURE=3, ARRIVAL=4, SEGMENT=5, FUEL=6, TYPE=7, FROM=8, 
		TO=9, WIDTH=10, WIND=11, REGULAR=12, CHARTER=13, ALTITUDE_RANGE=14, PHASE=15, 
		CLIMB=16, CRUISE=17, DESCENT=18, DEG=19, MS=20, KG=21, LITER=22, METER=23, 
		ID=24, NUMBER=25, DATE_LITERAL=26, TIME_LITERAL=27, LBRACE=28, RBRACE=29, 
		LPAREN=30, RPAREN=31, LBRACKET=32, RBRACKET=33, COMMA=34, COLON=35, SEMI=36, 
		WS=37, COMMENT=38;
	public static final int
		RULE_flightPlan = 0, RULE_flight = 1, RULE_flightType = 2, RULE_leg = 3, 
		RULE_departure = 4, RULE_arrival = 5, RULE_fuel = 6, RULE_segment = 7, 
		RULE_phaseType = 8, RULE_coordinates = 9, RULE_airportCode = 10, RULE_date = 11, 
		RULE_time = 12, RULE_unit = 13;
	private static String[] makeRuleNames() {
		return new String[] {
			"flightPlan", "flight", "flightType", "leg", "departure", "arrival", 
			"fuel", "segment", "phaseType", "coordinates", "airportCode", "date", 
			"time", "unit"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, "'m/s'", "'kg'", "'l'", 
			"'m'", null, null, null, null, "'{'", "'}'", "'('", "')'", "'['", "']'", 
			"','", "':'", "';'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "FLIGHT", "LEG", "DEPARTURE", "ARRIVAL", "SEGMENT", "FUEL", "TYPE", 
			"FROM", "TO", "WIDTH", "WIND", "REGULAR", "CHARTER", "ALTITUDE_RANGE", 
			"PHASE", "CLIMB", "CRUISE", "DESCENT", "DEG", "MS", "KG", "LITER", "METER", 
			"ID", "NUMBER", "DATE_LITERAL", "TIME_LITERAL", "LBRACE", "RBRACE", "LPAREN", 
			"RPAREN", "LBRACKET", "RBRACKET", "COMMA", "COLON", "SEMI", "WS", "COMMENT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "FlightPlan.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public FlightPlanParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FlightPlanContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(FlightPlanParser.EOF, 0); }
		public List<FlightContext> flight() {
			return getRuleContexts(FlightContext.class);
		}
		public FlightContext flight(int i) {
			return getRuleContext(FlightContext.class,i);
		}
		public FlightPlanContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_flightPlan; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterFlightPlan(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitFlightPlan(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitFlightPlan(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FlightPlanContext flightPlan() throws RecognitionException {
		FlightPlanContext _localctx = new FlightPlanContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_flightPlan);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(29); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(28);
				flight();
				}
				}
				setState(31); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==FLIGHT );
			setState(33);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FlightContext extends ParserRuleContext {
		public TerminalNode FLIGHT() { return getToken(FlightPlanParser.FLIGHT, 0); }
		public TerminalNode ID() { return getToken(FlightPlanParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(FlightPlanParser.LBRACE, 0); }
		public TerminalNode TYPE() { return getToken(FlightPlanParser.TYPE, 0); }
		public TerminalNode COLON() { return getToken(FlightPlanParser.COLON, 0); }
		public FlightTypeContext flightType() {
			return getRuleContext(FlightTypeContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(FlightPlanParser.RBRACE, 0); }
		public List<LegContext> leg() {
			return getRuleContexts(LegContext.class);
		}
		public LegContext leg(int i) {
			return getRuleContext(LegContext.class,i);
		}
		public FlightContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_flight; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterFlight(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitFlight(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitFlight(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FlightContext flight() throws RecognitionException {
		FlightContext _localctx = new FlightContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_flight);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(35);
			match(FLIGHT);
			setState(36);
			match(ID);
			setState(37);
			match(LBRACE);
			setState(38);
			match(TYPE);
			setState(39);
			match(COLON);
			setState(40);
			flightType();
			setState(42); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(41);
				leg();
				}
				}
				setState(44); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==LEG );
			setState(46);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FlightTypeContext extends ParserRuleContext {
		public TerminalNode REGULAR() { return getToken(FlightPlanParser.REGULAR, 0); }
		public TerminalNode CHARTER() { return getToken(FlightPlanParser.CHARTER, 0); }
		public FlightTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_flightType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterFlightType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitFlightType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitFlightType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FlightTypeContext flightType() throws RecognitionException {
		FlightTypeContext _localctx = new FlightTypeContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_flightType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(48);
			_la = _input.LA(1);
			if ( !(_la==REGULAR || _la==CHARTER) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LegContext extends ParserRuleContext {
		public TerminalNode LEG() { return getToken(FlightPlanParser.LEG, 0); }
		public TerminalNode LBRACE() { return getToken(FlightPlanParser.LBRACE, 0); }
		public DepartureContext departure() {
			return getRuleContext(DepartureContext.class,0);
		}
		public ArrivalContext arrival() {
			return getRuleContext(ArrivalContext.class,0);
		}
		public FuelContext fuel() {
			return getRuleContext(FuelContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(FlightPlanParser.RBRACE, 0); }
		public List<SegmentContext> segment() {
			return getRuleContexts(SegmentContext.class);
		}
		public SegmentContext segment(int i) {
			return getRuleContext(SegmentContext.class,i);
		}
		public LegContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_leg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterLeg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitLeg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitLeg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LegContext leg() throws RecognitionException {
		LegContext _localctx = new LegContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_leg);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(50);
			match(LEG);
			setState(51);
			match(LBRACE);
			setState(52);
			departure();
			setState(53);
			arrival();
			setState(54);
			fuel();
			setState(56); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(55);
				segment();
				}
				}
				setState(58); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==SEGMENT );
			setState(60);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DepartureContext extends ParserRuleContext {
		public TerminalNode DEPARTURE() { return getToken(FlightPlanParser.DEPARTURE, 0); }
		public TerminalNode COLON() { return getToken(FlightPlanParser.COLON, 0); }
		public AirportCodeContext airportCode() {
			return getRuleContext(AirportCodeContext.class,0);
		}
		public DateContext date() {
			return getRuleContext(DateContext.class,0);
		}
		public TimeContext time() {
			return getRuleContext(TimeContext.class,0);
		}
		public DepartureContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_departure; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterDeparture(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitDeparture(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitDeparture(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DepartureContext departure() throws RecognitionException {
		DepartureContext _localctx = new DepartureContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_departure);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(62);
			match(DEPARTURE);
			setState(63);
			match(COLON);
			setState(64);
			airportCode();
			setState(65);
			date();
			setState(66);
			time();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArrivalContext extends ParserRuleContext {
		public TerminalNode ARRIVAL() { return getToken(FlightPlanParser.ARRIVAL, 0); }
		public TerminalNode COLON() { return getToken(FlightPlanParser.COLON, 0); }
		public AirportCodeContext airportCode() {
			return getRuleContext(AirportCodeContext.class,0);
		}
		public TimeContext time() {
			return getRuleContext(TimeContext.class,0);
		}
		public ArrivalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrival; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterArrival(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitArrival(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitArrival(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrivalContext arrival() throws RecognitionException {
		ArrivalContext _localctx = new ArrivalContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_arrival);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(68);
			match(ARRIVAL);
			setState(69);
			match(COLON);
			setState(70);
			airportCode();
			setState(71);
			time();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FuelContext extends ParserRuleContext {
		public TerminalNode FUEL() { return getToken(FlightPlanParser.FUEL, 0); }
		public TerminalNode COLON() { return getToken(FlightPlanParser.COLON, 0); }
		public TerminalNode NUMBER() { return getToken(FlightPlanParser.NUMBER, 0); }
		public UnitContext unit() {
			return getRuleContext(UnitContext.class,0);
		}
		public FuelContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fuel; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterFuel(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitFuel(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitFuel(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FuelContext fuel() throws RecognitionException {
		FuelContext _localctx = new FuelContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_fuel);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(73);
			match(FUEL);
			setState(74);
			match(COLON);
			setState(75);
			match(NUMBER);
			setState(76);
			unit();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SegmentContext extends ParserRuleContext {
		public TerminalNode SEGMENT() { return getToken(FlightPlanParser.SEGMENT, 0); }
		public TerminalNode LBRACE() { return getToken(FlightPlanParser.LBRACE, 0); }
		public TerminalNode PHASE() { return getToken(FlightPlanParser.PHASE, 0); }
		public List<TerminalNode> COLON() { return getTokens(FlightPlanParser.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(FlightPlanParser.COLON, i);
		}
		public PhaseTypeContext phaseType() {
			return getRuleContext(PhaseTypeContext.class,0);
		}
		public TerminalNode FROM() { return getToken(FlightPlanParser.FROM, 0); }
		public List<CoordinatesContext> coordinates() {
			return getRuleContexts(CoordinatesContext.class);
		}
		public CoordinatesContext coordinates(int i) {
			return getRuleContext(CoordinatesContext.class,i);
		}
		public TerminalNode TO() { return getToken(FlightPlanParser.TO, 0); }
		public TerminalNode ALTITUDE_RANGE() { return getToken(FlightPlanParser.ALTITUDE_RANGE, 0); }
		public TerminalNode LBRACKET() { return getToken(FlightPlanParser.LBRACKET, 0); }
		public List<TerminalNode> NUMBER() { return getTokens(FlightPlanParser.NUMBER); }
		public TerminalNode NUMBER(int i) {
			return getToken(FlightPlanParser.NUMBER, i);
		}
		public TerminalNode COMMA() { return getToken(FlightPlanParser.COMMA, 0); }
		public TerminalNode RBRACKET() { return getToken(FlightPlanParser.RBRACKET, 0); }
		public TerminalNode WIDTH() { return getToken(FlightPlanParser.WIDTH, 0); }
		public TerminalNode METER() { return getToken(FlightPlanParser.METER, 0); }
		public TerminalNode WIND() { return getToken(FlightPlanParser.WIND, 0); }
		public TerminalNode DEG() { return getToken(FlightPlanParser.DEG, 0); }
		public TerminalNode MS() { return getToken(FlightPlanParser.MS, 0); }
		public TerminalNode RBRACE() { return getToken(FlightPlanParser.RBRACE, 0); }
		public SegmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_segment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterSegment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitSegment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitSegment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SegmentContext segment() throws RecognitionException {
		SegmentContext _localctx = new SegmentContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_segment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(78);
			match(SEGMENT);
			setState(79);
			match(LBRACE);
			setState(80);
			match(PHASE);
			setState(81);
			match(COLON);
			setState(82);
			phaseType();
			setState(83);
			match(FROM);
			setState(84);
			match(COLON);
			setState(85);
			coordinates();
			setState(86);
			match(TO);
			setState(87);
			match(COLON);
			setState(88);
			coordinates();
			setState(89);
			match(ALTITUDE_RANGE);
			setState(90);
			match(COLON);
			setState(91);
			match(LBRACKET);
			setState(92);
			match(NUMBER);
			setState(93);
			match(COMMA);
			setState(94);
			match(NUMBER);
			setState(95);
			match(RBRACKET);
			setState(96);
			match(WIDTH);
			setState(97);
			match(COLON);
			setState(98);
			match(NUMBER);
			setState(99);
			match(METER);
			setState(100);
			match(WIND);
			setState(101);
			match(COLON);
			setState(102);
			match(NUMBER);
			setState(103);
			match(DEG);
			setState(104);
			match(NUMBER);
			setState(105);
			match(MS);
			setState(106);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PhaseTypeContext extends ParserRuleContext {
		public TerminalNode CLIMB() { return getToken(FlightPlanParser.CLIMB, 0); }
		public TerminalNode CRUISE() { return getToken(FlightPlanParser.CRUISE, 0); }
		public TerminalNode DESCENT() { return getToken(FlightPlanParser.DESCENT, 0); }
		public PhaseTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_phaseType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterPhaseType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitPhaseType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitPhaseType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PhaseTypeContext phaseType() throws RecognitionException {
		PhaseTypeContext _localctx = new PhaseTypeContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_phaseType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(108);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 458752L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CoordinatesContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(FlightPlanParser.LPAREN, 0); }
		public List<TerminalNode> NUMBER() { return getTokens(FlightPlanParser.NUMBER); }
		public TerminalNode NUMBER(int i) {
			return getToken(FlightPlanParser.NUMBER, i);
		}
		public TerminalNode COMMA() { return getToken(FlightPlanParser.COMMA, 0); }
		public TerminalNode RPAREN() { return getToken(FlightPlanParser.RPAREN, 0); }
		public CoordinatesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_coordinates; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterCoordinates(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitCoordinates(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitCoordinates(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CoordinatesContext coordinates() throws RecognitionException {
		CoordinatesContext _localctx = new CoordinatesContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_coordinates);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(110);
			match(LPAREN);
			setState(111);
			match(NUMBER);
			setState(112);
			match(COMMA);
			setState(113);
			match(NUMBER);
			setState(114);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AirportCodeContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(FlightPlanParser.ID, 0); }
		public AirportCodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_airportCode; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterAirportCode(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitAirportCode(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitAirportCode(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AirportCodeContext airportCode() throws RecognitionException {
		AirportCodeContext _localctx = new AirportCodeContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_airportCode);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(116);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DateContext extends ParserRuleContext {
		public TerminalNode DATE_LITERAL() { return getToken(FlightPlanParser.DATE_LITERAL, 0); }
		public DateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_date; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterDate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitDate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitDate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DateContext date() throws RecognitionException {
		DateContext _localctx = new DateContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_date);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(118);
			match(DATE_LITERAL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TimeContext extends ParserRuleContext {
		public TerminalNode TIME_LITERAL() { return getToken(FlightPlanParser.TIME_LITERAL, 0); }
		public TimeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_time; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterTime(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitTime(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitTime(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TimeContext time() throws RecognitionException {
		TimeContext _localctx = new TimeContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_time);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(120);
			match(TIME_LITERAL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnitContext extends ParserRuleContext {
		public TerminalNode KG() { return getToken(FlightPlanParser.KG, 0); }
		public TerminalNode LITER() { return getToken(FlightPlanParser.LITER, 0); }
		public TerminalNode METER() { return getToken(FlightPlanParser.METER, 0); }
		public TerminalNode MS() { return getToken(FlightPlanParser.MS, 0); }
		public UnitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).enterUnit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlightPlanListener ) ((FlightPlanListener)listener).exitUnit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlightPlanVisitor ) return ((FlightPlanVisitor<? extends T>)visitor).visitUnit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnitContext unit() throws RecognitionException {
		UnitContext _localctx = new UnitContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_unit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(122);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 15728640L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u0001&}\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002\u0002"+
		"\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002\u0005"+
		"\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002\b\u0007"+
		"\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002\f\u0007"+
		"\f\u0002\r\u0007\r\u0001\u0000\u0004\u0000\u001e\b\u0000\u000b\u0000\f"+
		"\u0000\u001f\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0004\u0001+\b\u0001"+
		"\u000b\u0001\f\u0001,\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0004\u00039\b\u0003\u000b\u0003\f\u0003:\u0001\u0003\u0001\u0003\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001\u000b\u0001\u000b"+
		"\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0000\u0000\u000e\u0000\u0002"+
		"\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u0000\u0003"+
		"\u0001\u0000\f\r\u0001\u0000\u0010\u0012\u0001\u0000\u0014\u0017q\u0000"+
		"\u001d\u0001\u0000\u0000\u0000\u0002#\u0001\u0000\u0000\u0000\u00040\u0001"+
		"\u0000\u0000\u0000\u00062\u0001\u0000\u0000\u0000\b>\u0001\u0000\u0000"+
		"\u0000\nD\u0001\u0000\u0000\u0000\fI\u0001\u0000\u0000\u0000\u000eN\u0001"+
		"\u0000\u0000\u0000\u0010l\u0001\u0000\u0000\u0000\u0012n\u0001\u0000\u0000"+
		"\u0000\u0014t\u0001\u0000\u0000\u0000\u0016v\u0001\u0000\u0000\u0000\u0018"+
		"x\u0001\u0000\u0000\u0000\u001az\u0001\u0000\u0000\u0000\u001c\u001e\u0003"+
		"\u0002\u0001\u0000\u001d\u001c\u0001\u0000\u0000\u0000\u001e\u001f\u0001"+
		"\u0000\u0000\u0000\u001f\u001d\u0001\u0000\u0000\u0000\u001f \u0001\u0000"+
		"\u0000\u0000 !\u0001\u0000\u0000\u0000!\"\u0005\u0000\u0000\u0001\"\u0001"+
		"\u0001\u0000\u0000\u0000#$\u0005\u0001\u0000\u0000$%\u0005\u0018\u0000"+
		"\u0000%&\u0005\u001c\u0000\u0000&\'\u0005\u0007\u0000\u0000\'(\u0005#"+
		"\u0000\u0000(*\u0003\u0004\u0002\u0000)+\u0003\u0006\u0003\u0000*)\u0001"+
		"\u0000\u0000\u0000+,\u0001\u0000\u0000\u0000,*\u0001\u0000\u0000\u0000"+
		",-\u0001\u0000\u0000\u0000-.\u0001\u0000\u0000\u0000./\u0005\u001d\u0000"+
		"\u0000/\u0003\u0001\u0000\u0000\u000001\u0007\u0000\u0000\u00001\u0005"+
		"\u0001\u0000\u0000\u000023\u0005\u0002\u0000\u000034\u0005\u001c\u0000"+
		"\u000045\u0003\b\u0004\u000056\u0003\n\u0005\u000068\u0003\f\u0006\u0000"+
		"79\u0003\u000e\u0007\u000087\u0001\u0000\u0000\u00009:\u0001\u0000\u0000"+
		"\u0000:8\u0001\u0000\u0000\u0000:;\u0001\u0000\u0000\u0000;<\u0001\u0000"+
		"\u0000\u0000<=\u0005\u001d\u0000\u0000=\u0007\u0001\u0000\u0000\u0000"+
		">?\u0005\u0003\u0000\u0000?@\u0005#\u0000\u0000@A\u0003\u0014\n\u0000"+
		"AB\u0003\u0016\u000b\u0000BC\u0003\u0018\f\u0000C\t\u0001\u0000\u0000"+
		"\u0000DE\u0005\u0004\u0000\u0000EF\u0005#\u0000\u0000FG\u0003\u0014\n"+
		"\u0000GH\u0003\u0018\f\u0000H\u000b\u0001\u0000\u0000\u0000IJ\u0005\u0006"+
		"\u0000\u0000JK\u0005#\u0000\u0000KL\u0005\u0019\u0000\u0000LM\u0003\u001a"+
		"\r\u0000M\r\u0001\u0000\u0000\u0000NO\u0005\u0005\u0000\u0000OP\u0005"+
		"\u001c\u0000\u0000PQ\u0005\u000f\u0000\u0000QR\u0005#\u0000\u0000RS\u0003"+
		"\u0010\b\u0000ST\u0005\b\u0000\u0000TU\u0005#\u0000\u0000UV\u0003\u0012"+
		"\t\u0000VW\u0005\t\u0000\u0000WX\u0005#\u0000\u0000XY\u0003\u0012\t\u0000"+
		"YZ\u0005\u000e\u0000\u0000Z[\u0005#\u0000\u0000[\\\u0005 \u0000\u0000"+
		"\\]\u0005\u0019\u0000\u0000]^\u0005\"\u0000\u0000^_\u0005\u0019\u0000"+
		"\u0000_`\u0005!\u0000\u0000`a\u0005\n\u0000\u0000ab\u0005#\u0000\u0000"+
		"bc\u0005\u0019\u0000\u0000cd\u0005\u0017\u0000\u0000de\u0005\u000b\u0000"+
		"\u0000ef\u0005#\u0000\u0000fg\u0005\u0019\u0000\u0000gh\u0005\u0013\u0000"+
		"\u0000hi\u0005\u0019\u0000\u0000ij\u0005\u0014\u0000\u0000jk\u0005\u001d"+
		"\u0000\u0000k\u000f\u0001\u0000\u0000\u0000lm\u0007\u0001\u0000\u0000"+
		"m\u0011\u0001\u0000\u0000\u0000no\u0005\u001e\u0000\u0000op\u0005\u0019"+
		"\u0000\u0000pq\u0005\"\u0000\u0000qr\u0005\u0019\u0000\u0000rs\u0005\u001f"+
		"\u0000\u0000s\u0013\u0001\u0000\u0000\u0000tu\u0005\u0018\u0000\u0000"+
		"u\u0015\u0001\u0000\u0000\u0000vw\u0005\u001a\u0000\u0000w\u0017\u0001"+
		"\u0000\u0000\u0000xy\u0005\u001b\u0000\u0000y\u0019\u0001\u0000\u0000"+
		"\u0000z{\u0007\u0002\u0000\u0000{\u001b\u0001\u0000\u0000\u0000\u0003"+
		"\u001f,:";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}