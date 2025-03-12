package Tran;
import AST.*;
import org.w3c.dom.ls.LSParserFilter;

import javax.swing.text.html.Option;
import java.util.*;

public class Parser {

    private TranNode ParserNode = new TranNode();
    private  TokenManager ParserManager;
    private int position;

    public Parser(TranNode top, List<Token> tokens)
    {
        this.ParserNode = top;
        this.ParserManager = new TokenManager(tokens);
    }

    public void Tran() throws SyntaxErrorException
    {
        while (!this.ParserManager.done())
        {
            if (getTokenTypeAt(position) == Token.TokenTypes.INTERFACE)
            {
                position++;
                this.ParserNode.Interfaces.add(ParseInterface());
                ClearFromHere();
            }
            else if (getTokenTypeAt(position) == Token.TokenTypes.CLASS)
            {
               this.ParserNode.Classes.add(ParseClass());
               ClearFromHere();
            }
            else if(getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE))
            {
                this.ParserManager.Remove(1);
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
        if(getTokenTypeAt(position).equals(Token.TokenTypes.WORD) && getTokenTypeAt(position + 1).equals(Token.TokenTypes.NEWLINE) && getTokenTypeAt(position + 2).equals(Token.TokenTypes.INDENT))
        {
            parserInterface.name = getTokenValueAt(position);
            position += 3;
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
        if(getTokenTypeAt(position) == Token.TokenTypes.DEDENT)
        {
            position++;
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
        if(!MatchTokens(new Token.TokenTypes[] {Token.TokenTypes.CLASS, Token.TokenTypes.WORD}))
        {
            throw new SyntaxErrorException("Improper class declaration", this.ParserManager.getCurrentLine(), this.ParserManager.getCurrentColumnNumber());
        }
        position++;
        parserClass.name = getTokenValueAt(position);
        position++;

        if (getTokenTypeAt(position).equals(Token.TokenTypes.IMPLEMENTS))
        {
            do
            {
                position++;
                parserClass.interfaces.add(getTokenValueAt(position));
                position++;
            }
            while(getTokenTypeAt(position).equals(Token.TokenTypes.COMMA));
        }
        if(!MatchTokens(new Token.TokenTypes[]{Token.TokenTypes.NEWLINE, Token.TokenTypes.INDENT}))
        {
            throw new SyntaxErrorException("Improper class declaration", this.ParserManager.getCurrentLine(), this.ParserManager.getCurrentColumnNumber());
        }
        position +=2;
        while (!getTokenTypeAt(position).equals(Token.TokenTypes.DEDENT))
        {
            if (getTokenTypeAt(position).equals(Token.TokenTypes.CONSTRUCT))
            {
                parserClass.constructors.add(ParseConstructor());
            }
            else {
                int temp = position;
                try {
                    parserClass.methods.add(ParseMethodDeclaration());
                }
                catch (Exception SyntaxErrorException)
                {
                    //if it cannot be parsed as a method Declaration it must be a member
                    position = temp;
                    parserClass.members.addAll(ParseMembers());
                }

            }
        }
        position++;
        return parserClass;
    }

    private ConstructorNode ParseConstructor() throws SyntaxErrorException
    {
        ConstructorNode parserConstructor = new ConstructorNode();
        List <StatementNode> Statements = new LinkedList<>();
        List <VariableDeclarationNode> Locals = new LinkedList<>();
        if(!MatchTokens(new Token.TokenTypes[]{Token.TokenTypes.CONSTRUCT, Token.TokenTypes.LPAREN}))
        {
            ExceptionHandler("Improper Constructor declaration");
        }
        position+= 2;
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.RPAREN))
        {
            parserConstructor.parameters.addAll(ParseParameterVariableDeclarations());
        }
        if(!MatchTokens(new Token.TokenTypes[]{Token.TokenTypes.RPAREN, Token.TokenTypes.NEWLINE}))
        {
            ExceptionHandler("Improper Constructor declaration");
        }
        position+= 2;
        ParseMethodBody(Statements, Locals);
        parserConstructor.locals.addAll(Locals);
        parserConstructor.statements.addAll(Statements);
        return parserConstructor;
    }

    private MethodDeclarationNode ParseMethodDeclaration() throws SyntaxErrorException
    {
        MethodDeclarationNode parserMethodDeclaration = new MethodDeclarationNode();
        MethodHeaderNode parserMethodHeader = new MethodHeaderNode();
        List <StatementNode> Statements = new LinkedList<>();
        List <VariableDeclarationNode> Locals = new LinkedList<>();
        parserMethodDeclaration.isPrivate = false;
        parserMethodDeclaration.isShared = false;
        if (getTokenTypeAt(position).equals(Token.TokenTypes.PRIVATE))
        {
            parserMethodDeclaration.isPrivate = true;
        }
        if(getTokenTypeAt(position).equals(Token.TokenTypes.SHARED))
        {
            parserMethodDeclaration.isShared = true;
        }
        parserMethodHeader = ParseMethodHeader();
        parserMethodDeclaration.name  = parserMethodHeader.name;
        parserMethodDeclaration.parameters = parserMethodHeader.parameters;
        parserMethodDeclaration.returns = parserMethodHeader.returns;
        ParseMethodBody(Statements, Locals);
        parserMethodDeclaration.locals = Locals;
        parserMethodDeclaration.statements = Statements;
        return parserMethodDeclaration;
    }

    private List <MemberNode> ParseMembers() throws SyntaxErrorException
    {
        List <MemberNode> parserMembers = new LinkedList<>();
        List <VariableDeclarationNode> parserVariableDeclarations = new LinkedList<>();
        parserVariableDeclarations = ParseVariableDeclarations();
        for ( VariableDeclarationNode variable : parserVariableDeclarations)
        {
            parserMembers.add(new MemberNode());
            parserMembers.getLast().declaration = variable;
        }
        return parserMembers;
    }

    private void ParseMethodBody(List <StatementNode> Statements, List <VariableDeclarationNode> Locals) throws SyntaxErrorException
    {
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.INDENT))
        {
            ExceptionHandler("missing Indent in method boddy");
        }
        position++;
        while (getTokenTypeAt(position).equals(Token.TokenTypes.IF) || getTokenTypeAt(position).equals(Token.TokenTypes.LOOP) || getTokenTypeAt(position).equals(Token.TokenTypes.WORD))
        {
            try
            {
                Locals.addAll(ParseVariableDeclarations());
            }
            catch (Exception SyntaxErrorException) {
                Statements.add(ParseStatement());
            }
        }

        if(!getTokenTypeAt(position).equals(Token.TokenTypes.DEDENT))
        {
            ExceptionHandler("missing Dendent in method boddy");
        }
        position++;
    }

    private StatementNode ParseStatement() throws SyntaxErrorException
    {
        StatementNode parserStatement;
        if(getTokenTypeAt(position).equals(Token.TokenTypes.IF))
        {
            parserStatement = ParseIf();
        }
        else if(getTokenTypeAt(position).equals(Token.TokenTypes.LOOP))
        {
            parserStatement = ParseLoop();
        }
        else
        {
            int temp = position;
            try {
                parserStatement = ParseMethodCall();
            }
            catch (Exception SyntexErrorException)
            {
                position = temp;
                parserStatement = ParseAssignment();
            }
        }
        return parserStatement;
    }

    private List <VariableDeclarationNode> ParseVariableDeclarations() throws SyntaxErrorException
    {
        List <VariableDeclarationNode> variables = new ArrayList<>();
        String dataType;
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.WORD))
        {
            ExceptionHandler("improper Variable declaration");
        }
        dataType = getTokenValueAt(position);
        do
        {
            position++;
            variables.add(parseVariableNameValue());
            variables.getLast().type = dataType;
            position++;
        }
        while (getTokenTypeAt(position).equals(Token.TokenTypes.COMMA));

        return variables;
    }

    private VariableDeclarationNode parseVariableNameValue() throws SyntaxErrorException
    {
        VariableDeclarationNode parserVariableDeclaration = new VariableDeclarationNode();
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.WORD))
        {
            ExceptionHandler("improper Variable declaration");
        }
        parserVariableDeclaration.name = getTokenValueAt(position);
        position++;
        if(getTokenTypeAt(position).equals(Token.TokenTypes.EQUAL))
        {
            position++;
            parserVariableDeclaration.initializer = Optional.of(ParseExpression());
        }
        return parserVariableDeclaration;
    }

    private IfNode ParseIf() throws SyntaxErrorException
    {
        IfNode parserIf = new IfNode();
        parserIf.condition = ParseBoolExpTerm();
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE))
        {
            ExceptionHandler("Missing Newline in if statement");
        }
        parserIf.statements.addAll(ParseStatements());
        if(getTokenTypeAt(position).equals(Token.TokenTypes.ELSE))
        {
            parserIf.elseStatement = ParseElse();
        }
        return parserIf;
    }

    private List<StatementNode> ParseStatements() throws SyntaxErrorException
    {
        List <StatementNode> parserStatements = new ArrayList<>();

        return parserStatements;
    }

    private Optional <ElseNode> ParseElse() throws SyntaxErrorException
    {
        ElseNode parseElse = new ElseNode();
        if(getTokenTypeAt(position).equals(Token.TokenTypes.INDENT))
        {
            parseElse.statements.addAll(ParseStatements());
        }
        else
        {
            parseElse.statements.add(ParseStatement());
        }
        return Optional.of(parseElse);
    }

    private LoopNode ParseLoop() throws SyntaxErrorException
    {
        LoopNode parserLoop = new LoopNode();
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.LOOP))
        {
            ExceptionHandler("Missing Loop keyword in loop statement (how???)");
        }
        position++;
        if(getTokenTypeAt(position + 1).equals(Token.TokenTypes.EQUAL))
        {
            parserLoop.assignment = Optional.of(ParseVariableReference());
        }
        parserLoop.expression = ParseBoolExpTerm();
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE))
        {
            ExceptionHandler("Loop statement is missing Newline");
        }
        return parserLoop;
    }
    //@TODO this piece of crap
    private MethodCallStatementNode ParseMethodCall() throws SyntaxErrorException
    {
        MethodCallStatementNode parserMethodCall = new MethodCallStatementNode();
        if(getTokenTypeAt(position + 1).equals(Token.TokenTypes.COMMA) || getTokenTypeAt(position + 1).equals(Token.TokenTypes.EQUAL))
        {
            do {
                parserMethodCall.returnValues.add(ParseVariableReference());
                position++;
            }
            while(getTokenTypeAt(position - 1).equals(Token.TokenTypes.COMMA));
            if(!getTokenTypeAt(position - 1).equals(Token.TokenTypes.EQUAL))
            {
                ExceptionHandler("missing assignment fror references in Method call");
            }
        }
        //ParseMethodCallExpression();
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE))
        {
            ExceptionHandler("Missing NEWLINE in Method call");
        }
        position++;
        return parserMethodCall;
    }

    private AssignmentNode ParseAssignment() throws SyntaxErrorException
    {
        AssignmentNode parserAssignment = new AssignmentNode();
        parserAssignment.target = ParseVariableReference();
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.EQUAL))
        {
            ExceptionHandler("Missing = sign for assignment declaration");
        }
        position++;
        parserAssignment.expression = ParseExpression();
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE))
        {
            ExceptionHandler("Missing NEWLINE in assignment expression");
        }
        return parserAssignment;
    }

    private MethodHeaderNode ParseMethodHeader() throws SyntaxErrorException
    {
        MethodHeaderNode parserMethodHeader = null;
        VariableDeclarationNode parserVariableDeclaration = new VariableDeclarationNode();

        if(getTokenTypeAt(position).equals(Token.TokenTypes.WORD))
        {
            parserMethodHeader = new MethodHeaderNode();
            parserMethodHeader.name = getTokenValueAt(position);
            position++;
            if(getTokenTypeAt(position).equals(Token.TokenTypes.LPAREN) && getTokenTypeAt(position + 1).equals(Token.TokenTypes.WORD))
            {
                position++;
                parserMethodHeader.parameters.addAll(ParseParameterVariableDeclarations());
                if(getTokenTypeAt(position) == Token.TokenTypes.RPAREN)
                {
                    position++;
                }
                else
                {
                    throw new SyntaxErrorException("Improper Method Initialization", this.ParserManager.getCurrentLine(), ParserManager.getCurrentColumnNumber());
                }

            }
            else if (MatchTokens(new Token.TokenTypes[]{Token.TokenTypes.LPAREN, Token.TokenTypes.RPAREN}))
            {
                position += 2;
            }
            else
            {
                throw new SyntaxErrorException("Improper Method Initialization", this.ParserManager.getCurrentLine(), ParserManager.getCurrentColumnNumber());
            }
            if (getTokenTypeAt(position) == Token.TokenTypes.COLON)
            {
                position++;
                parserMethodHeader.returns.addAll(ParseParameterVariableDeclarations());
            }
            while (getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE))
            {
                position++;
            }
            return parserMethodHeader;
        }
        return parserMethodHeader;
    }

    private ExpressionNode ParseBoolExpTerm() throws SyntaxErrorException
    {
        BooleanOpNode parserBool = new BooleanOpNode();
        try
        {
            //return ParseMethodCallExpression();
        }
        catch (Exception SyntaxErrorException)
        {
            try
            {
                parserBool.left = ParseExpression();
                parserBool.op = BooleanOpNode.BooleanOperations.valueOf(getTokenValueAt(position));
                position++;
                parserBool.right = ParseExpression();
            }
            catch (Exception SyntaxErrorException2)
            {
                return ParseVariableReference();
            }
        }
        return ParseVariableReference();
    }

    VariableReferenceNode ParseVariableReference() throws SyntaxErrorException
    {
        VariableReferenceNode parserVariableReference = new VariableReferenceNode();
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.WORD))
        {
            ExceptionHandler("improper Variable Reference");
        }
        parserVariableReference.name = getTokenValueAt(position);
        position++;
        return parserVariableReference;
    }

    private MathOpNode ParseExpression() throws SyntaxErrorException
    {
        MathOpNode parserExpression = new MathOpNode();
        parserExpression.left = ParseTerm();
        if(getTokenTypeAt(position).equals(Token.TokenTypes.PLUS) || getTokenTypeAt(position).equals(Token.TokenTypes.MINUS))
        {
            parserExpression.op = MathOpNode.MathOperations.valueOf(getTokenValueAt(position));
            position++;
            parserExpression.right = ParseTerm();
        }
        return parserExpression;
    }

    private MathOpNode ParseTerm() throws SyntaxErrorException
    {
        MathOpNode parserTerm = new MathOpNode();
        //parserTerm.left = ParseFactor();
        if(getTokenTypeAt(position).equals(Token.TokenTypes.DIVIDE) || getTokenTypeAt(position).equals(Token.TokenTypes.TIMES) || getTokenTypeAt(position).equals(Token.TokenTypes.MODULO))
        {
            parserTerm.op = MathOpNode.MathOperations.valueOf(getTokenValueAt(position));
            position++;
           // parserTerm.right = ParseFactor();
        }
        return  parserTerm;
    }


    private List<VariableDeclarationNode> ParseParameterVariableDeclarations() throws SyntaxErrorException
    {
        List <VariableDeclarationNode> DeclarationList = new LinkedList<>();

        DeclarationList.add(ParseParameterVariableDeclaration());
        while (getTokenTypeAt(position) == Token.TokenTypes.COMMA)
        {
            position++;
            DeclarationList.add(ParseParameterVariableDeclaration());
        }
        return DeclarationList;
    }

    private VariableDeclarationNode ParseParameterVariableDeclaration() throws SyntaxErrorException
    {
        VariableDeclarationNode parserVariableDeclarationNode = new VariableDeclarationNode();
        if(MatchTokens(new Token.TokenTypes[]{Token.TokenTypes.WORD, Token.TokenTypes.WORD}))
        {
            parserVariableDeclarationNode.type = getTokenValueAt(position);
            position++;
            parserVariableDeclarationNode.name = getTokenValueAt(position);
            position++;
        }
        else
        {
            throw new SyntaxErrorException("Improper variable declaration", ParserManager.getCurrentLine(), ParserManager.getCurrentColumnNumber());
        }
        return parserVariableDeclarationNode;
    }

    private Token.TokenTypes getTokenTypeAt(int position)
    {
        return this.ParserManager.peek(position).get().getType();
    }
    private String getTokenValueAt(int position)
    {
        return this.ParserManager.peek(position).get().getValue();
    }

    private void ClearFromHere()
    {
        while(!this.ParserManager.done() && position > 0)
        {
            this.ParserManager.Remove(1);
            position--;
        }
        position = 0;
    }

    private boolean MatchTokens(Token.TokenTypes[] types)
    {
        int counter = 0;
        for (Token.TokenTypes type : types)
        {
            if(!getTokenTypeAt(position + counter).equals(type))
            {
                return false;
            }
            counter++;
        }
        return true;
    }

    private void ExceptionHandler(String message) throws SyntaxErrorException
    {
        throw new SyntaxErrorException(message, this.ParserManager.peek(position).get().getLineNumber(), this.ParserManager.peek(position).get().getColumnNumber());
    }


}
