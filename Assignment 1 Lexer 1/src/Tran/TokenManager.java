package Tran;
import java.util.List;
import java.util.Optional;

public class TokenManager {

    public TokenManager(List<Token> tokens) {
    }

    public boolean done() {
	    return false;
    }

    public Optional<Token> matchAndRemove(Token.TokenTypes t) {
	    return Optional.empty();
    }

    public Optional<Token> peek(int i) {
	    return Optional.empty();
    }

    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second) {
	    return false;
    }

    public boolean nextIsEither(Token.TokenTypes first, Token.TokenTypes second) {
	    return false;
    }

    public int getCurrentLine() {
            return -1;
    }

    public int getCurrentColumnNumber() {
            return -1;
    }
}
