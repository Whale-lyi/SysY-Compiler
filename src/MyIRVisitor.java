import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import scope.*;
import scope.base.*;

import java.util.Stack;

import static org.bytedeco.llvm.global.LLVM.*;

public class MyIRVisitor extends SysYParserBaseVisitor<LLVMValueRef> {
    private final LLVMModuleRef module = LLVMModuleCreateWithName("module");
    private final LLVMBuilderRef builder = LLVMCreateBuilder();
    private final LLVMTypeRef i32Type = LLVMInt32Type();
    private final LLVMTypeRef voidType = LLVMVoidType();
    private GlobalScope globalScope = null;
    private Scope currentScope = null;
    private LLVMValueRef currentFunc = null;
    private final LLVMValueRef zero = LLVMConstInt(i32Type, 0, 0);
    public static final BytePointer error = new BytePointer();
    private String destFile;
    private boolean hasReturn = false;
    private Stack<LLVMBasicBlockRef> whileStack = new Stack<>();
    public MyIRVisitor(String destFile) {
        //初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();
        this.destFile = destFile;
    }

    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope();
        currentScope = globalScope;

        LLVMValueRef result = super.visitProgram(ctx);

        currentScope = currentScope.getEnclosingScope();

        if (LLVMPrintModuleToFile(module, destFile, error) != 0) {    // module是你自定义的LLVMModuleRef对象
            LLVMDisposeMessage(error);
        }
        return result;
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        // 函数信息
        String funcName = ctx.IDENT().getText();
        String retTypeName = ctx.funcType().getText();
        int paramsCount = 0;
        if (ctx.funcFParams() != null) {
            paramsCount = ctx.funcFParams().funcFParam().size();
        }
        //生成返回值类型
        LLVMTypeRef returnType = getTypeRef(retTypeName);
        //生成函数参数类型
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(paramsCount);
        for (int i = 0; i < paramsCount; i++) {
            argumentTypes.put(i, i32Type);
        }
        //生成函数类型
        LLVMTypeRef funcType = LLVMFunctionType(returnType, argumentTypes, paramsCount, 0);
        //生成函数，即向之前创建的module中添加函数
        LLVMValueRef function = LLVMAddFunction(module, funcName, funcType);
        currentScope.define(funcName, function);
        currentFunc = function;
        // 创建基本块
        LLVMBasicBlockRef block = LLVMAppendBasicBlock(function, funcName + "_entry");
        // 在当前基本块插入指令
        LLVMPositionBuilderAtEnd(builder, block);
        currentScope = new LocalScope(currentScope);
        // 添加函数参数定义
        for (int i = 0; i < paramsCount; i++) {
            // 获取参数信息，此处参数仅为int
            SysYParser.FuncFParamContext paramContext = ctx.funcFParams().funcFParam(i);
            String varName = paramContext.IDENT().getText();
            // 定义
            LLVMValueRef varPointer = LLVMBuildAlloca(builder, i32Type, "pointer_" + varName);
            currentScope.define(varName, varPointer);
            LLVMValueRef argValue = LLVMGetParam(function, i);
            LLVMBuildStore(builder, argValue, varPointer);
        }
        hasReturn = false;
        visit(ctx.block());
        if (!hasReturn) {
            LLVMBuildRetVoid(builder);
        }
        currentScope = currentScope.getEnclosingScope();
        return function;
    }

    @Override
    public LLVMValueRef visitBlock(SysYParser.BlockContext ctx) {
        currentScope = new LocalScope(currentScope);
        LLVMValueRef result = super.visitBlock(ctx);
        currentScope = currentScope.getEnclosingScope();
        return result;
    }

    @Override
    public LLVMValueRef visitConstDecl(SysYParser.ConstDeclContext ctx) {
        for (SysYParser.ConstDefContext constDefContext : ctx.constDef()) {
            String varName = constDefContext.IDENT().getText();
            // int 类型
            LLVMTypeRef varType = i32Type;
            // 数组类型
            int elementCount = 0;
            if (constDefContext.constExp() != null && constDefContext.constExp().size() > 0) {
                elementCount = (int) LLVMConstIntGetSExtValue(visit(constDefContext.constExp(0).exp()));
                varType = LLVMVectorType(i32Type, elementCount);
            }

            if (currentScope == globalScope) { // 全局变量
                LLVMValueRef globalVar;
                if (constDefContext.constExp() != null && constDefContext.constExp().size() > 0) {
                    // array
                    LLVMValueRef[] initArray = new LLVMValueRef[elementCount];
                    SysYParser.ArrayConstInitValContext arrayInitValContext = (SysYParser.ArrayConstInitValContext) constDefContext.constInitVal();
                    int initValCount = arrayInitValContext.constInitVal().size();
                    for (int i = 0; i < elementCount; i++) {
                        if (i < initValCount) {
                            initArray[i] = visit(arrayInitValContext.constInitVal(i));
                        } else {
                            initArray[i] = zero;
                        }
                    }
                    globalVar = LLVMAddGlobal(module, varType, varName);
                    LLVMSetInitializer(globalVar, LLVMConstVector(new PointerPointer(initArray), elementCount));
                } else {
                    //创建全局变量
                    globalVar = LLVMAddGlobal(module, varType, varName);
                    //为全局变量设置初始化器
                    LLVMSetInitializer(globalVar, visit(constDefContext.constInitVal()));
                }
                currentScope.define(varName, globalVar);
            } else { // 局部变量
                LLVMValueRef varPointer = LLVMBuildAlloca(builder, varType, "pointer_" + varName);

                SysYParser.ConstInitValContext initValContext = constDefContext.constInitVal();
                if (initValContext instanceof SysYParser.ExpConstInitValContext) {
                    // 整型
                    SysYParser.ExpConstInitValContext expInitValContext = (SysYParser.ExpConstInitValContext) initValContext;
                    LLVMValueRef initVal = visit(expInitValContext.constExp().exp());
                    LLVMBuildStore(builder, initVal, varPointer);
                } else {
                    // 数组
                    SysYParser.ArrayConstInitValContext arrayInitValContext = (SysYParser.ArrayConstInitValContext) initValContext;
                    int initValCount = arrayInitValContext.constInitVal().size();
                    LLVMValueRef[] initArray = new LLVMValueRef[elementCount];
                    for (int i = 0; i < elementCount; i++) {
                        if (i < initValCount) {
                            initArray[i] = visit(arrayInitValContext.constInitVal(i));
                        } else {
                            initArray[i] = zero;
                        }
                    }
                    buildGEP(elementCount, varPointer, initArray);
                }

                currentScope.define(varName, varPointer);
            }
        }
        return null;
    }

    @Override
    public LLVMValueRef visitExpConstInitVal(SysYParser.ExpConstInitValContext ctx) {
        return visit(ctx.constExp().exp());
    }

    @Override
    public LLVMValueRef visitVarDecl(SysYParser.VarDeclContext ctx) {
        for (SysYParser.VarDefContext varDefContext : ctx.varDef()) {
            String varName = varDefContext.IDENT().getText();
            // int 类型
            LLVMTypeRef varType = i32Type;
            // 数组类型
            int elementCount = 0;
            if (varDefContext.constExp() != null && varDefContext.constExp().size() > 0) {
                elementCount = (int) LLVMConstIntGetSExtValue(visit(varDefContext.constExp(0).exp()));
                varType = LLVMVectorType(i32Type, elementCount);
            }

            if (currentScope == globalScope) { // 全局变量
                LLVMValueRef globalVar;
                if (varDefContext.constExp() != null && varDefContext.constExp().size() > 0) {
                    // array
                    LLVMValueRef[] initArray = new LLVMValueRef[elementCount];
                    if (varDefContext.ASSIGN() != null) {
                        SysYParser.ArrayInitValContext arrayInitValContext = (SysYParser.ArrayInitValContext) varDefContext.initVal();
                        int initValCount = arrayInitValContext.initVal().size();
                        for (int i = 0; i < elementCount; i++) {
                            if (i < initValCount) {
                                initArray[i] = visit(arrayInitValContext.initVal(i));
                            } else {
                                initArray[i] = zero;
                            }
                        }
                    } else {
                        // 没有初始化, =0
                        for (int i = 0; i < elementCount; i++) {
                            initArray[i] = zero;
                        }
                    }
                    globalVar = LLVMAddGlobal(module, varType, varName);
                    LLVMSetInitializer(globalVar, LLVMConstVector(new PointerPointer(initArray), elementCount));
                } else {
                    LLVMValueRef value = zero;
                    if (varDefContext.ASSIGN() != null) {
                        value = visit(varDefContext.initVal());
                    }
                    //创建全局变量
                    globalVar = LLVMAddGlobal(module, varType, varName);
                    //为全局变量设置初始化器
                    LLVMSetInitializer(globalVar, value);
                }
                currentScope.define(varName, globalVar);
            } else { // 局部变量
                LLVMValueRef varPointer = LLVMBuildAlloca(builder, varType, "pointer_" + varName);

                if (varDefContext.ASSIGN() != null) {
                    SysYParser.InitValContext initValContext = varDefContext.initVal();
                    if (initValContext instanceof SysYParser.ExpInitValContext) {
                        // 整型
                        SysYParser.ExpInitValContext expInitValContext = (SysYParser.ExpInitValContext) initValContext;
                        LLVMValueRef initVal = visit(expInitValContext.exp());
                        LLVMBuildStore(builder, initVal, varPointer);
                    } else {
                        // 数组
                        SysYParser.ArrayInitValContext arrayInitValContext = (SysYParser.ArrayInitValContext) initValContext;
                        int initValCount = arrayInitValContext.initVal().size();
                        LLVMValueRef[] initArray = new LLVMValueRef[elementCount];
                        for (int i = 0; i < elementCount; i++) {
                            if (i < initValCount) {
                                initArray[i] = visit(arrayInitValContext.initVal(i));
                            } else {
                                initArray[i] = zero;
                            }
                        }
                        buildGEP(elementCount, varPointer, initArray);
                    }
                }

                currentScope.define(varName, varPointer);
            }
        }
        return null;
    }

    @Override
    public LLVMValueRef visitExpInitVal(SysYParser.ExpInitValContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public LLVMValueRef visitReturnStat(SysYParser.ReturnStatContext ctx) {
        hasReturn = true;
        if (ctx.exp() != null) {
            return LLVMBuildRet(builder, visit(ctx.exp()));
        } else {
            return LLVMBuildRetVoid(builder);
        }
    }

    @Override
    public LLVMValueRef visitAssignStat(SysYParser.AssignStatContext ctx) {
        LLVMValueRef lValPointer = visitLVal(ctx.lVal());
        LLVMValueRef rVal = visit(ctx.exp());
        return LLVMBuildStore(builder, rVal, lValPointer);
    }

    @Override
    public LLVMValueRef visitWhileStat(SysYParser.WhileStatContext ctx) {
        LLVMBasicBlockRef whileCondition = LLVMAppendBasicBlock(currentFunc, "while_condition");
        LLVMBasicBlockRef whileBody = LLVMAppendBasicBlock(currentFunc, "while_body");
        LLVMBasicBlockRef next = LLVMAppendBasicBlock(currentFunc, "next");

        whileStack.push(next);

        LLVMBuildBr(builder, whileCondition);
        // 循环条件
        LLVMPositionBuilderAtEnd(builder, whileCondition);
        LLVMValueRef condition = LLVMBuildICmp(builder, LLVMIntNE, visit(ctx.cond()), zero, "icmp_res"); //i8
        LLVMBuildCondBr(builder, condition, whileBody, next);
        // 循环体
        LLVMPositionBuilderAtEnd(builder, whileBody);
        visit(ctx.stmt());
        LLVMBuildBr(builder, whileCondition);

        LLVMPositionBuilderAtEnd(builder, next);

        return null;
    }

    @Override
    public LLVMValueRef visitBreakStat(SysYParser.BreakStatContext ctx) {
        LLVMBuildBr(builder, whileStack.pop());
        return null;
    }

    @Override
    public LLVMValueRef visitContinueStat(SysYParser.ContinueStatContext ctx) {

        return super.visitContinueStat(ctx);
    }

    @Override
    public LLVMValueRef visitIfStat(SysYParser.IfStatContext ctx) {
        LLVMValueRef condition = LLVMBuildICmp(builder, LLVMIntNE, visit(ctx.cond()), zero, "icmp_res"); //i8

        LLVMBasicBlockRef ifTrue = LLVMAppendBasicBlock(currentFunc, "if_true");
        LLVMBasicBlockRef ifFalse = LLVMAppendBasicBlock(currentFunc, "if_false");
        LLVMBasicBlockRef next = LLVMAppendBasicBlock(currentFunc, "next");
        // 跳转
        LLVMBuildCondBr(builder, condition, ifTrue, ifFalse);
        // true
        LLVMPositionBuilderAtEnd(builder, ifTrue);
        visit(ctx.tstst);
        LLVMBuildBr(builder, next);
        // false
        LLVMPositionBuilderAtEnd(builder, ifFalse);
        if (ctx.ELSE() != null) {
            visit(ctx.fstat);
        }
        LLVMBuildBr(builder, next);
        // next
        LLVMPositionBuilderAtEnd(builder, next);

        return null;
    }

    @Override
    public LLVMValueRef visitOrCond(SysYParser.OrCondContext ctx) {
        LLVMValueRef lvalue = visit(ctx.lhs);
        LLVMValueRef rvalue = visit(ctx.rhs);
        return LLVMBuildOr(builder, lvalue, rvalue, "or_res");
    }

    @Override
    public LLVMValueRef visitExpCond(SysYParser.ExpCondContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public LLVMValueRef visitAndCond(SysYParser.AndCondContext ctx) {
        LLVMValueRef lvalue = visit(ctx.lhs);
        LLVMValueRef rvalue = visit(ctx.rhs);
        return LLVMBuildAnd(builder, lvalue, rvalue, "and_res");
    }

    @Override
    public LLVMValueRef visitLTGTLEGECond(SysYParser.LTGTLEGECondContext ctx) {
        LLVMValueRef lvalue = visit(ctx.lhs);
        LLVMValueRef rvalue = visit(ctx.rhs);
        LLVMValueRef result = null; // i8
        if (ctx.op.getType() == SysYParser.LT) {
            result = LLVMBuildICmp(builder, LLVMIntSLT, lvalue, rvalue, "icmp_res");
        } else if (ctx.op.getType() == SysYParser.GT) {
            result = LLVMBuildICmp(builder, LLVMIntSGT, lvalue, rvalue, "icmp_res");
        } else if (ctx.op.getType() == SysYParser.LE) {
            result = LLVMBuildICmp(builder, LLVMIntSLE, lvalue, rvalue, "icmp_res");
        } else if (ctx.op.getType() == SysYParser.GE) {
            result = LLVMBuildICmp(builder, LLVMIntSGE, lvalue, rvalue, "icmp_res");
        }
        result = LLVMBuildZExt(builder, result, i32Type, "zext_res");
        return result;
    }

    @Override
    public LLVMValueRef visitEQNEQCond(SysYParser.EQNEQCondContext ctx) {
        LLVMValueRef lvalue = visit(ctx.lhs);
        LLVMValueRef rvalue = visit(ctx.rhs);
        LLVMValueRef result = null; // i8
        if (ctx.op.getType() == SysYParser.EQ) {
            result = LLVMBuildICmp(builder, LLVMIntEQ, lvalue, rvalue, "icmp_res");
        } else if (ctx.op.getType() == SysYParser.NEQ) {
            result = LLVMBuildICmp(builder, LLVMIntNE, lvalue, rvalue, "icmp_res");
        }
        result = LLVMBuildZExt(builder, result, i32Type, "zext_res");
        return result;
    }

    /**
     * lVal
     * @param ctx the parse tree
     * @return LLVMValueRef
     */
    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx) {
        if (ctx.exp() != null && ctx.exp().size() > 0) {
            // 数组
            LLVMValueRef varPointer = currentScope.resolve(ctx.IDENT().getText());
            PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(zero, visit(ctx.exp(0)));
            return LLVMBuildGEP(builder, varPointer, indexPointer, 2, "pointer_array");
        } else {
            return currentScope.resolve(ctx.IDENT().getText());
        }
    }

    /**
     * exp
     */
    @Override
    public LLVMValueRef visitMulDivModExp(SysYParser.MulDivModExpContext ctx) {
        LLVMValueRef lvalue = visit(ctx.lhs);
        LLVMValueRef rvalue = visit(ctx.rhs);
        LLVMValueRef result = null;
        if (ctx.op.getType() == SysYParser.MUL) {
            result = LLVMBuildMul(builder, lvalue, rvalue, "mul_res");
        } else if (ctx.op.getType() == SysYParser.DIV) {
            result = LLVMBuildSDiv(builder, lvalue, rvalue, "div_res");
        } else if (ctx.op.getType() == SysYParser.MOD) {
            result = LLVMBuildSRem(builder, lvalue, rvalue, "rem_res");
        }
        return result;
    }

    @Override
    public LLVMValueRef visitLeftValExp(SysYParser.LeftValExpContext ctx) {
        LLVMValueRef lValPointer = this.visitLVal(ctx.lVal());
        return LLVMBuildLoad(builder, lValPointer, ctx.lVal().getText());
    }

    @Override
    public LLVMValueRef visitIntegerExp(SysYParser.IntegerExpContext ctx) {
        int number = parseInt(ctx.number().INTEGR_CONST().getText());
        return LLVMConstInt(i32Type, number, 0);
    }

    @Override
    public LLVMValueRef visitParenExp(SysYParser.ParenExpContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public LLVMValueRef visitUnaryOpExp(SysYParser.UnaryOpExpContext ctx) {
        LLVMValueRef value = visit(ctx.exp());
        LLVMValueRef result = null;
        if ("+".equals(ctx.unaryOp().getText())) {
            result = value;
        } else if ("-".equals(ctx.unaryOp().getText())) {
            result = LLVMBuildSub(builder, LLVMConstInt(i32Type, 0, 0), value, "sub_res");
        } else if ("!".equals(ctx.unaryOp().getText())) {
            // 生成 icmp
            LLVMValueRef tmp_ = LLVMBuildICmp(builder, LLVMIntNE, LLVMConstInt(i32Type, 0, 0), value, "icmp_res");
            // 生成 xor
            tmp_ = LLVMBuildXor(builder, tmp_, LLVMConstInt(LLVMInt1Type(), 1, 0), "xor_res");
            // 生成 zext
            result = LLVMBuildZExt(builder, tmp_, i32Type, "zext_res");
        }
        return result;
    }

    @Override
    public LLVMValueRef visitFuncCallExp(SysYParser.FuncCallExpContext ctx) {
        String funcName = ctx.IDENT().getText();
        int argsCount = 0;
        if (ctx.funcRParams() != null) {
            argsCount = ctx.funcRParams().param().size();
        }

        LLVMValueRef function = currentScope.resolve(funcName);
        PointerPointer<Pointer> args = new PointerPointer<>(argsCount);
        for (int i = 0; i < argsCount; i++) {
            SysYParser.ParamContext param = ctx.funcRParams().param(i);
            args.put(i, visit(param.exp()));
        }
        return LLVMBuildCall(builder, function, args, argsCount, "");
    }

    @Override
    public LLVMValueRef visitAddSubExp(SysYParser.AddSubExpContext ctx) {
        LLVMValueRef lvalue = visit(ctx.lhs);
        LLVMValueRef rvalue = visit(ctx.rhs);
        LLVMValueRef result = null;
        if (ctx.op.getType() == SysYParser.PLUS) {
            result = LLVMBuildAdd(builder, lvalue, rvalue, "add_res");
        } else if (ctx.op.getType() == SysYParser.MINUS) {
            result = LLVMBuildSub(builder, lvalue, rvalue, "sub_res");
        }
        return result;
    }

    public Integer parseInt(String text) {
        if (text.startsWith("0x") || text.startsWith("0X")) {
            return Integer.parseInt(text.substring(2), 16);
        } else if (text.startsWith("0") && text.length() > 1) {
            return Integer.parseInt(text.substring(1), 8);
        }
        return Integer.parseInt(text);
    }

    public LLVMTypeRef getTypeRef(String name) {
        if (name.equals("void")) {
            return voidType;
        } else {
            return i32Type;
        }
    }

    private void buildGEP(int elementCount, LLVMValueRef varPointer, LLVMValueRef[] initArray) {
        for (int i = 0; i < elementCount; i++) {
            PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(zero, LLVMConstInt(i32Type, i, 0));
            LLVMValueRef elementPtr = LLVMBuildGEP(builder, varPointer, indexPointer, 2, "GEP_" + i);
            LLVMBuildStore(builder, initArray[i], elementPtr);
        }
    }
}
