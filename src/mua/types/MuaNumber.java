package mua.types;

//import org.jetbrains.annotations.NotNull;

public class MuaNumber extends MuaType {
    double value;

    public MuaNumber(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    //@NotNull
    public MuaType makeCopy() {
        return new MuaNumber(value);
    }
}
