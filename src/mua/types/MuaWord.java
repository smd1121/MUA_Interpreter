package mua.types;

import org.jetbrains.annotations.NotNull;

public class MuaWord extends MuaType {
    String value;

    public MuaWord(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @NotNull
    public MuaType makeCopy() {
        return new MuaWord(value);
    }
}
