package lprog;

import lprog.FlightPlanLexer;
import lprog.FlightPlanParser;
import lprog.ErrorCollector.ErrorType;
import lprog.ErrorCollector.ValidationError;
import lprog.FlightPlanBuilderListener.FlightData;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.util.List;

public class FlightPlanImporter {

    // ─────────────────────────────────────────
    // Result class
    // ─────────────────────────────────────────

    public static class FlightPlanImportResult {

        private final boolean success;
        private final List<FlightData> flights;
        private final List<ValidationError> errors;

        private FlightPlanImportResult(boolean success,
                                       List<FlightData> flights,
                                       List<ValidationError> errors) {
            this.success = success;
            this.flights = flights;
            this.errors  = errors;
        }

        public static FlightPlanImportResult success(List<FlightData> flights) {
            return new FlightPlanImportResult(true, flights, List.of());
        }

        public static FlightPlanImportResult failure(List<ValidationError> errors) {
            return new FlightPlanImportResult(false, List.of(), errors);
        }

        public boolean isSuccess()               { return success; }
        public List<FlightData> getFlights()     { return flights; }
        public List<ValidationError> getErrors() { return errors;  }
    }

    // ─────────────────────────────────────────
    // Import pipeline
    // ─────────────────────────────────────────

    public FlightPlanImportResult importFromFile(final String filePath) {
        final ErrorCollector errorCollector = new ErrorCollector();

        // ── Step 1: Lexical analysis ──────────────────────────────
        CharStream input;
        try {
            input = CharStreams.fromFileName(filePath);
        } catch (IOException e) {
            errorCollector.addSemanticError(0, 0,
                    "Cannot open file: " + filePath + " — " + e.getMessage());
            return FlightPlanImportResult.failure(errorCollector.getErrors());
        }

        errorCollector.setPhase(ErrorType.LEXICAL);
        FlightPlanLexer lexer = new FlightPlanLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorCollector);

        // ── Step 2: Syntactic analysis ────────────────────────────
        errorCollector.setPhase(ErrorType.SYNTACTIC);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FlightPlanParser parser = new FlightPlanParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorCollector);
        ParseTree tree = parser.flightPlan();

        if (errorCollector.hasErrors()) {
            return FlightPlanImportResult.failure(errorCollector.getErrors());
        }

        // ── Step 3: Semantic validation via Visitor ───────────────
        errorCollector.setPhase(ErrorType.SEMANTIC);
        FlightPlanSemanticVisitor visitor =
                new FlightPlanSemanticVisitor(errorCollector);
        visitor.visit(tree);

        if (errorCollector.hasErrors()) {
            return FlightPlanImportResult.failure(errorCollector.getErrors());
        }

        // ── Step 4: Build domain objects via Listener ─────────────
        FlightPlanBuilderListener builder = new FlightPlanBuilderListener();
        ParseTreeWalker.DEFAULT.walk(builder, tree);

        return FlightPlanImportResult.success(builder.getFlights());
    }

    // ─────────────────────────────────────────
    // Import from string (for testing)
    // ─────────────────────────────────────────

    public FlightPlanImportResult importFromString(final String input) {
        final ErrorCollector errorCollector = new ErrorCollector();

        // ── Step 1: Lexical analysis ──────────────────────────────
        errorCollector.setPhase(ErrorType.LEXICAL);
        CharStream charStream = CharStreams.fromString(input);
        FlightPlanLexer lexer = new FlightPlanLexer(charStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorCollector);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill(); // força o lexer a processar TODO o input agora

        if (errorCollector.hasErrors()) {
            return FlightPlanImportResult.failure(errorCollector.getErrors());
        }

        // ── Step 2: Syntactic analysis ────────────────────────────
        errorCollector.setPhase(ErrorType.SYNTACTIC);
        FlightPlanParser parser = new FlightPlanParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorCollector);
        ParseTree tree = parser.flightPlan();

        if (errorCollector.hasErrors()) {
            return FlightPlanImportResult.failure(errorCollector.getErrors());
        }

        // ── Step 3: Semantic validation via Visitor ───────────────
        errorCollector.setPhase(ErrorType.SEMANTIC);
        FlightPlanSemanticVisitor visitor =
                new FlightPlanSemanticVisitor(errorCollector);
        visitor.visit(tree);

        if (errorCollector.hasErrors()) {
            return FlightPlanImportResult.failure(errorCollector.getErrors());
        }

        // ── Step 4: Build domain objects via Listener ─────────────
        FlightPlanBuilderListener builder = new FlightPlanBuilderListener();
        ParseTreeWalker.DEFAULT.walk(builder, tree);

        return FlightPlanImportResult.success(builder.getFlights());
    }
}
