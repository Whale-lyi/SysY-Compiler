import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.*;
import java.util.List;

public class Main
{    
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
//        String source = "hello.txt";
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
        sysYLexer.removeErrorListeners();
        sysYLexer.addErrorListener(new MyErrorListener());
        List<? extends Token> allTokens = sysYLexer.getAllTokens();
        try {
            String[] ruleNames = sysYLexer.getRuleNames();
            for (Token token : allTokens) {
                String ruleName = ruleNames[token.getType() - 1];
                if (ruleName.equals("INTEGR_CONST")) {
                    Integer number = parseInt(token.getText());
                    System.err.println(ruleName + " " + number + " at Line " + token.getLine() + ".");
                } else {
                    System.err.println(ruleName + " " + token.getText() + " at Line " + token.getLine() + ".");
                }
            }
        } catch (RuntimeException ignored) {}
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
