package Tran;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Lexer {

    private TextManager document;
    private int line;
    private int column;
    private int  indents = 0;
    private int whitespace = 0;
    private LinkedList<Token> TokenList = new LinkedList<>();
    private HashMap<String, Token.TokenTypes> TokenMap = new HashMap<>(Map.ofEntries(
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
        this.line = 1;
        this.column = 1;
    }

    public List<Token> Lex() throws Exception
    {
            Token dummyToken;

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
            else if(document.peekCharacter() == '\n')
            {
                handleIndentation();
            }
            else if(document.peekCharacter() == '{')
            {
                skipComment();
            }
            else if(document.peekCharacter() == '\"')
            {
                TokenList.add(readString());
            }
            else
            {
                //checking if token can be found in the next 4 chars
                dummyToken = readPunctuation();
                //adds nothing if no token is found
                if (dummyToken.getType() != null) TokenList.add(dummyToken);
            }
        }
        //throwing all final dedents into TokenList
        if (indents != 0)
        {
            for (int counter = 0; counter < indents; counter++)
            {
                TokenList.add(new Token(Token.TokenTypes.DEDENT, this.line, this.column));
            }
        }

        return TokenList;
    }

    public Token readWord()
    {
        //reads a char from the document until it hits a char which is not a letter
        String buffer = "";
        while(!document.isAtEnd())
        {
            if(Character.isLetter(document.peekCharacter()) || Character.isDigit(document.peekCharacter()))
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
        return new Token(TokenMap.getOrDefault(buffer, Token.TokenTypes.WORD), this.line, this.column - buffer.length(), buffer);
    }

    public Token readNumber()
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
        return new Token(Token.TokenTypes.NUMBER, this.line, this.column - buffer.length(), buffer);
    }

    public Token readPunctuation()
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

    public Token readString()
    {
        //reads all string within a quoted string, creates a new quoted string token with the string read
        String buffer = "";
        document.skip(1);
        this.column++;
        while(!document.isAtEnd())
        {
            if(document.peekCharacter() == '\"') break;
            buffer = buffer.concat(Character.toString(document.getCharacter()));
        }
        document.skip(1);
        this.column++;
        return new Token(Token.TokenTypes.QUOTEDSTRING, this.line, this.column - buffer.length(), buffer);
    }

    public void skipComment()
    {
        //skips over comments by finding the first '}' endpoint in the document
        int layer = 1;
        if(!document.isAtEnd())
        {
            document.skip(1);
        }
        while(!document.isAtEnd())
        {
            if(document.peekCharacter() == '}')
            {
                layer--;
            }
            //nesting comment, another '}' endpoint must be found to exit the comment now
            else if (document.peekCharacter() == '{')
            {
                layer++;
            }
            if(layer <= 0)
            {
                break;
            }
            document.skip(1);
            this.column++;

        }
    }

    public void handleIndentation()
    {
        //adding newline to List
        this.line++;
        this.TokenList.add(new Token(Token.TokenTypes.NEWLINE, this.line, this.column, "\n"));
        document.skip(1);
        this.column = 1;
        //loops until the first non-whitespace char is found, counts every whitespace along the way
        while(!document.isAtEnd())
        {
            // \t is a tab, since tabs are equal to 4 whitespace in TRAN 4 whitespace will be added
            if(document.peekCharacter() == '\t')
            {
                whitespace += 4;
            }
            //no content in line exception
            else if(document.peekCharacter() == '\n')
            {
                whitespace = indents * 4;
                break;
            }
            else if(document.peekCharacter() != ' ')
            {
                break;
            }
            else
            {
                whitespace++;
            }
            document.skip(1);
            this.column++;
        }
        //checking if there is more or less whitespace than the prior line
        whitespace = whitespace/4;
        if(whitespace > indents)
        {
            this.TokenList.add(new Token(Token.TokenTypes.INDENT, this.line, this.column));
            indents = whitespace;
        }
        else if(whitespace < indents)
        {
            for(int counter = 0; counter < indents - whitespace; counter++)
            {
                this.TokenList.add(new Token(Token.TokenTypes.DEDENT, this.line, this.column));
            }
            indents = whitespace;
        }
        whitespace = 0;
    }

}
