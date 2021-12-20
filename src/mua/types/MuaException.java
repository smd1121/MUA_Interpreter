package mua.types;

//import org.jetbrains.annotations.NotNull;

public class MuaException extends MuaType {
    public String toString() {
        return "Exception caught!";
    }

    //@NotNull
    @Override
    public MuaType makeCopy() {
        return this;
    }
}
