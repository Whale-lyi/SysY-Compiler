package symbol.base;

import type.base.Type;

import java.util.ArrayList;

public class BaseSymbol implements Symbol{
    private final String name;
    private final Type type;
    private final ArrayList<Position> positions;
    public BaseSymbol(String name, Type type) {
        this.name = name;
        this.type = type;
        positions = new ArrayList<>();
    }
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Type getType() {
        return this.type;
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

    public ArrayList<Position> getPositions() {
        return positions;
    }
}
