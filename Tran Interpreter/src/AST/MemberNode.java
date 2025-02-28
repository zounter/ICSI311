package AST;

public class MemberNode implements Node {
    public VariableDeclarationNode declaration;

    @Override
    public String toString() {
        return  declaration +
                "\n" ;
    }
}
