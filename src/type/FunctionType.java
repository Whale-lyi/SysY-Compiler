package type;

import type.base.BaseType;
import type.base.Type;

import java.util.ArrayList;

public class FunctionType extends BaseType {
    private final Type retTy;
    private ArrayList<Type> paramsType;
    public FunctionType(Type retTy, ArrayList<Type> paramsType) {
        this.retTy = retTy;
        this.paramsType = paramsType;
    }

    public void setParamsType(ArrayList<Type> paramsType) {
        this.paramsType = paramsType;
    }

    public Type getRetTy() {
        return retTy;
    }

    public ArrayList<Type> getParamsType() {
        return paramsType;
    }
}
