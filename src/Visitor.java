import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import symbol.base.Position;
import symbol.base.Symbol;

public class Visitor extends SysYParserBaseVisitor<Void>{

    private final String[] parserRuleNames;
    private final String[] lexerRuleNames;
    private final String[] highLight;
    private final Symbol symbol;
    private final String newName;

    public Visitor(String[] parserRuleNames, String[] lexerRuleNames, Symbol symbol, String newName) {
        this.parserRuleNames = parserRuleNames;
        this.lexerRuleNames = lexerRuleNames;
        this.highLight = new String[lexerRuleNames.length];
        for (int i = 0; i < highLight.length; i++) {
            if (i + 1 <= 9) {
                highLight[i] = "[orange]";
            } else if (i + 1 <= 24) {
                highLight[i] = "[blue]";
            } else if (i + 1 == 33) {
                highLight[i] = "[red]";
            } else if (i + 1 == 34) {
                highLight[i] = "[green]";
            }
        }
        this.symbol = symbol;
        this.newName = newName;
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
        int type = node.getSymbol().getType();
        if ((type >= 1 && type <= 24) || type == 33) {
            Position position = new Position(node.getSymbol().getLine(), node.getSymbol().getCharPositionInLine());
            if (symbol.checkPosition(position)) {
                String ruleName = lexerRuleNames[type - 1];
                System.err.println(getIndent(depth) + newName + " " + ruleName + highLight[type - 1]);
            } else {
                String ruleName = lexerRuleNames[type - 1];
                System.err.println(getIndent(depth) + node.getSymbol().getText() + " " + ruleName + highLight[type - 1]);
            }
        } else if (type == 34) {
            String ruleName = lexerRuleNames[type - 1];
            System.err.println(getIndent(depth) + parseInt(node.getSymbol().getText()) + " " + ruleName + highLight[type - 1]);
        }
        return super.visitTerminal(node);
    }

    public static String getIndent(int depth) {
        return "  ".repeat(Math.max(0, depth - 1));
    }

    public static String titleCase(String ruleName) {
        return ruleName.substring(0, 1).toUpperCase() + ruleName.substring(1);
    }

    public static Integer parseInt(String text) {
        if (text.startsWith("0x") || text.startsWith("0X")) {
            return Integer.parseInt(text.substring(2), 16);
        } else if (text.startsWith("0") && text.length() > 1) {
            return Integer.parseInt(text.substring(1), 8);
        }
        return Integer.parseInt(text);
    }
}
