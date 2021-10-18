package types;

public class MuaNumber extends MuaType {
    double value;

    public MuaNumber(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }
}
