import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class Visitor extends SysYParserBaseVisitor<Void>{

    private String[] ruleNames;

    public Visitor(String[] ruleNames) {
        this.ruleNames = ruleNames;
    }

    @Override
    public Void visitChildren(RuleNode node) {
        int depth = node.getRuleContext().depth();
        System.out.println(getIndent(depth) + ruleNames[node.getRuleContext().getRuleIndex()]);
        return super.visitChildren(node);
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
//        System.out.println("visitTerminal: " + node.getSymbol().getText());
        return super.visitTerminal(node);
    }

    public static String getIndent(int depth) {
        return "  ".repeat(Math.max(0, depth - 1));
    }
}
