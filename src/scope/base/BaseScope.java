package scope.base;

import symbol.base.Symbol;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope{
    private final Scope enclosingScope;
    private final String name;
    private final Map<String, Symbol> symbols = new LinkedHashMap<>();
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
    public Map<String, Symbol> getSymbols() {
        return this.symbols;
    }

    @Override
    public void define(Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
    }

    @Override
    public Symbol resolve(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
            return symbol;
        }
        // 当前作用域没找到
        if (enclosingScope != null) {
            return enclosingScope.resolve(name);
        }
        return null;
    }
}
