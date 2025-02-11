package Tran;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Lexer {

    private TextManager document = new TextManager("");
    private int line;
    private int column;

    private HashMap<String, Token.TokenTypes> TokenMap = new HashMap<String, Token.TokenTypes>();

    public void buildTokenMap()
    {
        TokenMap.put("=", Token.TokenTypes.ASSIGN);
        TokenMap.put("(", Token.TokenTypes.LPAREN);
        TokenMap.put(")", Token.TokenTypes.RPAREN);
        TokenMap.put(":", Token.TokenTypes.COLON);
        TokenMap.put(".", Token.TokenTypes.DOT);
        TokenMap.put("+", Token.TokenTypes.PLUS);
        TokenMap.put("-", Token.TokenTypes.MINUS);
        TokenMap.put("*", Token.TokenTypes.TIMES);
        TokenMap.put("/", Token.TokenTypes.DIVIDE);
        TokenMap.put("%", Token.TokenTypes.MODULO);
        TokenMap.put(",", Token.TokenTypes.COMMA);
        TokenMap.put("==", Token.TokenTypes.EQUAL);
        TokenMap.put("!=", Token.TokenTypes.NOTEQUAL);
        TokenMap.put("<", Token.TokenTypes.LESSTHAN);
        TokenMap.put("<=", Token.TokenTypes.LESSTHANEQUAL);
        TokenMap.put(">", Token.TokenTypes.GREATERTHAN);
        TokenMap.put(">=", Token.TokenTypes.GREATERTHANEQUAL);
        TokenMap.put("implements", Token.TokenTypes.IMPLEMENTS);
        TokenMap.put("class", Token.TokenTypes.CLASS);
        TokenMap.put("interface", Token.TokenTypes.INTERFACE);
        TokenMap.put("loop", Token.TokenTypes.LOOP);
        TokenMap.put("if", Token.TokenTypes.IF);
        TokenMap.put("else", Token.TokenTypes.ELSE);
        TokenMap.put("    ", Token.TokenTypes.INDENT);
        TokenMap.put("\n", Token.TokenTypes.NEWLINE);
        TokenMap.put("new", Token.TokenTypes.NEW);
        TokenMap.put("private", Token.TokenTypes.PRIVATE);
        TokenMap.put("shared", Token.TokenTypes.SHARED);
        TokenMap.put("construct", Token.TokenTypes.CONSTRUCT);
    }

    public Lexer(String input)
    {
        this.document = new TextManager(input);
        this.line = 0;
        this.column = 0;
    }

    public List<Token> Lex() throws Exception
    {
        buildTokenMap();
        Token dummyToken;
        LinkedList<Token> TokenList = new LinkedList<Token>();
        while(!document.isAtEnd())
        {
            if(Character.isLetter(document.peekCharacter() ))
            {
                TokenList.add(readWord());
            }
            else if(Character.isDigit(document.peekCharacter()) || (document.peekCharacter() == '.' && Character.isDigit(document.peekCharacter(1))))
            {
                TokenList.add(readNumber());
            }
            else
            {
                //checking if token can be found in the next 4 chars
                dummyToken = readPunctuation();
                //adds nothing if no token is found
                if (dummyToken.getType() != null) TokenList.add(dummyToken);
            }

        }
        return TokenList;
    }

    public Token readWord() throws Exception
    {
        //reads a char from the document until it hits a char which is not a letter
        String buffer = "";
        while(!document.isAtEnd())
        {
            if(Character.isLetter(document.peekCharacter()))
            {
                buffer = buffer.concat(Character.toString(document.getCharacter()));
                this.column++;
            }
            else
            {
                break;
            }
        }
        //checking for keywords, sending buffer as a word/keyword token
        if (TokenMap.containsKey(buffer))return new Token(TokenMap.get(buffer), this.line, this.column - buffer.length(), buffer);
        else return new Token(Token.TokenTypes.WORD, this.line, this.column - buffer.length(), buffer);
    }

    public Token readNumber() throws  Exception
    {
        //reading document until it is no longer a number
        String buffer = "";
        boolean noSymbolYet = true;
        while(!document.isAtEnd())
        {
            if(Character.isDigit(document.peekCharacter()))
            {
                this.column++;
                buffer = buffer.concat(Character.toString(document.getCharacter()));
            }
            //. has been hit bool active, one more . and the number will be finished
            else if(document.peekCharacter() == '.' && noSymbolYet)
            {
                this.column++;
                noSymbolYet = false;
                buffer = buffer.concat(Character.toString(document.getCharacter()));
            }
            else
            {
                break;
            }
        }

        System.out.println(buffer);
        return new Token(Token.TokenTypes.NUMBER, this.line, this.column - buffer.length(), buffer);
    }

    public Token readPunctuation() throws Exception
    {
        //filling buffer with 4 chars
        String buffer = "";
        for(int counter = 0; counter < 4; counter++)
        {
            buffer = buffer.concat(Character.toString(document.peekCharacter(counter)));
        }
        //checking if a keyword matches buffer
        if(TokenMap.containsKey(buffer))
        {
            document.skip(4);
            return new Token(TokenMap.get(buffer), this.line, this.column, buffer);
        }
        //filling buffer with 2 chars
        buffer = "";
        for(int counter = 0; counter < 2; counter++)
        {
            buffer = buffer.concat(Character.toString(document.peekCharacter(counter)));
        }
        //checking if a keword matches buffer
        if(TokenMap.containsKey(buffer))
        {
            document.skip(2);
            return new Token(TokenMap.get(buffer), this.line, this.column, buffer);
        }
        //treating character as a keyword (if it doesn't exist it will return as a null token)
        buffer = "";
        buffer = buffer.concat(Character.toString(document.peekCharacter()));
        document.skip(1);
        return new Token(TokenMap.get(buffer), this.line, this.column, buffer);
    }

}
