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

import java.util.ArrayList;
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
    private boolean valid = true;

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
        // 检查是否重复定义
        Symbol resolve = currentScope.resolve(funName);
        if (resolve != null) {
            reportError(4, ctx.IDENT().getSymbol().getLine(), ": Redefined function: " + funName);
            FunctionType functionType = new FunctionType(new BasicTypeSymbol(typeName), null);
            FunctionSymbol fun = new FunctionSymbol(currentScope, funName, functionType);
            currentScope = fun;
            valid = false;
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
//        if (!(currentScope instanceof FunctionSymbol)) {
//            LocalScope localScope = new LocalScope(currentScope);
//            currentScope = localScope;
//        }
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
        if (valid) {
            for (Symbol sym : currentScope.getSymbols().values()) {
                if (sym.checkPosition(position)) {
                    symbol = sym;
                    break;
                }
            }
        } else {
            valid = true;
        }
        currentScope = currentScope.getEnclosingScope();
    }

    @Override
    public void exitBlock(SysYParser.BlockContext ctx) {
//        if (!(currentScope instanceof FunctionSymbol)) {
//            for (Symbol sym : currentScope.getSymbols().values()) {
//                if (sym.checkPosition(position)) {
//                    symbol = sym;
//                    break;
//                }
//            }
//            currentScope = currentScope.getEnclosingScope();
//        }
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
    public void exitFuncFParams(SysYParser.FuncFParamsContext ctx) {
        ArrayList<Type> paramsType = new ArrayList<>();
        for (SysYParser.FuncFParamContext paramContext : ctx.funcFParam()) {
            String varName = paramContext.IDENT().getText();
            String typeName = paramContext.bType().getText();
            Symbol resolve = currentScope.resolveInCurScope(varName);
            if (resolve != null) {
                reportError(3, paramContext.IDENT().getSymbol().getLine(), ": Redefined variable: " + varName);
            } else {
                List<TerminalNode> lBrackt = paramContext.L_BRACKT();
                Type type;
                if (lBrackt == null || lBrackt.size() == 0) {
                    type = new BasicTypeSymbol(typeName);
                } else if (lBrackt.size() == 1){
                    type = new ArrayType(-1, new BasicTypeSymbol(typeName));
                } else {
                    type = new BasicTypeSymbol(typeName);
                    for (int i = lBrackt.size() - 1; i >= 1; i--) {
                        type = new ArrayType(expValueProperty.get(paramContext.exp().get(i)) + 1, type);
                    }
                    type = new ArrayType(-1, type);
                }
                paramsType.add(type);
                VariableSymbol varSymbol = new VariableSymbol(varName, type, false);
                currentScope.define(varSymbol);
                varSymbol.addPosition(new Position(paramContext.IDENT().getSymbol().getLine(), paramContext.IDENT().getSymbol().getCharPositionInLine()));
            }
        }
        ((FunctionType)((FunctionSymbol) currentScope).getType()).setParamsType(paramsType);
    }

    @Override
    public void exitConstDecl(SysYParser.ConstDeclContext ctx) {
        String typeName = ctx.bType().getText();
        List<SysYParser.ConstDefContext> constDefContexts = ctx.constDef();
        for (SysYParser.ConstDefContext constDefContext : constDefContexts) {
            String varName = constDefContext.IDENT().getText();
            // 检查重复定义
            Symbol resolve = currentScope.resolveInCurScope(varName);
            if (resolve != null) {
                reportError(3, constDefContext.IDENT().getSymbol().getLine(), ": Redefined variable: " + varName);
            } else {
                List<SysYParser.ConstExpContext> constExpContexts = constDefContext.constExp();
                Type type;
                if (constExpContexts == null || constExpContexts.size() == 0) {
                    // 变量
                    type = new BasicTypeSymbol(typeName);
                    if (constDefContext.constInitVal() instanceof SysYParser.ExpConstInitValContext) {
                        Type tmp = typeProperty.get(((SysYParser.ExpConstInitValContext) constDefContext.constInitVal()).constExp());
                        if (tmp != null) {
                            if (type.getLevel() != tmp.getLevel()) {
                                reportError(5, constDefContext.IDENT().getSymbol().getLine(), ": type.Type mismatched for assignment.");
                            }
                        }
                    } else {
                        reportError(5, constDefContext.IDENT().getSymbol().getLine(), ": type.Type mismatched for assignment.");
                    }
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
        if (currentScope.getEnclosingScope() instanceof FunctionSymbol) {
            resolve = currentScope.getEnclosingScope().resolveInCurScope(varName);
        }
        if (resolve != null) {
            reportError(3, ctx.IDENT().getSymbol().getLine(), ": Redefined variable: " + varName);
        } else {
            List<SysYParser.ConstExpContext> constExpContexts = ctx.constExp();
            Type type = new BasicTypeSymbol(typeName);
            if (constExpContexts == null || constExpContexts.size() == 0) {
                // 变量
                if (ctx.initVal() instanceof SysYParser.ExpInitValContext) {
                    Type tmp = typeProperty.get(((SysYParser.ExpInitValContext) ctx.initVal()).exp());
                    if (tmp != null) {
                        if (type.getLevel() != tmp.getLevel()) {
                            reportError(5, ctx.IDENT().getSymbol().getLine(), ": type.Type mismatched for assignment.");
                        }
                    }
                } else {
                    reportError(5, ctx.IDENT().getSymbol().getLine(), ": type.Type mismatched for assignment.");
                }
            } else {
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
            reportError(3, ctx.IDENT().getSymbol().getLine(), ": Redefined variable: " + varName);
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
        Scope fun = currentScope;
        while (!(fun instanceof FunctionSymbol)) {
            fun = fun.getEnclosingScope();
        }
        Type retType = ((FunctionType)((FunctionSymbol)fun).getType()).getRetTy();
        if (!retType.getIsFunction() && !retType.getIsArray()) {
            if ("void".equals(((BasicTypeSymbol) retType).getName())) {
                if (ctx.exp() != null) {
                    if (typeProperty.get(ctx.exp()) != null) {
                        // TODO: return void_f();
                        reportError(7, ctx.RETURN().getSymbol().getLine(), ": type.Type mismatched for return.");
                    }
                }
            } else if ("int".equals(((BasicTypeSymbol) retType).getName())) {
                if (ctx.exp() == null) {
                    reportError(7, ctx.RETURN().getSymbol().getLine(), ": type.Type mismatched for return.");
                } else {
                    Type type = typeProperty.get(ctx.exp());
                    if (type != null) {
                        if (type.getIsArray() || type.getIsFunction()) {
                            reportError(7, ctx.RETURN().getSymbol().getLine(), ": type.Type mismatched for return.");
                        } else {
                            if (!"int".equals(((BasicTypeSymbol) type).getName())) {
                                reportError(7, ctx.RETURN().getSymbol().getLine(), ": type.Type mismatched for return.");
                            }
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
                reportError(11, ctx.lVal().IDENT().getSymbol().getLine(), ": The left-hand side of an assignment must be a variable.");
            } else if (lValType.getIsArray()){
                // 数组
                if (expType.getIsArray()) {
                    if (lValType.getLevel() != expType.getLevel()) {
                        reportError(5, ctx.lVal().IDENT().getSymbol().getLine(), ": type.Type mismatched for assignment.");
                    }
                } else {
                    reportError(5, ctx.lVal().IDENT().getSymbol().getLine(), ": type.Type mismatched for assignment.");
                }
            } else {
                // 变量
                if (expType.getIsArray() || expType.getIsFunction()) {
                    reportError(5, ctx.lVal().IDENT().getSymbol().getLine(), ": type.Type mismatched for assignment.");
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
            reportError(1, ctx.IDENT().getSymbol().getLine(), ": Undefined variable: " + ctx.IDENT().getText());
        } else {
            if (!resolve.getType().getIsArray() && !resolve.getType().getIsFunction()) {
                // 变量
                List<TerminalNode> lBrackt = ctx.L_BRACKT();
                if(lBrackt != null && lBrackt.size() > 0) {
                    reportError(9, ctx.IDENT().getSymbol().getLine(), ": Not an array: " + ctx.IDENT().getText());
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
                            reportError(9, ctx.IDENT().getSymbol().getLine(), ": Not an array: " + ctx.IDENT().getText());
                            break;
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
        Symbol resolve = globalScope.resolve(ctx.IDENT().getText());
        if (resolve == null) {
            reportError(2, ctx.IDENT().getSymbol().getLine(), ": Undefined function: " + ctx.IDENT().getText());
        } else {
            if (!resolve.getType().getIsFunction()) {
                reportError(10, ctx.IDENT().getSymbol().getLine(), ": Not a function: " + ctx.IDENT().getText());
            } else {
                resolve.addPosition(new Position(ctx.IDENT().getSymbol().getLine(), ctx.IDENT().getSymbol().getCharPositionInLine()));
                typeProperty.put(ctx, ((FunctionType) resolve.getType()).getRetTy());
                lineProperty.put(ctx, ctx.IDENT().getSymbol().getLine());
                // 参数匹配
                ArrayList<Type> paramsType = ((FunctionType) resolve.getType()).getParamsType();
                if (paramsType == null) {
                    paramsType = new ArrayList<>();
                }
                ArrayList<Type> realParams = new ArrayList<>();
                if (ctx.funcRParams() != null) {
                    for (SysYParser.ParamContext paramCtx : ctx.funcRParams().param()) {
                        realParams.add(typeProperty.get(paramCtx.exp()));
                    }
                }
                if (realParams.size() != paramsType.size()) {
                    reportError(8, ctx.IDENT().getSymbol().getLine(), ": Function is not applicable for arguments.");
                } else {
                    int length = realParams.size();
                    for (int i = 0; i < length; i++) {
                        Type type1 = realParams.get(i); // 调用
                        Type type2 = paramsType.get(i); // 定义
                        if (type1 != null) {
                            if (!type1.equals(type2)) {
                                reportError(8, ctx.IDENT().getSymbol().getLine(), ": Function is not applicable for arguments.");
                                break;
                            }
                        }
                    }
                }
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
                reportError(6, lineProperty.get(ctx.lhs), ": type.Type mismatched for operands.");
            } else if (rvalue.getIsArray() || rvalue.getIsFunction()) {
                reportError(6, lineProperty.get(ctx.rhs), ": type.Type mismatched for operands.");
            } else {
                typeProperty.put(ctx, lvalue);
                lineProperty.put(ctx, lineProperty.get(ctx.rhs));
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
                reportError(6, lineProperty.get(ctx.exp()), ": type.Type mismatched for operands.");
            } else {
                typeProperty.put(ctx, type);
                lineProperty.put(ctx, lineProperty.get(ctx.exp()));
            }
        }
    }

    @Override
    public void exitAddSubExp(SysYParser.AddSubExpContext ctx) {
        Type lvalue = typeProperty.get(ctx.lhs);
        Type rvalue = typeProperty.get(ctx.rhs);
        if (lvalue != null && rvalue != null) {
            if (lvalue.getIsArray() || lvalue.getIsFunction()) {
                reportError(6, lineProperty.get(ctx.lhs), ": type.Type mismatched for operands.");
            } else if (rvalue.getIsArray() || rvalue.getIsFunction()) {
                reportError(6, lineProperty.get(ctx.rhs), ": type.Type mismatched for operands.");
            } else {
                typeProperty.put(ctx, lvalue);
                lineProperty.put(ctx, lineProperty.get(ctx.rhs));
            }
        }
    }

    @Override
    public void exitConstExp(SysYParser.ConstExpContext ctx) {
        typeProperty.put(ctx, typeProperty.get(ctx.exp()));
    }

    /**
     * cond
     */

    @Override
    public void exitOrCond(SysYParser.OrCondContext ctx) {
        Type lvalue = typeProperty.get(ctx.lhs);
        Type rvalue = typeProperty.get(ctx.rhs);
        if (lvalue != null && rvalue != null) {
            if (lvalue.getIsArray() || lvalue.getIsFunction()) {
                reportError(6, lineProperty.get(ctx.lhs), ": type.Type mismatched for operands.");
            } else if (rvalue.getIsArray() || rvalue.getIsFunction()) {
                reportError(6, lineProperty.get(ctx.rhs), ": type.Type mismatched for operands.");
            } else {
                typeProperty.put(ctx, lvalue);
            }
        }
    }

    @Override
    public void exitExpCond(SysYParser.ExpCondContext ctx) {
        typeProperty.put(ctx, typeProperty.get(ctx.exp()));
        lineProperty.put(ctx, lineProperty.get(ctx.exp()));
    }

    @Override
    public void exitAndCond(SysYParser.AndCondContext ctx) {
        Type lvalue = typeProperty.get(ctx.lhs);
        Type rvalue = typeProperty.get(ctx.rhs);
        if (lvalue != null && rvalue != null) {
            if (lvalue.getIsArray() || lvalue.getIsFunction()) {
                reportError(6, lineProperty.get(ctx.lhs), ": type.Type mismatched for operands.");
            } else if (rvalue.getIsArray() || rvalue.getIsFunction()) {
                reportError(6, lineProperty.get(ctx.rhs), ": type.Type mismatched for operands.");
            } else {
                typeProperty.put(ctx, lvalue);
            }
        }
    }

    @Override
    public void exitLTGTLEGECond(SysYParser.LTGTLEGECondContext ctx) {
        Type lvalue = typeProperty.get(ctx.lhs);
        Type rvalue = typeProperty.get(ctx.rhs);
        if (lvalue != null && rvalue != null) {
            if (lvalue.getIsArray() || lvalue.getIsFunction()) {
                reportError(6, lineProperty.get(ctx.lhs), ": type.Type mismatched for operands.");
            } else if (rvalue.getIsArray() || rvalue.getIsFunction()) {
                reportError(6, lineProperty.get(ctx.rhs), ": type.Type mismatched for operands.");
            } else {
                typeProperty.put(ctx, lvalue);
            }
        }
    }

    @Override
    public void exitEQNEQCond(SysYParser.EQNEQCondContext ctx) {
        Type lvalue = typeProperty.get(ctx.lhs);
        Type rvalue = typeProperty.get(ctx.rhs);
        if (lvalue != null && rvalue != null) {
            if (lvalue.getIsArray() || lvalue.getIsFunction()) {
                reportError(6, lineProperty.get(ctx.lhs), ": type.Type mismatched for operands.");
            } else if (rvalue.getIsArray() || rvalue.getIsFunction()) {
                reportError(6, lineProperty.get(ctx.rhs), ": type.Type mismatched for operands.");
            } else {
                typeProperty.put(ctx, lvalue);
            }
        }
    }

    public Symbol getSymbol() {
        return symbol;
    }

    private void reportError(int type, int line, String msg) {
        if (valid) {
            hasError = true;
            System.err.println("Error type " + type + " at Line " + line + msg);
        }
    }
}
