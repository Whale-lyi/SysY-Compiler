package symbol.base;

import type.base.Type;

import java.util.ArrayList;

public interface Symbol {
    String getName();
    Type getType();
    void addPosition(Position position);
    boolean checkPosition(Position position);
    ArrayList<Position> getPositions();
}
