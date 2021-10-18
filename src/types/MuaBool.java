package types;

public class MuaBool extends MuaType {
    Boolean value;

    public MuaBool(Boolean value) {
        this.value = value;
    }

    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
