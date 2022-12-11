package type;

import type.base.BaseType;
import type.base.Type;

public class ArrayType extends BaseType {
    private final int count;
    private final Type subType;
    public ArrayType(int count, Type subType) {
        this.count = count;
        this.subType = subType;
    }
}
