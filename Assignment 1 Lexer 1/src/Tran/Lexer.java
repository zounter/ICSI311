package Tran;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Lexer {

    private TextManager document = new TextManager("");
    private int line;
    private int column;

    private HashMap<String, Token.TokenTypes> TokenMap = new HashMap<String, Token.TokenTypes>(Map.ofEntries(
            Map.entry("=", Token.TokenTypes.ASSIGN),
            Map.entry("(", Token.TokenTypes.LPAREN),
            Map.entry(")", Token.TokenTypes.RPAREN),
            Map.entry(":", Token.TokenTypes.COLON),
            Map.entry(".", Token.TokenTypes.DOT),
            Map.entry("+", Token.TokenTypes.PLUS),
            Map.entry("-", Token.TokenTypes.MINUS),
            Map.entry("*", Token.TokenTypes.TIMES),
            Map.entry("/", Token.TokenTypes.DIVIDE),
            Map.entry("%", Token.TokenTypes.MODULO),
            Map.entry(",", Token.TokenTypes.COMMA),
            Map.entry("==", Token.TokenTypes.EQUAL),
            Map.entry("!=", Token.TokenTypes.NOTEQUAL),
            Map.entry("<", Token.TokenTypes.LESSTHAN),
            Map.entry("<=", Token.TokenTypes.LESSTHANEQUAL),
            Map.entry(">", Token.TokenTypes.GREATERTHAN),
            Map.entry(">=", Token.TokenTypes.GREATERTHANEQUAL),
            Map.entry("implements", Token.TokenTypes.IMPLEMENTS),
            Map.entry("class", Token.TokenTypes.CLASS),
            Map.entry("interface", Token.TokenTypes.INTERFACE),
            Map.entry("loop", Token.TokenTypes.LOOP),
            Map.entry("if", Token.TokenTypes.IF),
            Map.entry("else", Token.TokenTypes.ELSE),
            Map.entry("    ", Token.TokenTypes.INDENT),
            Map.entry("\n", Token.TokenTypes.NEWLINE),
            Map.entry("new", Token.TokenTypes.NEW),
            Map.entry("private", Token.TokenTypes.PRIVATE),
            Map.entry("shared", Token.TokenTypes.SHARED),
            Map.entry("construct", Token.TokenTypes.CONSTRUCT)
    ));



    public Lexer(String input)
    {
        this.document = new TextManager(input);
        this.line = 0;
        this.column = 0;
    }

    public List<Token> Lex() throws Exception
    {
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
