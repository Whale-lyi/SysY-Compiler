package scope.base;

import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope{
    private final Scope enclosingScope;
    private final String name;
    private final Map<String, LLVMValueRef> valueRefMap = new LinkedHashMap<>();
    public BaseScope(Scope enclosingScope, String name) {
        this.enclosingScope = enclosingScope;
        this.name = name;
    }
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Scope getEnclosingScope() {
        return this.enclosingScope;
    }

    @Override
    public Map<String, LLVMValueRef> getValueRefMap() {
        return this.valueRefMap;
    }

    @Override
    public void define(String name, LLVMValueRef valueRef) {
        valueRefMap.put(name, valueRef);
    }

    @Override
    public LLVMValueRef resolve(String name) {
        LLVMValueRef valueRef = valueRefMap.get(name);
        if (valueRef != null) {
            return valueRef;
        }
        // 当前作用域没找到
        if (enclosingScope != null) {
            return enclosingScope.resolve(name);
        }
        return null;
    }

    @Override
    public LLVMValueRef resolveInCurScope(String name) {
        LLVMValueRef valueRef = valueRefMap.get(name);
        if (valueRef != null) {
            return valueRef;
        }
        return null;
    }
}
