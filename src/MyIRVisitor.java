import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import static org.bytedeco.llvm.global.LLVM.*;

public class MyIRVisitor extends SysYParserBaseVisitor<LLVMValueRef> {
    private LLVMModuleRef module = LLVMModuleCreateWithName("module");
    private LLVMBuilderRef builder = LLVMCreateBuilder();
    private LLVMTypeRef i32Type = LLVMInt32Type();
    public static final BytePointer error = new BytePointer();
    private String destFile;
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
        LLVMValueRef result = super.visitProgram(ctx);

        if (LLVMPrintModuleToFile(module, destFile, error) != 0) {    // module是你自定义的LLVMModuleRef对象
            LLVMDisposeMessage(error);
        }
//        LLVMDumpModule(module);

        return result;
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        //生成返回值类型
        LLVMTypeRef returnType = i32Type;
        //生成函数参数类型
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(0);
        //生成函数类型
        LLVMTypeRef ft = LLVMFunctionType(returnType, argumentTypes, 0, 0);
        //生成函数，即向之前创建的module中添加函数
        LLVMValueRef function = LLVMAddFunction(module, ctx.IDENT().getText(), ft);
        // 创建基本块
        LLVMBasicBlockRef block = LLVMAppendBasicBlock(function, "mainEntry");
        // 在当前基本块插入指令
        LLVMPositionBuilderAtEnd(builder, block);

        return super.visitFuncDef(ctx);
    }

    @Override
    public LLVMValueRef visitReturnStat(SysYParser.ReturnStatContext ctx) {
        if (ctx.exp() != null) {
            LLVMBuildRet(builder, visit(ctx.exp()));
        }
        return null;
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
        // TODO
        return super.visitLeftValExp(ctx);
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
        // TODO
        return super.visitFuncCallExp(ctx);
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

    public static Integer parseInt(String text) {
        if (text.startsWith("0x") || text.startsWith("0X")) {
            return Integer.parseInt(text.substring(2), 16);
        } else if (text.startsWith("0") && text.length() > 1) {
            return Integer.parseInt(text.substring(1), 8);
        }
        return Integer.parseInt(text);
    }
}
