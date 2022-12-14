package scope.base;

import symbol.base.Symbol;

import java.util.Map;

public interface Scope {
    String getName();
    Scope getEnclosingScope();
    Map<String, Symbol> getSymbols();
    void define(Symbol symbol);
    Symbol resolve(String name);
    Symbol resolveInCurScope(String name);
}
