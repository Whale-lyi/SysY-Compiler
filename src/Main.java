import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public class Main
{    
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);
        ParseTree tree = sysYParser.program();

        Visitor visitor = new Visitor(sysYParser.getRuleNames());
        visitor.visit(tree);
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
