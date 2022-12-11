package scope;

import scope.base.BaseScope;
import scope.base.Scope;

public class LocalScope extends BaseScope {
    private static int localScopeCounter = 0;
    public LocalScope(Scope enclosingScope) {
        super(enclosingScope, "LocalScope" + localScopeCounter++);
    }
}
