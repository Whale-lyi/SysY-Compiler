package scope.base;

import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.Map;

public interface Scope {
    String getName();
    Scope getEnclosingScope();
    Map<String, LLVMValueRef> getValueRefMap();
    void define(String name, LLVMValueRef valueRef);
    LLVMValueRef resolve(String name);
    LLVMValueRef resolveInCurScope(String name);
}
