import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("There should be 2 arguments");
        }
//        String source = args[0];
//        String destination = args[1];
        String source = "tests/test1.sysy";
        String destination = "tests/test2.ll";
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);
        ParseTree tree = sysYParser.program();

        MyIRVisitor irVisitor = new MyIRVisitor(destination);
        irVisitor.visit(tree);
    }

}
