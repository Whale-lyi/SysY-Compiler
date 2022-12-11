package symbol;

import symbol.base.BaseSymbol;
import type.base.Type;

public class VariableSymbol extends BaseSymbol {
    public final boolean isConst;
    public VariableSymbol(String name, Type type, boolean isConst) {
        super(name, type);
        this.isConst = isConst;
    }
}
