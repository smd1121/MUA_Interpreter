package mua.types;

import org.jetbrains.annotations.NotNull;

public abstract class MuaType {
    public abstract String toString();
    @NotNull
    public abstract MuaType makeCopy();
}
