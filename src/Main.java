import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("There should be 4 arguments");
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);
        ParseTree tree = sysYParser.program();

        ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
        TypeCheckingListener typeCheckingListener = new TypeCheckingListener();
        parseTreeWalker.walk(typeCheckingListener, tree);

        if (!typeCheckingListener.hasError) {
            Visitor visitor = new Visitor(sysYParser.getRuleNames(), sysYLexer.getRuleNames());
            visitor.visit(tree);
        }
    }

}
