package Tran;
import java.util.List;
import java.util.Optional;

public class TokenManager {

    private List<Token> TokenList;

    public TokenManager(List<Token> tokens)
    {
        this.TokenList = tokens;
    }

    public boolean done()
    {
	    return TokenList.isEmpty();
    }

    public Optional<Token> matchAndRemove(Token.TokenTypes t)
    {
        if(this.TokenList.get(0).getType() == t)
        {
            return Optional.of(this.TokenList.remove(0));
        }

	    return Optional.empty();
    }

    public Optional<Token> Remove(int n)
    {
        for (int i = 0; i < n -1; i++)
        {
            this.TokenList.remove(0);
        }
        return Optional.of(this.TokenList.remove(0));
    }

    public Optional<Token> peek(int i)
    {
        if(i < TokenList.size())
        {
            return Optional.of(TokenList.get(i));
        }
        else
        {
            return Optional.of(TokenList.getLast());
        }
    }

    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second)
    {
	    return first == this.TokenList.get(0).getType() && second == this.TokenList.get(1).getType();
    }

    public boolean nextIsEither(Token.TokenTypes first, Token.TokenTypes second)
    {
	    return this.TokenList.get(0).getType() == first || this.TokenList.get(1).getType() == second;
    }

    public int getCurrentLine()
    {
            return this.TokenList.get(0).getLineNumber();
    }

    public int getCurrentColumnNumber()
    {
            return this.TokenList.get(0).getColumnNumber();
    }

}
