package symbol.base;

import type.base.Type;

public interface Symbol {
    String getName();
    Type getType();
    void addPosition(Position position);
    boolean checkPosition(Position position);
}
