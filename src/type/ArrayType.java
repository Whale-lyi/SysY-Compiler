package type;

import type.base.BaseType;
import type.base.Type;

public class ArrayType extends BaseType {
    private int count; // -1 代表参数中未声明
    private final Type subType;
    public ArrayType(int count, Type subType) {
        this.count = count;
        this.subType = subType;
        isArray = true;
        this.level = subType.getLevel() + 1;
    }

    public int getCount() {
        return count;
    }

    public Type getSubType() {
        return subType;
    }


}
