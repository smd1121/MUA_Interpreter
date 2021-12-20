package mua;

import mua.types.MuaType;
import mua.types.MuaWord;

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

    public void clear() {
        list.clear();
    }

    public String toString()  {
        StringBuilder returnValue = new StringBuilder();
        list.forEach( (key, value) -> {
            returnValue.append("make \"").append(key).append(" ");
            if (value instanceof MuaWord)
                returnValue.append("\"");
            returnValue.append(value.toString()).append('\n');
        });
        return returnValue.toString();
    }
}
