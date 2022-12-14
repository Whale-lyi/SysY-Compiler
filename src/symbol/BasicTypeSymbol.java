package symbol;

import symbol.base.BaseSymbol;
import type.base.Type;

public class BasicTypeSymbol extends BaseSymbol implements Type {
    public BasicTypeSymbol(String name) {
        super(name, null);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean getIsArray() {
        return false;
    }

    @Override
    public boolean getIsFunction() {
        return false;
    }

    @Override
    public int getLevel() {
        return 0;
    }
}
