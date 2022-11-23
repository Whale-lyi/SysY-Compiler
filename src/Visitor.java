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
        String ruleName = ruleNames[node.getRuleContext().getRuleIndex()];
        System.out.println(getIndent(depth) + titleCase(ruleName));
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

    public static String titleCase(String ruleName) {
        return ruleName.substring(0, 1).toUpperCase() + ruleName.substring(1);
    }
}
