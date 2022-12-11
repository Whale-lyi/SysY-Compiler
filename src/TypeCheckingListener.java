import org.antlr.v4.runtime.tree.ParseTreeProperty;
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
    private final ParseTreeProperty<Type> arrayTypeProperty = new ParseTreeProperty<>();
    private final ParseTreeProperty<Type> basicTypeProperty = new ParseTreeProperty<>();

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
        ArrayList<Type> paramsType = new ArrayList<>();
        if (ctx.funcFParams().funcFParam() != null) {
            for (SysYParser.FuncFParamContext paramContext : ctx.funcFParams().funcFParam()) {
                // TODO: 检查节点级别是否正确
                paramsType.add(arrayTypeProperty.get(paramContext.IDENT()));
            }
        }
        FunctionType functionType = new FunctionType(new BasicTypeSymbol(typeName), paramsType);
        FunctionSymbol fun = new FunctionSymbol(currentScope, funName, functionType);
        // 检查是否重复定义
        Symbol resolve = currentScope.resolve(funName);
        if (resolve != null) {
            hasError = true;
            System.err.println("Error type 4 at Line " + ctx.IDENT().getSymbol().getLine() + ": Redefined function: " + funName);
        }
        currentScope.define(fun);
        fun.addPosition(new Position(ctx.IDENT().getSymbol().getLine(), ctx.IDENT().getSymbol().getCharPositionInLine()));
        // 切换作用域
        currentScope = fun;
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
        currentScope = currentScope.getEnclosingScope();
    }

    @Override
    public void exitFuncDef(SysYParser.FuncDefContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    @Override
    public void exitBlock(SysYParser.BlockContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    /**
     * (3) When to define symbols?
     */
    @Override
    public void exitConstDecl(SysYParser.ConstDeclContext ctx) {
        List<SysYParser.ConstDefContext> constDefContexts = ctx.constDef();
        for (SysYParser.ConstDefContext constDefContext : constDefContexts) {
            Type type = arrayTypeProperty.get(constDefContext.IDENT());
            String varName = constDefContext.IDENT().getText();
            // 检查重复定义
            Symbol resolve = currentScope.resolve(varName);
            if (resolve != null) {
                hasError = true;
                System.err.println("Error type 3 at Line " + constDefContext.IDENT().getSymbol().getLine() + ": Redefined variable: " + varName);
            }
            VariableSymbol varSymbol = new VariableSymbol(varName, type, true);

            currentScope.define(varSymbol);
        }
    }


    /**
     * (4) When to resolve symbols?
     */

    /**
     * (5) Construct the array type from bottom to top
     */


}
