import types.MuaType;

import java.util.HashMap;
import java.util.Map;

/*
 *
 */
public class SymbolList {
    private Map<String, MuaType> list = new HashMap<>();

    public void addToList(String str, MuaType val) {
        list.put(str, val);
    }

    /*
     * @return NULL if there is no corresponding item.
     */
    public MuaType find(String str) {
        return list.get(str);
    }
}
