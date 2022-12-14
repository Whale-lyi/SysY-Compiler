import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import scope.*;
import scope.base.*;
import symbol.*;
import symbol.base.*;
import type.*;
import type.base.Type;

import java.util.ArrayList;
import java.util.List;

public class TypeCheckingListener extends SysYParserBaseListener {
    public boolean hasError = false;
    private GlobalScope globalScope = null;
    private Scope currentScope = null;
    private Position position;
    private final ParseTreeProperty<Type> arrayTypeProperty = new ParseTreeProperty<>();
    private final ParseTreeProperty<Integer> expValueProperty = new ParseTreeProperty<>();
    private Symbol symbol;

    public TypeCheckingListener(Position position) {
        this.position = position;
    }

    /**
     * (1) When/How to start/enter a new scope?
     */
    @Override
    public void enterProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope();
        currentScope = globalScope;
    }

    @Override
    public void enterFuncDef(SysYParser.FuncDefContext ctx) {
        String typeName = ctx.funcType().getText();
        globalScope.resolve(typeName);
        // 声明函数符号
        String funName = ctx.IDENT().getText();
        // TODO：写到参数声明
//        ArrayList<Type> paramsType = new ArrayList<>();
//        if (ctx.funcFParams() != null) {
//            // 函数符号添加参数类型
//            for (SysYParser.FuncFParamContext paramContext : ctx.funcFParams().funcFParam()) {
//                List<TerminalNode> lBrackt = paramContext.L_BRACKT();
//                if (lBrackt == null || lBrackt.size() == 0) {
//                    paramsType.add(new BasicTypeSymbol(paramContext.bType().getText()));
//                } else if (lBrackt.size() == 1){
//                    paramsType.add(new ArrayType(-1, new BasicTypeSymbol(paramContext.bType().getText())));
//                }
//            }
//        }
        FunctionType functionType = new FunctionType(new BasicTypeSymbol(typeName), null);
        FunctionSymbol fun = new FunctionSymbol(currentScope, funName, functionType);
        // 检查是否重复定义
        Symbol resolve = currentScope.resolve(funName);
        if (resolve != null) {
            hasError = true;
            System.err.println("Error type 4 at Line " + ctx.IDENT().getSymbol().getLine() + ": Redefined function: " + funName);
        } else {
            currentScope.define(fun);
            fun.addPosition(new Position(ctx.IDENT().getSymbol().getLine(), ctx.IDENT().getSymbol().getCharPositionInLine()));
            // 切换作用域
            currentScope = fun;
        }
    }

    @Override
    public void enterBlock(SysYParser.BlockContext ctx) {
        LocalScope localScope = new LocalScope(currentScope);
        currentScope = localScope;
    }

    /**
     * (2) When/How to exit the current scope?
     */
    @Override
    public void exitProgram(SysYParser.ProgramContext ctx) {
        for (Symbol sym : currentScope.getSymbols().values()) {
            if (sym.checkPosition(position)) {
                symbol = sym;
                break;
            }
        }
        currentScope = currentScope.getEnclosingScope();
    }

    @Override
    public void exitFuncDef(SysYParser.FuncDefContext ctx) {
        for (Symbol sym : currentScope.getSymbols().values()) {
            if (sym.checkPosition(position)) {
                symbol = sym;
                break;
            }
        }
        currentScope = currentScope.getEnclosingScope();
    }

    @Override
    public void exitBlock(SysYParser.BlockContext ctx) {
        for (Symbol sym : currentScope.getSymbols().values()) {
            if (sym.checkPosition(position)) {
                symbol = sym;
                break;
            }
        }
        currentScope = currentScope.getEnclosingScope();
    }

    /**
     * (3) When to define symbols?
     */
    @Override
    public void exitConstDecl(SysYParser.ConstDeclContext ctx) {
        String typeName = ctx.bType().getText();
        List<SysYParser.ConstDefContext> constDefContexts = ctx.constDef();
        for (SysYParser.ConstDefContext constDefContext : constDefContexts) {
            String varName = constDefContext.IDENT().getText();
            // 检查重复定义
            Symbol resolve = currentScope.resolveInCurScope(varName);
            if (resolve != null) {
                hasError = true;
                System.err.println("Error type 3 at Line " + constDefContext.IDENT().getSymbol().getLine() + ": Redefined variable: " + varName);
            } else {
                List<SysYParser.ConstExpContext> constExpContexts = constDefContext.constExp();
                Type type;
                if (constExpContexts == null || constExpContexts.size() == 0) {
                    // 变量
                    type = new BasicTypeSymbol(typeName);
                } else {
                    // 数组
                    type = new BasicTypeSymbol(typeName);
                    for (int i = constExpContexts.size() - 1; i >= 0; i--) {
                        type = new ArrayType(expValueProperty.get(constExpContexts.get(i).exp()), type);
                    }
                }
                VariableSymbol varSymbol = new VariableSymbol(varName, type, true);
                currentScope.define(varSymbol);
                varSymbol.addPosition(new Position(constDefContext.IDENT().getSymbol().getLine(), constDefContext.IDENT().getSymbol().getCharPositionInLine()));
            }
        }
    }


    /**
     * (4) When to resolve symbols?
     */

    /**
     * (5) Construct the array type from bottom to top
     */


    /**
     * exp 求值
     * @return
     */
    @Override
    public void exitMulDivModExp(SysYParser.MulDivModExpContext ctx) {
        super.exitMulDivModExp(ctx);
    }

    @Override
    public void exitLeftValExp(SysYParser.LeftValExpContext ctx) {
        super.exitLeftValExp(ctx);
    }

    @Override
    public void exitIntegerExp(SysYParser.IntegerExpContext ctx) {
        super.exitIntegerExp(ctx);
    }

    @Override
    public void exitParenExp(SysYParser.ParenExpContext ctx) {
        super.exitParenExp(ctx);
    }

    @Override
    public void exitUnaryOpExp(SysYParser.UnaryOpExpContext ctx) {
        super.exitUnaryOpExp(ctx);
    }

    @Override
    public void exitFuncCallExp(SysYParser.FuncCallExpContext ctx) {
        super.exitFuncCallExp(ctx);
    }

    @Override
    public void exitAddSubExp(SysYParser.AddSubExpContext ctx) {
        super.exitAddSubExp(ctx);
    }


    public Symbol getSymbol() {
        return symbol;
    }


}
