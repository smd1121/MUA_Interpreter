package mua.types;

import org.jetbrains.annotations.NotNull;

public class MuaVoid extends MuaType {
    public void getValue() {}

    @Override
    public String toString() { return null; }

    @NotNull
    public MuaType makeCopy() {
        return this;
    }
}