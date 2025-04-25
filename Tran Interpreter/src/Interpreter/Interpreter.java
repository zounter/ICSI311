package Interpreter;

import AST.*;
import Tran.Lexer;

import java.util.*;

public class Interpreter {
    TranNode parsedCode = new TranNode();

    private HashMap<String, BuiltInMethodDeclarationNode> builtIns = new HashMap<>();
    private List<String> BuiltInObjects = new LinkedList<>();
    private boolean isBuiltIn = false;


    /** Constructor - get the interpreter ready to run. Set members from parameters and "prepare" the class.
     *
     * Store the tran node.
     * Add any built-in methods to the AST
     * @param top - the head of the AST
     */
    public Interpreter(TranNode top) {
        parsedCode = top;
    }

    /**
     * This is the public interface to the interpreter. After parsing, we will create an interpreter and call start to
     * start interpreting the code.
     *
     * Search the classes in Tran for a method that is "isShared", named "start", that is not private and has no parameters
     * Call "InterpretMethodCall" on that method, then return.
     * Throw an exception if no such method exists.
     */
    public void start() {
        // putting the pre defs here
        builtIns.put("console.write", new ConsoleWrite());
	    for (ClassNode curClass : parsedCode.Classes)
        {
            for(MethodDeclarationNode curMethod :curClass.methods)
            {
                if(curMethod.isShared && curMethod.name.equals("start"))
                {
                    interpretMethodCall(Optional.of(new ObjectIDT(curClass)), curMethod, new LinkedList<InterpreterDataType>());
                    return;
                }
            }
        }
        throw new RuntimeException("No 'start' method found");
    }

    //              Running Methods

    /**
     * Find the method (local to this class, shared (like Java's system.out.print), or a method on another class)
     * Evaluate the parameters to have a list of values
     * Use interpretMethodCall() to actually run the method.
     *
     * Call GetParameters() to get the parameter value list
     * Find the method. This is tricky - there are several cases:
     * someLocalMethod() - has NO object name. Look in "object"
     * console.write() - the objectName is a CLASS and the method is shared
     * bestStudent.getGPA() - the objectName is a local or a member
     *
     * Once you find the method, call InterpretMethodCall() on it. Return the list that it returns.
     * Throw an exception if we can't find a match.
     * @param object - the object we are inside right now (might be empty)
     * @param locals - the current local variables
     * @param mc - the method call
     * @return - the return values
     */
    private List<InterpreterDataType> findMethodForMethodCallAndRunIt(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) {
        List<InterpreterDataType> parameters = getParameters(object, locals, mc);
        for (ClassNode curClass : parsedCode.Classes)
        {
            for (MethodDeclarationNode curMethod : curClass.methods)
            {
                if ((!curMethod.isPrivate || object.get().astNode.name.equals(curClass)) && curMethod.name.equals(mc.methodName))
                {
                    if (mc.objectName.isPresent())
                    {
                        if(locals.get(mc.objectName.get()).getClass().equals(ReferenceIDT.class))
                        {
                            ReferenceIDT reference = (ReferenceIDT) locals.get(mc.objectName.get());
                            return interpretMethodCall(reference.refersTo, curMethod, parameters);
                        }
                    }
                   return interpretMethodCall(object, curMethod, parameters);
                }
            }
        }
        if (mc.objectName.isPresent())
        {
            if (builtIns.containsKey(mc.objectName.get().concat("." + mc.methodName)))
            {
                isBuiltIn = true;
                return interpretMethodCall(object, builtIns.get(mc.objectName.get().concat("." + mc.methodName)), parameters);
            }
        }
        throw new RuntimeException("No method found");
    }

    private InterpreterDataType findMethodForMethodCallExpressionAndRunIt(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallExpressionNode me) {
        List<InterpreterDataType> parameters = getParametersExpression(object, locals, me);
        for (ClassNode curClass : parsedCode.Classes)
        {
            for (MethodDeclarationNode curMethod : curClass.methods)
            {
                if ((!curMethod.isPrivate || object.get().astNode.name.equals(curClass)) && curMethod.name.equals(me.methodName))
                {
                    if (me.objectName.isPresent())
                    {
                        if(locals.get(me.objectName.get()).getClass().equals(ReferenceIDT.class))
                        {
                            ReferenceIDT reference = (ReferenceIDT) locals.get(me.objectName.get());
                            return interpretMethodCall(reference.refersTo, curMethod, parameters).getFirst();
                        }
                    }
                    return interpretMethodCall(object, curMethod, parameters).getFirst();
                }
            }
        }
        if (me.objectName.isPresent())
        {
            if (builtIns.containsKey(me.objectName.get().concat("." + me.methodName)))
            {
                isBuiltIn = true;
                return interpretMethodCall(object, builtIns.get(me.objectName.get().concat("." + me.methodName)), parameters).getFirst();
            }
        }
        throw new RuntimeException("No method found");
    }

    /**
     * Run a "prepared" method (found, parameters evaluated)
     * This is split from findMethodForMethodCallAndRunIt() because there are a few cases where we don't need to do the finding:
     * in start() and dealing with loops with iterator objects, for example.
     *
     * Check to see if "m" is a built-in. If so, call Execute() on it and return it
     * Make local variables, per "m"
     * If the number of passed in values doesn't match m's "expectations", throw
     * Add the parameters by name to locals.
     * Call InterpretStatementBlock
     * Build the return list - find the names from "m", then get the values for those names and add them to the list.
     * @param object - The object this method is being called on (might be empty for shared)
     * @param m - Which method is being called
     * @param values - The values to be passed in
     * @return the returned values from the method
     */
    private List<InterpreterDataType> interpretMethodCall(Optional<ObjectIDT> object, MethodDeclarationNode m, List<InterpreterDataType> values) {
        var retVal = new LinkedList<InterpreterDataType>();
        if(isBuiltIn)
        {
            BuiltInMethodDeclarationNode BuiltInCalledMethod = (BuiltInMethodDeclarationNode) m;
            retVal.addAll((((BuiltInMethodDeclarationNode) m).Execute(values)));
            isBuiltIn = false;
            return retVal;
        }
        List<InterpreterDataType> parameters = new ArrayList<>();
        HashMap <String, InterpreterDataType> locals = new HashMap<>();
        if (object.isPresent()) {
            for (MemberNode curMember : object.get().astNode.members) {
                locals.put(curMember.declaration.name, instantiate(curMember.declaration.type));
            }
            locals.putAll(object.get().members);
        }
        if (values.size() != m.parameters.size()) throw new RuntimeException("Parameter mismatch");
        for (int i = 0; i < m.parameters.size(); i++)
        {
            if (!typeMatchToIDT(m.parameters.get(i).type, values.get(i))) throw new RuntimeException("Parameter mismatch");
        }
        for (VariableDeclarationNode curReturn : m.returns)
        {
            locals.put(curReturn.name, instantiate(curReturn.type));
            retVal.add(locals.get(curReturn.name));
        }
        for (VariableDeclarationNode curLocal : m.locals)
        {
            locals.put(curLocal.name, instantiate(curLocal.type));
        }
        interpretStatementBlock(object, m.statements, locals);
        return retVal;
    }

    //              Running Constructors

    /**
     * This is a special case of the code for methods. Just different enough to make it worthwhile to split it out.
     *
     * Call GetParameters() to populate a list of IDT's
     * Call GetClassByName() to find the class for the constructor
     * If we didn't find the class, throw an exception
     * Find a constructor that is a good match - use DoesConstructorMatch()
     * Call InterpretConstructorCall() on the good match
     * @param callerObj - the object that we are inside when we called the constructor
     * @param locals - the current local variables (used to fill parameters)
     * @param mc  - the method call for this construction
     * @param newOne - the object that we just created that we are calling the constructor for
     */
    private void findConstructorAndRunIt(Optional<ObjectIDT> callerObj, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc, ObjectIDT newOne) {
        List <InterpreterDataType> parameters = getParameters(callerObj, locals, mc);
        Optional<ClassNode> Class = getClassByName(mc.methodName);
        if (Class.isEmpty())
            {
                throw new RuntimeException("Class "+ mc.methodName + " not found");
            }
        for (ConstructorNode curConstructor : Class.get().constructors) {
            if (doesConstructorMatch(curConstructor, mc, parameters))
            {
                if (callerObj.isPresent()) {
                    for (MemberNode curMember : callerObj.get().astNode.members) {
                        newOne.members.put(curMember.declaration.name, instantiate(curMember.declaration.type));
                    }
                }
                interpretConstructorCall(newOne, curConstructor, parameters);
            }
        }
    }

    /**
     * Similar to interpretMethodCall, but "just different enough" - for example, constructors don't return anything.
     *
     * Creates local variables (as defined by the ConstructorNode), calls Instantiate() to do the creation
     * Checks to ensure that the right number of parameters were passed in, if not throw.
     * Adds the parameters (with the names from the ConstructorNode) to the locals.
     * Calls InterpretStatementBlock
     * @param object - the object that we allocated
     * @param c - which constructor is being called
     * @param values - the parameter values being passed to the constructor
     */
    private void interpretConstructorCall(ObjectIDT object, ConstructorNode c, List<InterpreterDataType> values) {
        HashMap<String, InterpreterDataType> locals = new HashMap<>();
        locals.putAll(object.members);
        int i = 0;
        for (VariableDeclarationNode curParameter : c.parameters) {
            locals.put(curParameter.name, values.get(i));
            i++;
        }
        if (values.size() != c.parameters.size())
        {
            throw new RuntimeException("parameter mismatch");
        }
        interpretStatementBlock(Optional.of(object), c.statements, locals);
    }

    //              Running Instructions

    /**
     * Given a block (which could be from a method or an "if" or "loop" block, run each statement.
     * Blocks, by definition, do ever statement, so iterating over the statements makes sense.
     *
     * For each statement in statements:
     * check the type:
     *      For AssignmentNode, FindVariable() to get the target. Evaluate() the expression. Call Assign() on the target with the result of Evaluate()
     *      For MethodCallStatementNode, call doMethodCall(). Loop over the returned values and copy the into our local variables
     *      For LoopNode - there are 2 kinds.
     *          Setup:
     *          If this is a Loop over an iterator (an Object node whose class has "iterator" as an interface) LIES
     *              Find the "getNext()" method; throw an exception if there isn't one
     *          Loop:
     *          While we are not done:
     *              if this is a boolean loop, Evaluate() to get true or false.
     *              if this is an iterator, call "getNext()" - it has 2 return values. The first is a boolean (was there another?), the second is a value
     *              If the loop has an assignment variable, populate it: for boolean loops, the true/false. For iterators, the "second value"
     *              If our answer from above is "true", InterpretStatementBlock() on the body of the loop.
     *       For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements. If not AND there is an else, InterpretStatementBlock on the else body.
     * @param object - the object that this statement block belongs to (used to get member variables and any members without an object)
     * @param statements - the statements to run
     * @param locals - the local variables
     */
    private void interpretStatementBlock(Optional<ObjectIDT> object, List<StatementNode> statements, HashMap<String, InterpreterDataType> locals) {
        for (StatementNode curStatement : statements)
        {
            if (curStatement.getClass().equals(AssignmentNode.class))
            {
                InterpreterDataType destination = findVariable(((AssignmentNode) curStatement).target.name, locals, object);
                InterpreterDataType evaluation = evaluate(locals, object, ((AssignmentNode) curStatement).expression);
                destination.Assign(evaluation);
            }
            if (curStatement.getClass().equals(MethodCallStatementNode.class)){
                List <InterpreterDataType> result = findMethodForMethodCallAndRunIt(object, locals, (MethodCallStatementNode) curStatement);
                int i = 0;
                for (InterpreterDataType curResult :result)
                {
                    InterpreterDataType destination = findVariable(((MethodCallStatementNode) curStatement).returnValues.get(i).name, locals, object);
                    destination.Assign(curResult);
                    i++;
                }
            }
            if (curStatement.getClass().equals(LoopNode.class))
            {
                do {
                    interpretStatementBlock(object, ((LoopNode) curStatement).statements, locals);
                }
                while (((BooleanIDT) evaluate(locals, object, ((LoopNode) curStatement).expression)).Value);
                //iterators are fake, they never existed. they were an illision like everything in tran
                //this was painful lets never do this again

            }
            if (curStatement.getClass().equals(IfNode.class))
            {
                InterpreterDataType outcome = evaluate(locals, object, ((IfNode) curStatement).condition);
                if (outcome.getClass().equals(BooleanIDT.class) && ((BooleanIDT) outcome).Value == true)
                {
                    interpretStatementBlock(object, ((IfNode) curStatement).statements, locals);
                }
                else if(((IfNode) curStatement).elseStatement.isPresent())
                {
                    interpretStatementBlock(object, ((IfNode) curStatement).elseStatement.get().statements, locals);
                }
            }
        }
    }

    /**
     *  evaluate() processes everything that is an expression - math, variables, boolean expressions.
     *  There is a good bit of recursion in here, since math and comparisons have left and right sides that need to be evaluated.
     *
     * See the How To Write an Interpreter document for examples
     * For each possible ExpressionNode, do the work to resolve it:
     * BooleanLiteralNode - create a new BooleanLiteralNode with the same value
     *      - Same for all of the basic data types
     * BooleanOpNode - Evaluate() left and right, then perform either and/or on the results.
     * CompareNode - Evaluate() both sides. Do good comparison for each data type
     * MathOpNode - Evaluate() both sides. If they are both numbers, do the math using the built-in operators. Also handle String + String as concatenation (like Java)
     * MethodCallExpression - call doMethodCall() and return the first value
     * VariableReferenceNode - call findVariable()
     * @param locals the local variables
     * @param object - the current object we are running
     * @param expression - some expression to evaluate
     * @return a value
     */
    private InterpreterDataType evaluate(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, ExpressionNode expression) {
        if (expression.getClass().equals(BooleanLiteralNode.class))
        {
            return new BooleanIDT(((BooleanLiteralNode) expression).value);
        }
        if (expression.getClass().equals(BooleanOpNode.class))
        {
            InterpreterDataType left = evaluate(locals, object, ((BooleanOpNode) expression).left);
            InterpreterDataType right = evaluate(locals, object, ((BooleanOpNode) expression).right);
            if (((BooleanOpNode) expression).op.equals(BooleanOpNode.BooleanOperations.and))
            {
                if (((BooleanLiteralNode) left).value && ((BooleanLiteralNode) right).value)
                {
                    return new BooleanIDT(true);
                }
                return new BooleanIDT(false);
            }
            else
            {
                if (((BooleanLiteralNode) left).value || ((BooleanLiteralNode) right).value)
                {
                    return new BooleanIDT(true);
                }
                return new BooleanIDT(false);
            }

        }
        if (expression.getClass().equals(CompareNode.class))
        {
            InterpreterDataType left = evaluate(locals, object, ((CompareNode) expression).left);
            InterpreterDataType right = evaluate(locals, object, ((CompareNode) expression).right);
            BooleanIDT result = new BooleanIDT(false);
            if (left.getClass().equals(StringIDT.class))
            {
                left = new NumberIDT(((StringIDT) left).Value.hashCode());
            }
            if (right.getClass().equals(StringIDT.class))
            {
                right = new NumberIDT(((StringIDT) right).Value.hashCode());
            }
            if (left.getClass().equals(CharIDT.class))
            {
                left = new NumberIDT((float) ((CharIDT) left).Value);
            }
            if (right.getClass().equals(CharIDT.class))
            {
                right = new NumberIDT((float) ( (CharIDT) right).Value);
            }
            if (left.getClass().equals(BooleanIDT.class))
            {
                float val = ((BooleanIDT) left).Value ? 1 : 0;
                left = new NumberIDT(val);
            }
            if (right.getClass().equals(BooleanIDT.class))
            {
                float val = ((BooleanIDT) right).Value ? 1 : 0;
                right = new NumberIDT(val);
            }
            if (right.getClass().equals(NumberIDT.class) && left.getClass().equals(NumberIDT.class)) {
                switch (((CompareNode) expression).op) {
                    case eq -> {result.Value = ((NumberIDT) left).Value == ((NumberIDT) right).Value ? true : false;}
                    case ge -> {result.Value = ((NumberIDT) left).Value >= ((NumberIDT) right).Value ? true : false;}
                    case gt -> {result.Value = ((NumberIDT) left).Value > ((NumberIDT) right).Value ? true : false;}
                    case le -> {result.Value = ((NumberIDT) left).Value <= ((NumberIDT) right).Value ? true : false;}
                    case lt -> {result.Value = ((NumberIDT) left).Value < ((NumberIDT) right).Value ? true : false;}
                    case ne -> {result.Value = ((NumberIDT) left).Value != ((NumberIDT) right).Value ? true : false;}
                }
            }
            return result;
        }
        if (expression.getClass().equals(MathOpNode.class))
        {
            InterpreterDataType left = evaluate(locals, object, ((MathOpNode) expression).left);
            InterpreterDataType right = evaluate(locals, object, ((MathOpNode) expression).right);
            float result = 0;
            boolean isChar = false;
            //concatenating string
            if(left.getClass().equals(StringIDT.class) && !right.getClass().equals(StringIDT.class))
            {
                right = new StringIDT(right.toString());
            }
            if(right.getClass().equals(StringIDT.class) && !left.getClass().equals(StringIDT.class))
            {
                left = new StringIDT(left.toString());
            }
            if (left.getClass().equals(StringIDT.class) && right.getClass().equals(StringIDT.class) && ((MathOpNode) expression).op.equals(MathOpNode.MathOperations.add))
            {
                return new StringIDT(((StringIDT) left).Value.concat(((StringIDT) right).Value));
            }
            //converting chars to numbers
            if (left.getClass().equals(CharIDT.class))
            {
                left = new NumberIDT((int) ((CharIDT) left).Value);
            }
            if (right.getClass().equals(CharIDT.class))
            {
                right = new NumberIDT((int) ((CharIDT) right).Value);
            }
            //preforming opperation
            if (left.getClass().equals(NumberIDT.class) && right.getClass().equals(NumberIDT.class))
            {
                switch (((MathOpNode) expression).op)
                {
                    case add -> {result = (((NumberIDT) left).Value) + (((NumberIDT) right).Value);}
                    case divide -> {result = (((NumberIDT) left).Value) / (((NumberIDT) right).Value);}
                    case modulo -> {result = (((NumberIDT) left).Value) % (((NumberIDT) right).Value);}
                    case multiply -> {result = (((NumberIDT) left).Value) * (((NumberIDT) right).Value);}
                    case subtract -> {result = (((NumberIDT) left).Value) - (((NumberIDT) right).Value);}
                }
                return new NumberIDT(result);
            }
            else if (left.getClass().equals(right.getClass()))
            {
                throw new RuntimeException("Data type " + left.getClass() + " is not compatable with opperation provided");
            }
            else
            {
                throw new RuntimeException("Type mismatch: {" + left.getClass() + ":" + right.getClass() + "}" );
            }

        }
        if (expression.getClass().equals(NewNode.class))
        {
            ObjectIDT NewObject = new ObjectIDT(getClassByName(((NewNode) expression).className).get());
            MethodCallStatementNode CallConstructor = new MethodCallStatementNode();
            CallConstructor.methodName = new String(((NewNode) expression).className);
            CallConstructor.objectName = Optional.of(((NewNode) expression).className);
            CallConstructor.parameters.addAll(((NewNode) expression).parameters);
            findConstructorAndRunIt(object, locals, CallConstructor, NewObject);
            return NewObject;
        }
        if (expression.getClass().equals(MethodCallExpressionNode.class))
        {
            return findMethodForMethodCallExpressionAndRunIt(object, locals, (MethodCallExpressionNode) expression);
        }
        if (expression.getClass().equals(VariableReferenceNode.class))
        {
            return findVariable(((VariableReferenceNode) expression).name, locals, object);
        }
        if (expression.getClass().equals(NumericLiteralNode.class))
        {
            return new NumberIDT(((NumericLiteralNode) expression).value);
        }
        if (expression.getClass().equals(StringLiteralNode.class))
        {
            return new StringIDT(((StringLiteralNode) expression).value);
        }
        if (expression.getClass().equals(CharLiteralNode.class))
        {
            return new CharIDT(((CharLiteralNode) expression).value);
        }


        throw new IllegalArgumentException();
    }

    //              Utility Methods

    /**
     * Used when trying to find a match to a method call. Given a method declaration, does it match this methoc call?
     * We double check with the parameters, too, although in theory JUST checking the declaration to the call should be enough.
     *
     * Match names, parameter counts (both declared count vs method call and declared count vs value list), return counts.
     * If all of those match, consider the types (use TypeMatchToIDT).
     * If everything is OK, return true, else return false.
     * Note - if m is a built-in and isVariadic is true, skip all of the parameter validation.
     * @param m - the method declaration we are considering
     * @param mc - the method call we are trying to match
     * @param parameters - the parameter values for this method call
     * @return does this method match the method call?
     */
    private boolean doesMatch(MethodDeclarationNode m, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        if (parameters.size() != m.parameters.size()) return false;
        for (int i = 0 ; i < parameters.size(); i++)
        {
            if (!typeMatchToIDT(m.parameters.get(i).type, parameters.get(i)))
            {
                return false;
            }
        }
        if (m.returns.size() != mc.returnValues.size()) return false;
        return true;
    }

    /**
     * Very similar to DoesMatch() except simpler - there are no return values, the name will always match.
     * @param c - a particular constructor
     * @param mc - the method call
     * @param parameters - the parameter values
     * @return does this constructor match the method call?
     */
    private boolean doesConstructorMatch(ConstructorNode c, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        if (c.parameters.size() != parameters.size()) return false;
        for (int i = 0; i < c.parameters.size(); i++)
        {
            if (typeMatchToIDT(c.parameters.get(i).type, parameters.get(i))) {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * Used when we call a method to get the list of values for the parameters.
     *
     * for each parameter in the method call, call Evaluate() on the parameter to get an IDT and add it to a list
     * @param object - the current object
     * @param locals - the local variables
     * @param mc - a method call
     * @return the list of method values
     */
    private List<InterpreterDataType> getParameters(Optional<ObjectIDT> object, HashMap<String,InterpreterDataType> locals, MethodCallStatementNode mc) {
        List<InterpreterDataType> returnList = new LinkedList<>();
        for (ExpressionNode curParameter : mc.parameters)
        {
            returnList.add(evaluate(locals, object, curParameter));
        }
        return returnList;
    }

    private List<InterpreterDataType> getParametersExpression(Optional<ObjectIDT> object, HashMap<String,InterpreterDataType> locals, MethodCallExpressionNode mc) {
        List<InterpreterDataType> returnList = new LinkedList<>();
        for (ExpressionNode curParameter : mc.parameters)
        {
            returnList.add(evaluate(locals, object, curParameter));
        }
        return returnList;
    }

    /**
     * Used when we have an IDT and we want to see if it matches a type definition
     * Commonly, when someone is making a function call - do the parameter values match the method declaration?
     *
     * If the IDT is a simple type (boolean, number, etc) - does the string type match the name of that IDT ("boolean", etc)
     * If the IDT is an object, check to see if the name matches OR the class has an interface that matches
     * If the IDT is a reference, check the inner (refered to) type
     * @param type the name of a data type (parameter to a method)
     * @param idt the IDT someone is trying to pass to this method
     * @return is this OK?
     */
    private boolean typeMatchToIDT(String type, InterpreterDataType idt) {
        switch (type)
        {
            case "boolean":
                if(idt.getClass().equals(BooleanIDT.class))
                {
                    return true;
                }
                return false;
            case "number":
                if (idt.getClass().equals(NumberIDT.class))
                {
                    return true;
                }
                return false;
            case "char":
                if (idt.getClass().equals(CharIDT.class))
                {
                    return true;
                }
                return false;
            case "string":
                if (idt.getClass().equals(StringIDT.class))
                {
                    return true;
                }
                return false;
        }
        throw new RuntimeException("Unable to resolve type " + type);
    }

    /**
     * Find a method in an object that is the right match for a method call (same name, parameters match, etc. Uses doesMatch() to do most of the work)
     *
     * Given a method call, we want to loop over the methods for that class, looking for a method that matches (use DoesMatch) or throw
     * @param object - an object that we want to find a method on
     * @param mc - the method call
     * @param parameters - the parameter value list
     * @return a method or throws an exception
     */
    private MethodDeclarationNode getMethodFromObject(ObjectIDT object, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
       for (MethodDeclarationNode curMethod : object.astNode.methods)
       {
           if (doesMatch(curMethod, mc, parameters))
           {
               return curMethod;
           }
       }
        throw new RuntimeException("Unable to resolve method call " + mc);
    }

    /**
     * Find a class, given the name. Just loops over the TranNode's classes member, matching by name.
     *
     * Loop over each class in the top node, comparing names to find a match.
     * @param name Name of the class to find
     * @return either a class node or empty if that class doesn't exist
     */
    private Optional<ClassNode> getClassByName(String name) {
        for (ClassNode curClass : parsedCode.Classes)
        {
            if (curClass.name.equals(name)) return Optional.of(curClass);
        }
        return Optional.empty();
    }

    /**
     * Given an execution environment (the current object, the current local variables), find a variable by name.
     *
     * @param name  - the variable that we are looking for
     * @param locals - the current method's local variables
     * @param object - the current object (so we can find members)
     * @return the IDT that we are looking for or throw an exception
     */
    private InterpreterDataType findVariable(String name, HashMap<String,InterpreterDataType> locals, Optional<ObjectIDT> object) {
        if (locals.containsKey(name))
        {
            return locals.get(name);
        }
        if (object.get().members.containsKey(name))
        {
            return object.get().members.get(name);
        }
        if(name.equals("true"))
        {
            return new BooleanIDT(true);
        }
        if(name.equals("false"))
        {
            return new BooleanIDT(false);
        }
        if(name.equals("boolean.true"))
        {
            return new BooleanIDT(true);
        }
        if(name.equals("boolean.false"))
        {
            return new BooleanIDT(false);
        }


        throw new RuntimeException("Unable to find variable " + name);
    }

    /**
     * Given a string (the type name), make an IDT for it.
     *
     * @param type The name of the type (string, number, boolean, character). Defaults to ReferenceIDT if not one of those.
     * @return an IDT with default values (0 for number, "" for string, false for boolean, ' ' for character)
     */
    private InterpreterDataType instantiate(String type) {
	    switch (type)
        {
            case "number" -> {return new NumberIDT(0);}
            case "string" -> {return new StringIDT("");}
            case "boolean" -> {return new BooleanIDT(false);}
            case "character" -> {return new CharIDT(' ');}
        }
        ReferenceIDT result = new ReferenceIDT();
        result.refersTo = Optional.empty();
        return result;
    }

}
