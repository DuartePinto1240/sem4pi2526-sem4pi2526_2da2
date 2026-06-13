grammar FlightPlan;

// ─────────────────────────────────────────
// Parser Rules
// ─────────────────────────────────────────

flightPlan
    : flight+ EOF
    ;

flight
    : FLIGHT ID LBRACE
        TYPE COLON flightType
        leg+
      RBRACE
    ;

flightType
    : REGULAR
    | CHARTER
    ;

leg
    : LEG LBRACE
        departure
        arrival
        fuel
        segment+
      RBRACE
    ;

departure
    : DEPARTURE COLON airportCode date time
    ;

arrival
    : ARRIVAL COLON airportCode time
    ;

fuel
    : FUEL COLON NUMBER unit
    ;

segment
    : SEGMENT LBRACE
        PHASE COLON phaseType
        FROM COLON coordinates
        TO COLON coordinates
        ALTITUDE_RANGE COLON LBRACKET NUMBER COMMA NUMBER RBRACKET
        WIDTH COLON NUMBER METER
        WIND COLON NUMBER DEG NUMBER MS
      RBRACE
    ;

phaseType
    : CLIMB
    | CRUISE
    | DESCENT
    ;

coordinates
    : LPAREN NUMBER COMMA NUMBER RPAREN
    ;

airportCode
    : ID
    ;

date
    : DATE_LITERAL
    ;

time
    : TIME_LITERAL
    ;

unit
    : KG
    | LITER
    | METER
    | MS
    ;

// ─────────────────────────────────────────
// Lexer Rules
// ─────────────────────────────────────────

// Keywords (case-insensitive)
FLIGHT         : [Ff][Ll][Ii][Gg][Hh][Tt] ;
LEG            : [Ll][Ee][Gg] ;
DEPARTURE      : [Dd][Ee][Pp][Aa][Rr][Tt][Uu][Rr][Ee] ;
ARRIVAL        : [Aa][Rr][Rr][Ii][Vv][Aa][Ll] ;
SEGMENT        : [Ss][Ee][Gg][Mm][Ee][Nn][Tt] ;
FUEL           : [Ff][Uu][Ee][Ll] ;
TYPE           : [Tt][Yy][Pp][Ee] ;
FROM           : [Ff][Rr][Oo][Mm] ;
TO             : [Tt][Oo] ;
WIDTH          : [Ww][Ii][Dd][Tt][Hh] ;
WIND           : [Ww][Ii][Nn][Dd] ;
REGULAR        : [Rr][Ee][Gg][Uu][Ll][Aa][Rr] ;
CHARTER        : [Cc][Hh][Aa][Rr][Tt][Ee][Rr] ;
ALTITUDE_RANGE : [Aa][Ll][Tt][Ii][Tt][Uu][Dd][Ee]'_'[Rr][Aa][Nn][Gg][Ee] ;
PHASE          : [Pp][Hh][Aa][Ss][Ee] ;
CLIMB          : [Cc][Ll][Ii][Mm][Bb] ;
CRUISE         : [Cc][Rr][Uu][Ii][Ss][Ee] ;
DESCENT        : [Dd][Ee][Ss][Cc][Ee][Nn][Tt] ;

// Units
DEG   : [Dd][Ee][Gg] ;
MS    : 'm/s' ;
KG    : 'kg' ;
LITER : 'l' ;
METER : 'm' ;

// Literals
ID           : [A-Za-z][A-Za-z0-9_\-]* ;
NUMBER       : '-'? [0-9]+ ('.' [0-9]+)? ;
DATE_LITERAL : [0-9][0-9][0-9][0-9] '-' [0-9][0-9] '-' [0-9][0-9] ;
TIME_LITERAL : [0-9][0-9] ':' [0-9][0-9] ;

// Separators
LBRACE   : '{' ;
RBRACE   : '}' ;
LPAREN   : '(' ;
RPAREN   : ')' ;
LBRACKET : '[' ;
RBRACKET : ']' ;
COMMA    : ',' ;
COLON    : ':' ;
SEMI     : ';' ;

// Skip
WS      : [ \t\r\n]+ -> skip ;
COMMENT : '//' ~[\r\n]* -> skip ;