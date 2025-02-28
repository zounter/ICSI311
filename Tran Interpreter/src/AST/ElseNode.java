package AST;

import java.util.List;

public class ElseNode {
    public List<StatementNode> statements;

    @Override
    public String toString() {
        return "else: " + statements;
    }
}
