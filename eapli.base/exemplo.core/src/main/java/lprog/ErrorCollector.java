package lprog;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ErrorCollector extends BaseErrorListener {

    public enum ErrorType {
        LEXICAL,
        SYNTACTIC,
        SEMANTIC
    }

    public static class ValidationError {
        private final ErrorType type;
        private final int line;
        private final int column;
        private final String message;

        public ValidationError(ErrorType type, int line, int column, String message) {
            this.type    = type;
            this.line    = line;
            this.column  = column;
            this.message = message;
        }

        public ErrorType getType()    { return type;    }
        public int       getLine()    { return line;    }
        public int       getColumn()  { return column;  }
        public String    getMessage() { return message; }

        @Override
        public String toString() {
            return String.format("[%s] Line %d, Col %d — %s",
                    type, line, column, message);
        }
    }

    private final List<ValidationError> errors = new ArrayList<>();
    private ErrorType currentPhase = ErrorType.LEXICAL;

    public void setPhase(ErrorType phase) {
        this.currentPhase = phase;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        errors.add(new ValidationError(currentPhase, line, charPositionInLine, msg));
    }

    public void addSemanticError(int line, int column, String message) {
        errors.add(new ValidationError(ErrorType.SEMANTIC, line, column, message));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public void printErrors() {
        for (ValidationError e : errors) {
            System.out.println(e);
        }
    }
}
