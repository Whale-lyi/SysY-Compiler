package scope;

import scope.base.BaseScope;
import scope.base.Scope;
import symbol.BasicTypeSymbol;

public class GlobalScope extends BaseScope {

    public GlobalScope() {
        super(null, "GlobalScope");
        define(new BasicTypeSymbol("int"));
        define(new BasicTypeSymbol("void"));
    }
}
