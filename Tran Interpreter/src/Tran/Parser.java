package Tran;
import AST.*;
import org.w3c.dom.ls.LSParserFilter;

import java.util.LinkedList;
import java.util.List;

public class Parser {

    private TranNode ParserNode = new TranNode();
    private  TokenManager ParserManager;

    public Parser(TranNode top, List<Token> tokens)
    {
        this.ParserNode = top;
        this.ParserManager = new TokenManager(tokens);
    }

    public void Tran() throws SyntaxErrorException
    {
        while (!this.ParserManager.done())
        {
            if (this.ParserManager.peek(0).get().getType() == Token.TokenTypes.INTERFACE)
            {
                this.ParserManager.Remove(1);
                this.ParserNode.Interfaces.add(ParseInterface());
            }
            else if (this.ParserManager.peek(0).get().getType() == Token.TokenTypes.CLASS)
            {
               this.ParserNode.Classes.add(ParseClass());
            }
            else
            {
                throw new SyntaxErrorException("Tran does not begin with a class or interface", this.ParserManager.getCurrentLine(), ParserManager.getCurrentColumnNumber());
            }
        }
    }

    private InterfaceNode ParseInterface() throws SyntaxErrorException
    {
        InterfaceNode parserInterface = new InterfaceNode();
        MethodHeaderNode parseMethod = new MethodHeaderNode();
        if(this.ParserManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.NEWLINE) && this.ParserManager.peek(2).get().getType() == Token.TokenTypes.INDENT)
        {
            parserInterface.name = this.ParserManager.Remove(1).get().getValue();
            this.ParserManager.Remove(2);
            parseMethod = ParseMethodHeader();
            while (parseMethod != null)
            {
                parserInterface.methods.add(parseMethod);
                parseMethod = ParseMethodHeader();
            }
        }
        else
        {
            throw new SyntaxErrorException("Improper Interface Initializer", this.ParserManager.getCurrentLine(), this.ParserManager.getCurrentColumnNumber());
        }
        if(this.ParserManager.peek(0).get().getType() == Token.TokenTypes.DEDENT)
        {
            ParserManager.Remove(1);
            return parserInterface;
        }
        else
        {
            throw new SyntaxErrorException("Interface has no end", this.ParserManager.getCurrentLine(), this.ParserManager.getCurrentColumnNumber());
        }
    }

    private ClassNode ParseClass() throws SyntaxErrorException
    {
        ClassNode parserClass = new ClassNode();
        if(!this.ParserManager.nextTwoTokensMatch(Token.TokenTypes.CLASS, Token.TokenTypes.WORD))
        {
            throw new SyntaxErrorException("Improper class declaration", this.ParserManager.getCurrentLine(), this.ParserManager.getCurrentColumnNumber());
        }
        this.ParserManager.Remove(1);
        parserClass.name = this.ParserManager.Remove(1).get().getValue();

        if ()
        {}
        if (getCurrentTokenType().equals(Token.TokenTypes.IMPLEMENTS))
        {
            this.ParserManager.Remove(1);

        }
        if(this.ParserManager.nextTwoTokensMatch(Token.TokenTypes.NEWLINE, Token.TokenTypes.INDENT))
        {
            throw new SyntaxErrorException("Improper class declaration", this.ParserManager.getCurrentLine(), this.ParserManager.getCurrentColumnNumber());
        }
        this.ParserManager.Remove(2);
        while (!getCurrentTokenType().equals(Token.TokenTypes.DEDENT))
        {
            if (getCurrentTokenType().equals(Token.TokenTypes.CONSTRUCT))
            {
                parserClass.constructors.add(parseConstructor);
            }
            else if (getCurrentTokenType().equals(Token.TokenTypes.PRIVATE) || getCurrentTokenType().equals(Token.TokenTypes.SHARED) || getCurrentTokenType().equals(Token.TokenTypes.WORD))
            {
                parserClass.methods.add(parseMethodDeclaration);
            }
            else if(getCurrentTokenType().equals())
            {
                parserClass.members.add(parseMember);
            }
            else
            {

            }
        }


    }

    private MethodHeaderNode ParseMethodHeader() throws SyntaxErrorException
    {
        MethodHeaderNode parserMethodHeader = null;
        VariableDeclarationNode parserVariableDeclaration = new VariableDeclarationNode();
        while (this.ParserManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE)
        {
            ParserManager.Remove(1);
        }

        if(this.ParserManager.peek(0).get().getType() == Token.TokenTypes.WORD)
        {
            parserMethodHeader = new MethodHeaderNode();
            parserMethodHeader.name = this.ParserManager.peek(0).get().getValue();
            this.ParserManager.Remove(1);
            if(this.ParserManager.nextTwoTokensMatch(Token.TokenTypes.LPAREN, Token.TokenTypes.WORD))
            {
                this.ParserManager.Remove(1);
                parserMethodHeader.parameters.addAll(parseParameterVariableDeclorations());
                if(this.ParserManager.peek(0).get().getType() == Token.TokenTypes.RPAREN)
                {
                    ParserManager.Remove(1);
                }
                else
                {
                    throw new SyntaxErrorException("Improper Method Initialization", this.ParserManager.getCurrentLine(), ParserManager.getCurrentColumnNumber());
                }

            }
            else if (this.ParserManager.nextTwoTokensMatch(Token.TokenTypes.LPAREN, Token.TokenTypes.RPAREN))
            {
                this.ParserManager.Remove(2);
            }
            else
            {
                throw new SyntaxErrorException("Improper Method Initialization", this.ParserManager.getCurrentLine(), ParserManager.getCurrentColumnNumber());
            }
            if (this.ParserManager.peek(0).get().getType() == Token.TokenTypes.COLON)
            {
                ParserManager.Remove(1);
                parserMethodHeader.returns.addAll(parseParameterVariableDeclorations());
            }
            if (this.ParserManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE)
            {
                ParserManager.Remove(1);
                return parserMethodHeader;
            }
            else if(this.ParserManager.peek(0).get().getType() == Token.TokenTypes.DEDENT)
            {
                return parserMethodHeader;
            }
            else
            {
                throw new SyntaxErrorException("incorrect parameter declaration of method", this.ParserManager.getCurrentLine(), ParserManager.getCurrentColumnNumber());
            }
        }
        return parserMethodHeader;
    }

    private List<VariableDeclarationNode> parseParameterVariableDeclorations() throws SyntaxErrorException
    {
        List <VariableDeclarationNode> DeclarationList = new LinkedList<>();
        DeclarationList.add(parseParameterVariableDecloration());
        while (this.ParserManager.peek(0).get().getType() == Token.TokenTypes.COMMA)
        {
            ParserManager.Remove(1);
            DeclarationList.add(parseParameterVariableDecloration());
        }
        return DeclarationList;
    }

    private VariableDeclarationNode parseParameterVariableDecloration() throws SyntaxErrorException
    {
        VariableDeclarationNode parserVariableDeclarationNode = new VariableDeclarationNode();
        if(this.ParserManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD))
        {
            parserVariableDeclarationNode.type = this.ParserManager.Remove(1).get().getValue();
            parserVariableDeclarationNode.name = this.ParserManager.Remove(1).get().getValue();
        }
        else
        {
            throw new SyntaxErrorException("Improper variable declaration", ParserManager.getCurrentLine(), ParserManager.getCurrentColumnNumber());
        }
        return parserVariableDeclarationNode;
    }

    private Token.TokenTypes getCurrentTokenType()
    {
        return this.ParserManager.peek(0).get().getType();
    }


}
