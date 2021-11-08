package mua;

import mua.types.MuaType;
import java.util.HashMap;
import java.util.Map;

public class SymbolList {
    private final Map<String, MuaType> list = new HashMap<>();

    public void put(String str, MuaType val) {
        list.put(str, val);
    }

    /**
     * @return NULL if there is no corresponding item.
     */
    public MuaType find(String str) {
        return list.get(str);
    }

    /**
     * @return NULL if there is no corresponding item.
     */
    public MuaType remove(String str) { return list.remove(str); }
}
