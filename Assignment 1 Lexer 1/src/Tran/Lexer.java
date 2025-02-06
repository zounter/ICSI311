package Tran;
import java.util.LinkedList;
import java.util.List;

public class Lexer {

    private String incomingText = new String();

    public Lexer(String input) {
        incomingText = input;
    }



    public List<Token> Lex() throws Exception
    {
        LinkedList<Token> TokenList = new LinkedList<Token>();
	    TextManager document = new TextManager(incomingText);
        String currentWord = new String();
        while(!document.isAtEnd())
        {
            if(Character.isLetter(document.peekCharacter()))
            {
               // TokenList.add(readword());
            }
            if(Character.isDigit(document.peekCharacter()))
            {
                //TokenList.add(readNumber());
            }
        }

        return TokenList;
    }



}
