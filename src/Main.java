import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import symbol.base.Position;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("There should be 4 arguments");
        }
        String source = args[0];
//        String source = "hello.txt";
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);
        ParseTree tree = sysYParser.program();

        ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
        TypeCheckingListener typeCheckingListener = new TypeCheckingListener(new Position(Integer.parseInt(args[1]), Integer.parseInt(args[2])));
//        TypeCheckingListener typeCheckingListener = new TypeCheckingListener(new Position(8, 4));
        parseTreeWalker.walk(typeCheckingListener, tree);


        if (!typeCheckingListener.hasError) {
            Visitor visitor = new Visitor(sysYParser.getRuleNames(), sysYLexer.getRuleNames(), typeCheckingListener.getSymbol(), args[3]);
//            Visitor visitor = new Visitor(sysYParser.getRuleNames(), sysYLexer.getRuleNames(), typeCheckingListener.getSymbol(), "d");
            visitor.visit(tree);
        }
    }

}
