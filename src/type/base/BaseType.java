package type.base;

public class BaseType implements Type{
    public boolean isArray = false;
    public boolean isFunction = false;
    public int level = 0;

    @Override
    public boolean getIsArray() {
        return isArray;
    }

    @Override
    public boolean getIsFunction() {
        return isFunction;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public boolean equals(Type type) {
        return false;
    }
}
