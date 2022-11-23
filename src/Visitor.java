import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class Visitor extends SysYParserBaseVisitor<Void>{

    private final String[] parserRuleNames;
    private final String[] lexerRuleNames;

    public Visitor(String[] parserRuleNames, String[] lexerRuleNames) {
        this.parserRuleNames = parserRuleNames;
        this.lexerRuleNames = lexerRuleNames;
    }

    @Override
    public Void visitChildren(RuleNode node) {
        int depth = node.getRuleContext().depth();
        String ruleName = parserRuleNames[node.getRuleContext().getRuleIndex()];
        System.err.println(getIndent(depth) + titleCase(ruleName));
        return super.visitChildren(node);
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        RuleContext ruleContext = (RuleContext) node.getParent();
        int depth = ruleContext.depth() + 1;
        String ruleName = lexerRuleNames[node.getSymbol().getType() + 1];
        System.err.println(getIndent(depth) + node.getSymbol().getText() + " " + ruleName);
        return super.visitTerminal(node);
    }

    public static String getIndent(int depth) {
        return "  ".repeat(Math.max(0, depth - 1));
    }

    public static String titleCase(String ruleName) {
        return ruleName.substring(0, 1).toUpperCase() + ruleName.substring(1);
    }
}
