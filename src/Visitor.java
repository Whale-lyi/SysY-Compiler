import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class Visitor extends SysYParserBaseVisitor<Void>{
    @Override
    public Void visitChildren(RuleNode node) {
        System.out.println("visitChildren: " + node.getRuleContext().toString());
        return super.visitChildren(node);
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
//        System.out.println("visitTerminal: " + node.getSymbol().getText());
        return super.visitTerminal(node);
    }
}
