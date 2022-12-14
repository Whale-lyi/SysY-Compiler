import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import scope.GlobalScope;
import scope.LocalScope;
import scope.base.Scope;
import symbol.BasicTypeSymbol;
import symbol.FunctionSymbol;
import symbol.VariableSymbol;
import symbol.base.Position;
import symbol.base.Symbol;
import type.ArrayType;
import type.FunctionType;
import type.base.Type;

import java.util.List;

public class TypeCheckingListener extends SysYParserBaseListener {
    public boolean hasError = false;
    private GlobalScope globalScope = null;
    private Scope currentScope = null;
    private final Position position;
    private final ParseTreeProperty<Type> typeProperty = new ParseTreeProperty<>();
    private final ParseTreeProperty<Integer> expValueProperty = new ParseTreeProperty<>();
    private final ParseTreeProperty<Integer> lineProperty = new ParseTreeProperty<>();
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
        // 检查是否重复定义
        Symbol resolve = currentScope.resolve(funName);
        if (resolve != null) {
            hasError = true;
            System.err.println("Error type 4 at Line " + ctx.IDENT().getSymbol().getLine() + ": Redefined function: " + funName);
        } else {
            FunctionType functionType = new FunctionType(new BasicTypeSymbol(typeName), null);
            FunctionSymbol fun = new FunctionSymbol(currentScope, funName, functionType);
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
        if (currentScope != globalScope) {
            currentScope = currentScope.getEnclosingScope();
        }
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
                        type = new ArrayType(expValueProperty.get(constExpContexts.get(i).exp()) + 1, type);
                    }
                }
                VariableSymbol varSymbol = new VariableSymbol(varName, type, true);
                currentScope.define(varSymbol);
                varSymbol.addPosition(new Position(constDefContext.IDENT().getSymbol().getLine(), constDefContext.IDENT().getSymbol().getCharPositionInLine()));
            }
        }
    }

    @Override
    public void exitVarDefWithAssign(SysYParser.VarDefWithAssignContext ctx) {
        String typeName = ((SysYParser.VarDeclContext) ctx.parent).bType().getText();
        String varName = ctx.IDENT().getText();
        // 检查重复定义
        Symbol resolve = currentScope.resolveInCurScope(varName);
        if (resolve != null) {
            hasError = true;
            System.err.println("Error type 3 at Line " + ctx.IDENT().getSymbol().getLine() + ": Redefined variable: " + varName);
        } else {
            List<SysYParser.ConstExpContext> constExpContexts = ctx.constExp();
            // 变量
            Type type = new BasicTypeSymbol(typeName);
            if (constExpContexts != null && constExpContexts.size() > 0) {
                // 数组
                for (int i = constExpContexts.size() - 1; i >= 0; i--) {
                    type = new ArrayType(expValueProperty.get(constExpContexts.get(i).exp()) + 1, type);
                }
            }
            VariableSymbol varSymbol = new VariableSymbol(varName, type, false);
            currentScope.define(varSymbol);
            varSymbol.addPosition(new Position(ctx.IDENT().getSymbol().getLine(), ctx.IDENT().getSymbol().getCharPositionInLine()));
        }
    }

    @Override
    public void exitVarDefWithoutAssign(SysYParser.VarDefWithoutAssignContext ctx) {
        String typeName = ((SysYParser.VarDeclContext) ctx.parent).bType().getText();
        String varName = ctx.IDENT().getText();
        // 检查重复定义
        Symbol resolve = currentScope.resolveInCurScope(varName);
        if (resolve != null) {
            hasError = true;
            System.err.println("Error type 3 at Line " + ctx.IDENT().getSymbol().getLine() + ": Redefined variable: " + varName);
        } else {
            List<SysYParser.ConstExpContext> constExpContexts = ctx.constExp();
            // 变量
            Type type = new BasicTypeSymbol(typeName);
            if (constExpContexts != null && constExpContexts.size() > 0) {
                // 数组
                for (int i = constExpContexts.size() - 1; i >= 0; i--) {
                    type = new ArrayType(expValueProperty.get(constExpContexts.get(i).exp()) + 1, type);
                }
            }
            VariableSymbol varSymbol = new VariableSymbol(varName, type, false);
            currentScope.define(varSymbol);
            varSymbol.addPosition(new Position(ctx.IDENT().getSymbol().getLine(), ctx.IDENT().getSymbol().getCharPositionInLine()));
        }
    }

    /**
     * (4) When to resolve symbols?
     */

    @Override
    public void exitReturnStat(SysYParser.ReturnStatContext ctx) {
        FunctionSymbol fun = (FunctionSymbol) currentScope;
        Type retType = ((FunctionType)fun.getType()).getRetTy();
        if (!retType.getIsFunction() && !retType.getIsArray()) {
            if ("void".equals(((BasicTypeSymbol) retType).getName())) {
                if (ctx.exp() != null) {
                    hasError = true;
                    System.err.println("Error type 7 at Line " + ctx.RETURN().getSymbol().getLine() + ": type.Type mismatched for return.");
                }
            } else if ("int".equals(((BasicTypeSymbol) retType).getName())) {
                if (ctx.exp() == null) {
                    hasError = true;
                    System.err.println("Error type 7 at Line " + ctx.RETURN().getSymbol().getLine() + ": type.Type mismatched for return.");
                } else {
                    Type type = typeProperty.get(ctx.exp());
                    if (type != null) {
                        if (type.getIsArray() || type.getIsFunction()) {
                            hasError = true;
                            System.err.println("Error type 7 at Line " + ctx.RETURN().getSymbol().getLine() + ": type.Type mismatched for return.");
                        }
                    }
                }
            }
        }
    }

    @Override
    public void exitAssignStat(SysYParser.AssignStatContext ctx) {
        Type lValType = typeProperty.get(ctx.lVal());
        Type expType = typeProperty.get(ctx.exp());
        if (lValType != null && expType != null) {
            if (lValType.getIsFunction()) {
                // 函数
                hasError = true;
                System.err.println("Error type 11 at Line " + ctx.lVal().IDENT().getSymbol().getLine() + ": The left-hand side of an assignment must be a variable.");
            } else if (lValType.getIsArray()){
                // 数组
                if (expType.getIsArray()) {
                    if (lValType.getLevel() != expType.getLevel()) {
                        hasError = true;
                        System.err.println("Error type 5 at Line " + ctx.lVal().IDENT().getSymbol().getLine() + ": type.Type mismatched for assignment.");
                    }
                } else {
                    hasError = true;
                    System.err.println("Error type 5 at Line " + ctx.lVal().IDENT().getSymbol().getLine() + ": type.Type mismatched for assignment.");
                }
            } else {
                // 变量
                if (expType.getIsArray() || expType.getIsFunction()) {
                    hasError = true;
                    System.err.println("Error type 5 at Line " + ctx.lVal().IDENT().getSymbol().getLine() + ": type.Type mismatched for assignment.");
                }
            }
        }
    }

    /**
     * lVal
     */
    @Override
    public void exitLVal(SysYParser.LValContext ctx) {
        Symbol resolve = currentScope.resolve(ctx.IDENT().getText());
        if (resolve == null) {
            hasError = true;
            System.err.println("Error type 1 at Line " + ctx.IDENT().getSymbol().getLine() + ": Undefined variable: " + ctx.IDENT().getText());
        } else {
            if (!resolve.getType().getIsArray() && !resolve.getType().getIsFunction()) {
                // 变量
                List<TerminalNode> lBrackt = ctx.L_BRACKT();
                if(lBrackt != null && lBrackt.size() > 0) {
                    hasError = true;
                    System.err.println("Error type 9 at Line " + ctx.IDENT().getSymbol().getLine() + ": Not an array: " + ctx.IDENT().getText());
                } else {
                    resolve.addPosition(new Position(ctx.IDENT().getSymbol().getLine(), ctx.IDENT().getSymbol().getCharPositionInLine()));
                    typeProperty.put(ctx, resolve.getType());
                    lineProperty.put(ctx, ctx.IDENT().getSymbol().getLine());
                }
            } else if (resolve.getType().getIsArray()){
                // 数组
                boolean temp = true;
                resolve.addPosition(new Position(ctx.IDENT().getSymbol().getLine(), ctx.IDENT().getSymbol().getCharPositionInLine()));
                Type type = resolve.getType();
                if (ctx.exp() != null && ctx.exp().size() > 0) {
                    for (SysYParser.ExpContext exp : ctx.exp()) {
                        //TODO: 比较是否越界
                        if (type.getIsArray()) {
                            type = ((ArrayType) type).getSubType();
                        } else {
                            temp = false;
                            hasError = true;
                            System.err.println("Error type 9 at Line " + ctx.IDENT().getSymbol().getLine() + ": Not an array: " + ctx.IDENT().getText());
                        }
                    }
                }
                if (temp) {
                    typeProperty.put(ctx, type);
                    lineProperty.put(ctx, ctx.IDENT().getSymbol().getLine());
                }
            } else if (resolve.getType().getIsFunction()) {
                // 函数
                resolve.addPosition(new Position(ctx.IDENT().getSymbol().getLine(), ctx.IDENT().getSymbol().getCharPositionInLine()));
                typeProperty.put(ctx, resolve.getType());
                lineProperty.put(ctx, ctx.IDENT().getSymbol().getLine());
            }
        }
    }

    /**
     * exp 求值
     * @return
     */
    @Override
    public void exitFuncCallExp(SysYParser.FuncCallExpContext ctx) {
        Symbol resolve = currentScope.resolve(ctx.IDENT().getText());
        if (resolve == null) {
            hasError = true;
            System.err.println("Error type 2 at Line " + ctx.IDENT().getSymbol().getLine() + ": Undefined function: " + ctx.IDENT().getText());
        } else {
            if (!resolve.getType().getIsFunction()) {
                hasError = true;
                System.err.println("Error type 10 at Line " + ctx.IDENT().getSymbol().getLine() + ": Not a function: " + ctx.IDENT().getText());
            } else {
                resolve.addPosition(new Position(ctx.IDENT().getSymbol().getLine(), ctx.IDENT().getSymbol().getCharPositionInLine()));
                typeProperty.put(ctx, ((FunctionType) resolve.getType()).getRetTy());
                //TODO: 参数匹配
            }
        }
    }

    @Override
    public void exitMulDivModExp(SysYParser.MulDivModExpContext ctx) {
        Type lvalue = typeProperty.get(ctx.lhs);
        Type rvalue = typeProperty.get(ctx.rhs);
        if (lvalue != null && rvalue != null) {
            // TODO: 报几次错
            if (lvalue.getIsArray() || lvalue.getIsFunction()) {
                hasError = true;
                System.err.println("Error type 6 at Line " + lineProperty.get(ctx.lhs) + ": type.Type mismatched for operands.");
            } else if (rvalue.getIsArray() || rvalue.getIsFunction()) {
                hasError = true;
                System.err.println("Error type 6 at Line " + lineProperty.get(ctx.rhs) + ": type.Type mismatched for operands.");
            } else {
                typeProperty.put(ctx, lvalue);
            }
        }
    }

    @Override
    public void exitLeftValExp(SysYParser.LeftValExpContext ctx) {
        typeProperty.put(ctx, typeProperty.get(ctx.lVal()));
        lineProperty.put(ctx, lineProperty.get(ctx.lVal()));
    }

    @Override
    public void exitIntegerExp(SysYParser.IntegerExpContext ctx) {
        expValueProperty.put(ctx, Visitor.parseInt(ctx.number().INTEGR_CONST().getText()));
        typeProperty.put(ctx, new BasicTypeSymbol("int"));
        lineProperty.put(ctx, ctx.number().INTEGR_CONST().getSymbol().getLine());
    }

    @Override
    public void exitParenExp(SysYParser.ParenExpContext ctx) {
        expValueProperty.put(ctx, expValueProperty.get(ctx.exp()));
        typeProperty.put(ctx, typeProperty.get(ctx.exp()));
        lineProperty.put(ctx, lineProperty.get(ctx.exp()));
    }

    @Override
    public void exitUnaryOpExp(SysYParser.UnaryOpExpContext ctx) {
        Type type = typeProperty.get(ctx.exp());
        if (type != null) {
            if (type.getIsArray() || type.getIsFunction()) {
                hasError = true;
                System.err.println("Error type 6 at Line " + lineProperty.get(ctx.exp()) + ": type.Type mismatched for operands.");
            } else {
                typeProperty.put(ctx, type);
            }
        }
    }

    @Override
    public void exitAddSubExp(SysYParser.AddSubExpContext ctx) {
        Type lvalue = typeProperty.get(ctx.lhs);
        Type rvalue = typeProperty.get(ctx.rhs);
        if (lvalue != null && rvalue != null) {
            if (lvalue.getIsArray() || lvalue.getIsFunction()) {
                hasError = true;
                System.err.println("Error type 6 at Line " + lineProperty.get(ctx.lhs) + ": type.Type mismatched for operands.");
            } else if (rvalue.getIsArray() || rvalue.getIsFunction()) {
                hasError = true;
                System.err.println("Error type 6 at Line " + lineProperty.get(ctx.rhs) + ": type.Type mismatched for operands.");
            } else {
                typeProperty.put(ctx, lvalue);
            }
        }
    }

    public Symbol getSymbol() {
        return symbol;
    }

}
