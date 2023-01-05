import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import scope.*;
import scope.base.*;

import static org.bytedeco.llvm.global.LLVM.*;

public class MyIRVisitor extends SysYParserBaseVisitor<LLVMValueRef> {
    private final LLVMModuleRef module = LLVMModuleCreateWithName("module");
    private final LLVMBuilderRef builder = LLVMCreateBuilder();
    private final LLVMTypeRef i32Type = LLVMInt32Type();
    private final LLVMTypeRef voidType = LLVMVoidType();
    private GlobalScope globalScope = null;
    private Scope currentScope = null;
    private final LLVMValueRef zero = LLVMConstInt(i32Type, 0, 0);
    public static final BytePointer error = new BytePointer();
    private String destFile;
    private boolean hasReturn = false;
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
        // 创建基本块
        LLVMBasicBlockRef block = LLVMAppendBasicBlock(function, funcName + "Entry");
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
    public LLVMValueRef visitReturnStat(SysYParser.ReturnStatContext ctx) {
        hasReturn = true;
        if (ctx.exp() != null) {
            return LLVMBuildRet(builder, visit(ctx.exp()));
        } else {
            return LLVMBuildRetVoid(builder);
        }
    }

    /**
     * lVal
     * @param ctx the parse tree
     * @return LLVMValueRef
     */
    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx) {
        // TODO: 数组
        return currentScope.resolve(ctx.IDENT().getText());
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
            result = LLVMBuildMul(builder, lvalue, rvalue, "result");
        } else if (ctx.op.getType() == SysYParser.DIV) {
            result = LLVMBuildSDiv(builder, lvalue, rvalue, "result");
        } else if (ctx.op.getType() == SysYParser.MOD) {
            result = LLVMBuildSRem(builder, lvalue, rvalue, "result");
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
            result = LLVMBuildSub(builder, LLVMConstInt(i32Type, 0, 0), value, "result");
        } else if ("!".equals(ctx.unaryOp().getText())) {
            // 生成 icmp
            LLVMValueRef tmp_ = LLVMBuildICmp(builder, LLVMIntNE, LLVMConstInt(i32Type, 0, 0), value, "tmp_");
            // 生成 xor
            tmp_ = LLVMBuildXor(builder, tmp_, LLVMConstInt(LLVMInt1Type(), 1, 0), "tmp_");
            // 生成 zext
            result = LLVMBuildZExt(builder, tmp_, i32Type, "result");
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
            result = LLVMBuildAdd(builder, lvalue, rvalue, "result");
        } else if (ctx.op.getType() == SysYParser.MINUS) {
            result = LLVMBuildSub(builder, lvalue, rvalue, "result");
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
        LLVMValueRef[] arrayPointer = new LLVMValueRef[2];
        arrayPointer[0] = zero;
        for (int i = 0; i < elementCount; i++) {
            arrayPointer[1] = LLVMConstInt(i32Type, i, 0);
            PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayPointer);
            LLVMValueRef elementPtr = LLVMBuildGEP(builder, varPointer, indexPointer, 2, "GEP_" + i);
            LLVMBuildStore(builder, initArray[i], elementPtr);
        }
    }
}
