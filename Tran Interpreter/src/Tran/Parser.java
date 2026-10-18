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
            else if(this.ParserNode.Classes.isEmpty() && this.ParserNode.Interfaces.isEmpty())
            {
                throw new SyntaxErrorException("Tran does not begin with a class or interface", this.ParserManager.getCurrentLine(), ParserManager.getCurrentColumnNumber());
            }
            else
            {
                break;
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
            while (getTokenTypeAt(position).equals(Token.TokenTypes.WORD))
            {
                parserInterface.methods.add(ParseMethodHeader());
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
        while (getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE))
        {
            position++;
        }
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.INDENT))
        {
            ExceptionHandler("Missing Indentation in class body");
        }
        position ++;
        while (!getTokenTypeAt(position).equals(Token.TokenTypes.DEDENT))
        {
            while (getTokenTypeAt(position) == Token.TokenTypes.NEWLINE)
            {
                position++;
            }
            if (getTokenTypeAt(position).equals(Token.TokenTypes.CONSTRUCT))
            {
                parserClass.constructors.add(ParseConstructor());
            }
            else {
                int temp = position;
                try {
                    parserClass.methods.add(ParseMethodDeclaration());
                }
                catch (SyntaxErrorException e)
                {
                    //if it cannot be parsed as a method Declaration it must be a member
                    int fail1 = position;
                    position = temp;
                    try {
                        parserClass.members.addAll(ParseMembers());
                    }
                    catch (SyntaxErrorException f)
                    {
                        int fail2 = position;
                        int position = temp;
                        if (fail1 > fail2) ParseMethodDeclaration();
                        else ParseMembers();

                    }
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
            position++;
        }
        if(getTokenTypeAt(position).equals(Token.TokenTypes.SHARED))
        {
            parserMethodDeclaration.isShared = true;
            position++;
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
            ExceptionHandler("missing Indent in method body");
        }
        position++;
        while (getTokenTypeAt(position).equals(Token.TokenTypes.IF) || getTokenTypeAt(position).equals(Token.TokenTypes.LOOP) || getTokenTypeAt(position).equals(Token.TokenTypes.WORD))
        {
            int temp = position;
            try
            {
                Locals.addAll(ParseVariableDeclarations());
            }
            catch (SyntaxErrorException e) {
                int fail1 = position;
                position = temp;
                try {
                    Statements.add(ParseStatement());
                }
                catch (SyntaxErrorException f)
                {
                    int fail2 = position;
                    position = temp;
                    if (fail1 > fail2) Locals.addAll(ParseVariableDeclarations());
                    else Statements.add(ParseStatement());
                }
                }
            while(getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE))
            {
                position++;
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
            catch (SyntaxErrorException e) {
                int fail1 = position;
                position = temp;
                try {
                    parserStatement = ParseAssignment();
                } catch (SyntaxErrorException f)
                {
                    int fail2 = position;
                    position = temp;
                    if (fail1 > fail2) parserStatement = ParseMethodCall();
                    else parserStatement = ParseAssignment();
                    return parserStatement;
                }
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
        if(getTokenTypeAt(position).equals(Token.TokenTypes.ASSIGN))
        {
            position++;
            parserVariableDeclaration.initializer = Optional.of(ParseExpression());
        }
        return parserVariableDeclaration;
    }

    private IfNode ParseIf() throws SyntaxErrorException
    {
        IfNode parserIf = new IfNode();
        parserIf.statements = new ArrayList<>();
        parserIf.elseStatement = Optional.empty();
        position++;
        parserIf.condition = ParseBoolExpTerm();
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE))
        {
            ExceptionHandler("Missing Newline in if statement");
        }
        position++;
        parserIf.statements.addAll(ParseStatements());
        if(getTokenTypeAt(position).equals(Token.TokenTypes.ELSE))
        {
            parserIf.elseStatement = Optional.of(ParseElse());
        }
        return parserIf;
    }

    private List<StatementNode> ParseStatements() throws SyntaxErrorException
    {
        List <StatementNode> parserStatements = new ArrayList<>();
        //check indent
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.INDENT))
        {
            ExceptionHandler("Missing indentation");
        }
        position++;
        while(getTokenTypeAt(position).equals(Token.TokenTypes.LOOP) || getTokenTypeAt(position).equals(Token.TokenTypes.IF) || getTokenTypeAt(position).equals(Token.TokenTypes.WORD))
        {
                parserStatements.add(ParseStatement());
        }

        if(getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE))
        {
            position++;
        }
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.DEDENT))
        {
            ExceptionHandler("Missing dedent");
        }
        position++;

        //check dedent
        return parserStatements;
    }

    private ElseNode ParseElse() throws SyntaxErrorException
    {
        ElseNode parseElse = new ElseNode();
        parseElse.statements = new ArrayList<>();
        position++;

        if(!getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE))
        {
            ExceptionHandler("Missing NEWLINE in else statement");
        }
        position++;

        if(getTokenTypeAt(position).equals(Token.TokenTypes.INDENT))
        {
            parseElse.statements.addAll(ParseStatements());
        }
        else
        {
            parseElse.statements.add(ParseStatement());
        }
        return parseElse;
    }

    private LoopNode ParseLoop() throws SyntaxErrorException
    {
        LoopNode parserLoop = new LoopNode();
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.LOOP))
        {
            ExceptionHandler("Missing Loop keyword in loop statement (how???)");
        }
        position++;
        if(getTokenTypeAt(position + 1).equals(Token.TokenTypes.ASSIGN))
        {
            parserLoop.assignment = Optional.of(ParseVariableReference());
            position++;
        }
        else parserLoop.assignment = Optional.empty();
        parserLoop.expression = ParseBoolExpTerm();
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE))
        {
            ExceptionHandler("Loop statement is missing Newline");
        }
        position++;
        parserLoop.statements = ParseStatements();

        return parserLoop;
    }
    private MethodCallStatementNode ParseMethodCall() throws SyntaxErrorException
    {
        MethodCallStatementNode parserMethodCall = new MethodCallStatementNode();
        if(getTokenTypeAt(position + 1).equals(Token.TokenTypes.COMMA) || getTokenTypeAt(position + 1).equals(Token.TokenTypes.ASSIGN))
        {
            do {
                parserMethodCall.returnValues.add(ParseVariableReference());
                position++;
            }
            while(getTokenTypeAt(position - 1).equals(Token.TokenTypes.COMMA));
            if(!getTokenTypeAt(position - 1).equals(Token.TokenTypes.ASSIGN))
            {
                ExceptionHandler("missing assignment for references in Method call");
            }
        }
        MethodCallExpressionNode parserMethodCallExpression = ParseMethodCallExpression();
        parserMethodCall.methodName = parserMethodCallExpression.methodName;
        parserMethodCall.objectName = parserMethodCallExpression.objectName;
        parserMethodCall.parameters = parserMethodCallExpression.parameters;
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE) && !(getTokenTypeAt(position) == null))
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
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.ASSIGN))
        {
            ExceptionHandler("Missing = sign for assignment declaration");
        }
        position++;
        parserAssignment.expression = ParseExpression();
        if(!getTokenTypeAt(position).equals(Token.TokenTypes.NEWLINE) && !getTokenTypeAt(position).equals(Token.TokenTypes.DEDENT))
        {
            ExceptionHandler("Missing NEWLINE in assignment expression");
        }
        position++;
        return parserAssignment;
    }

    private MethodHeaderNode ParseMethodHeader() throws SyntaxErrorException
    {
        MethodHeaderNode parserMethodHeader = new MethodHeaderNode();
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

        }
        return parserMethodHeader;
    }

    private ExpressionNode ParseBoolExpTerm() throws SyntaxErrorException
    {
        int temp = position;
        BooleanOpNode parserBool = new BooleanOpNode();
        try
        {
            return ParseMethodCallExpression();
        }
        catch (SyntaxErrorException e)
        {
            int fail1 = position;
            position = temp;
            try
            {
                CompareNode parserCompare = new CompareNode();
                parserCompare.left = ParseExpression();
                switch (getTokenTypeAt(position))
                {
                    case LESSTHAN:
                        parserCompare.op = CompareNode.CompareOperations.lt;
                        break;
                        case GREATERTHAN:
                        parserCompare.op = CompareNode.CompareOperations.gt;
                        break;
                    case EQUAL:
                        parserCompare.op = CompareNode.CompareOperations.eq;
                        break;
                    case NOTEQUAL:
                        parserCompare.op = CompareNode.CompareOperations.ne;
                        break;
                    case LESSTHANEQUAL:
                        parserCompare.op = CompareNode.CompareOperations.le;
                        break;
                    case GREATERTHANEQUAL:
                        parserCompare.op = CompareNode.CompareOperations.ge;
                        break;
                    default:
                        ExceptionHandler("Improper Comparison opperator");
                }
                position++;
                parserCompare.right = ParseExpression();
                return parserCompare;
            }
            catch (SyntaxErrorException f)
            {
                int fail2 = position;
                position = temp;
                try {
                    return ParseVariableReference();
                }
                catch (SyntaxErrorException g)
                {
                    int fail3 = position;
                    position = temp;
                    if (fail1 > fail2 && fail1 > fail3) return  ParseMethodCallExpression();
                    else if (fail2 > fail3)
                    {
                        CompareNode parserCompare = new CompareNode();
                        parserCompare.left = ParseExpression();
                        switch (getTokenTypeAt(position))
                        {
                            case LESSTHAN:
                                parserCompare.op = CompareNode.CompareOperations.lt;
                                break;
                            case GREATERTHAN:
                                parserCompare.op = CompareNode.CompareOperations.gt;
                                break;
                            case EQUAL:
                                parserCompare.op = CompareNode.CompareOperations.eq;
                                break;
                            case NOTEQUAL:
                                parserCompare.op = CompareNode.CompareOperations.ne;
                                break;
                            case LESSTHANEQUAL:
                                parserCompare.op = CompareNode.CompareOperations.le;
                                break;
                            case GREATERTHANEQUAL:
                                parserCompare.op = CompareNode.CompareOperations.ge;
                                break;
                            default:
                                ExceptionHandler("Improper Comparison opperator");
                        }
                        position++;
                        parserCompare.right = ParseExpression();
                    }
                    else  return ParseVariableReference();
                    return ParseVariableReference();
                }
            }
        }
    }

    MethodCallExpressionNode ParseMethodCallExpression() throws SyntaxErrorException
    {
        MethodCallExpressionNode parserMethodCallExpression = new MethodCallExpressionNode();
        if (MatchTokens(new Token.TokenTypes[]{Token.TokenTypes.WORD, Token.TokenTypes.DOT}))
        {
            parserMethodCallExpression.objectName = Optional.of(getTokenValueAt(position));
            position += 2;
        }
        else parserMethodCallExpression.objectName = Optional.empty();
        if (!getTokenTypeAt(position).equals(Token.TokenTypes.WORD))
        {
            ExceptionHandler("Missing method name in method call");
        }
        parserMethodCallExpression.methodName = getTokenValueAt(position);
        position++;
        if (!getTokenTypeAt(position).equals(Token.TokenTypes.LPAREN))
        {
            ExceptionHandler("Missing Left parentheses in method call");
        }
        while(!getTokenTypeAt(position).equals(Token.TokenTypes.RPAREN))
        {
            position++;
            if (!getTokenTypeAt(position).equals(Token.TokenTypes.RPAREN)) {
                parserMethodCallExpression.parameters.add(ParseExpression());
                if (!getTokenTypeAt(position).equals(Token.TokenTypes.COMMA) && !getTokenTypeAt(position).equals(Token.TokenTypes.RPAREN))
                {
                    ExceptionHandler("Missing comma in message expression");
                }
            }
        }
        position++;
        return parserMethodCallExpression;
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
        if(getTokenTypeAt(position).equals(Token.TokenTypes.DOT) && getTokenTypeAt(position + 1).equals(Token.TokenTypes.WORD))
        {
            position++;
            parserVariableReference.name = parserVariableReference.name.concat(".".concat(getTokenValueAt(position)));
            position++;
        }
        return parserVariableReference;
    }

    private ExpressionNode ParseExpression() throws SyntaxErrorException {
        MathOpNode parserExpression = new MathOpNode();
        parserExpression.left = ParseTerm();
        while (getTokenTypeAt(position).equals(Token.TokenTypes.PLUS) || getTokenTypeAt(position).equals(Token.TokenTypes.MINUS))
        {
            if (getTokenTypeAt(position).equals(Token.TokenTypes.PLUS)) {
                parserExpression.op = MathOpNode.MathOperations.add;
                position++;
                parserExpression.right = ParseTerm();
            } else if (getTokenTypeAt(position).equals(Token.TokenTypes.MINUS)) {
                parserExpression.op = MathOpNode.MathOperations.subtract;
                position++;
                parserExpression.right = ParseTerm();
            }
            MathOpNode temp = parserExpression;
            parserExpression = new MathOpNode();
            parserExpression.left = temp;
        }
        return parserExpression.left;
    }

    private ExpressionNode ParseTerm() throws SyntaxErrorException
    {
        MathOpNode parserTerm = new MathOpNode();
        parserTerm.left = ParseFactor();
        while(getTokenTypeAt(position).equals(Token.TokenTypes.DIVIDE) || getTokenTypeAt(position).equals(Token.TokenTypes.TIMES) || getTokenTypeAt(position).equals(Token.TokenTypes.MODULO))
        {
            if (getTokenTypeAt(position).equals(Token.TokenTypes.DIVIDE)) {
                parserTerm.op = MathOpNode.MathOperations.divide;
                position++;
                parserTerm.right = ParseFactor();
            }
            else if (getTokenTypeAt(position).equals(Token.TokenTypes.TIMES))
            {
                parserTerm.op = MathOpNode.MathOperations.multiply;
                position++;
                parserTerm.right = ParseFactor();
            }
            else if (getTokenTypeAt(position).equals(Token.TokenTypes.MODULO)) {
                parserTerm.op = MathOpNode.MathOperations.modulo;
                position++;
                parserTerm.right = ParseFactor();
            }
            MathOpNode temp = parserTerm;
            parserTerm = new MathOpNode();
            parserTerm.left = temp;
        }
        return  parserTerm.left;
    }

    private ExpressionNode ParseFactor() throws SyntaxErrorException
    {
        ExpressionNode parserFactor = null;
        if(getTokenTypeAt(position).equals(Token.TokenTypes.WORD) && (getTokenTypeAt(position + 1).equals(Token.TokenTypes.DOT) || getTokenTypeAt(position + 1).equals(Token.TokenTypes.LPAREN)))
        {
            int temp = position;
            try {
                return ParseMethodCallExpression();
            }
            catch (SyntaxErrorException e)
            {
                int fail1 = position;
                position = temp;
                try {
                    return ParseVariableReference();
                }
                catch (SyntaxErrorException f)
                {
                    int fail2 = position;
                    if (fail1 > fail2) return ParseMethodCallExpression();
                    else return ParseVariableReference();
                }
            }
        }
        switch (getTokenTypeAt(position))
        {
            case Token.TokenTypes.NUMBER:
                NumericLiteralNode parserNumber = new NumericLiteralNode();
                parserNumber.value = Float.parseFloat(getTokenValueAt(position));
                parserFactor = parserNumber;
                position++;
                return parserNumber;
            case Token.TokenTypes.QUOTEDCHARACTER:
                CharLiteralNode parserCharacter = new CharLiteralNode();
                parserCharacter.value = getTokenValueAt(position).charAt(0);
                position++;
                return parserCharacter;
            case Token.TokenTypes.QUOTEDSTRING:
                StringLiteralNode parserString = new StringLiteralNode();
                parserString.value = getTokenValueAt(position);
                position++;
                return parserString;
            case Token.TokenTypes.LPAREN:
                position++;
                ExpressionNode temp = ParseExpression();
                if (getTokenTypeAt(position) != Token.TokenTypes.RPAREN)
                {
                    ExceptionHandler("Missing parentheses in Factor");
                }
                position++;
                return temp;
            case Token.TokenTypes.NEW:
                NewNode parserNewNode = new NewNode();
                position++;
                if(!getTokenTypeAt(position).equals(Token.TokenTypes.WORD))
                {
                    ExceptionHandler("Missing object declaration in object initialization");
                }
                parserNewNode.className = getTokenValueAt(position);
                position++;
                if(!getTokenTypeAt(position).equals(Token.TokenTypes.LPAREN))
                {
                    ExceptionHandler("Missing Left parentheses on object declaration");
                }
                while (!getTokenTypeAt(position).equals(Token.TokenTypes.RPAREN))
                {
                    position++;
                    if(!getTokenTypeAt(position).equals(Token.TokenTypes.RPAREN)) {
                        parserNewNode.parameters.add(ParseExpression());
                        if (!getTokenTypeAt(position).equals(Token.TokenTypes.COMMA) && !getTokenTypeAt(position).equals(Token.TokenTypes.RPAREN)) {
                            ExceptionHandler("Missing comma in object declaration");
                        }
                    }
                }
                position++;
                return parserNewNode;
            default:
                return ParseVariableReference();
        }
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
