package AST;

import java.util.ArrayList;
import java.util.List;

public class MethodHeaderNode {
    public String name;
    public List<VariableDeclarationNode> parameters = new ArrayList<>();
    public List<VariableDeclarationNode> returns = new ArrayList<>();

    @Override
    public String toString() {
        return
                        name + " (" + Node.variableDeclarationListToString(parameters) + ")" +
                        (returns.isEmpty() ? "" : " : " + Node.variableDeclarationListToString(returns)) +
                         "\n";
    }
}
