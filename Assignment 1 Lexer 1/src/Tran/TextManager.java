package Tran;
public class TextManager {

    private String text = new String();
    private int position;
    public TextManager(String input)
    {
        text =  input;
        position = 0;
    }

    public boolean isAtEnd()
    {
        return position == this.text.length();
    }

    public char peekCharacter()
    {
            return text.charAt(position + 1);
    }

    public char peekCharacter(int dist)
    {
            return text.charAt(position + dist);
    }

    public char getCharacter()
    {
            position++;
            return text.charAt(position - 1);
    }
}
