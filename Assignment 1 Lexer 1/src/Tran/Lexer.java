package Tran;
import java.util.LinkedList;
import java.util.List;

public class Lexer {

    private TextManager document = new TextManager("");
    private int line;
    private int column;


    public Lexer(String input)
    {
        this.document = new TextManager(input);
        this.line = 0;
        this.column = 0;
    }

    public List<Token> Lex() throws Exception
    {
        LinkedList<Token> TokenList = new LinkedList<Token>();
        while(!document.isAtEnd())
        {
            if(Character.isLetter(document.peekCharacter()))
            {
                TokenList.add(readWord());
            }
            if(Character.isDigit(document.peekCharacter()))
            {
                TokenList.add(readNumber());
            }
        }

        return TokenList;
    }

    public Token readWord() throws Exception
    {
        String buffer = "";
        while(Character.isLetter(document.peekCharacter()) || document.peekCharacter() == '.')
        {
            buffer = buffer.concat(Character.toString(document.getCharacter()));
            this.column++;
        }
        Token.TokenTypes Type = getType(buffer);

        return new Token(Token.TokenTypes.WORD, this.line, this.column, buffer);

    }

    public Token readNumber() throws  Exception
    {
        //reading document until it is no longer a number
        String buffer = "";
        while(Character.isDigit(document.peekCharacter()))
        {
            this.column++;
            buffer = buffer.concat(Character.toString(document.getCharacter()));
        }
        //checking if the buffer is a key word
        return new Token(Token.TokenTypes.WORD, this.line, this.column, buffer);

    }

    public Token readPunctuation() throws Exception
    {

    }
}
