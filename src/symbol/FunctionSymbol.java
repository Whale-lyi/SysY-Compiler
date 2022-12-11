package symbol;

import scope.base.BaseScope;
import scope.base.Scope;
import symbol.base.Position;
import symbol.base.Symbol;
import type.base.Type;

import java.util.ArrayList;

public class FunctionSymbol extends BaseScope implements Symbol {
    private final Type type;
    private final ArrayList<Position> positions;
    public FunctionSymbol(Scope enclosingScope, String name, Type type) {
        super(enclosingScope, name);
        this.type = type;
        this.positions = new ArrayList<>();
    }
    @Override
    public Type getType() {
        return this.getType();
    }

    @Override
    public void addPosition(Position position) {
        positions.add(position);
    }

    @Override
    public boolean checkPosition(Position position) {
        for (Position pos : positions) {
            if (pos.equals(position)) {
                return true;
            }
        }
        return false;
    }
}
