package mua.types;

import mua.SymbolList;
import org.jetbrains.annotations.NotNull;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mua.Main.errorAndExit;

/**
 * List or function
 */
public class MuaList extends MuaType {
    public enum ListType {MUA_EMPTY_LIST, MUA_SINGLE_ITEM_LIST, MUA_FUNC, MUA_OTHER_LIST}

    // for all lists:
    String value;
    ListType listType;

    // for function:
    String func_code;
    String[] paramList;

    // for closure:
    SymbolList context;

    public MuaList(String value) {
        this.value = value;
        context = null;
        paramList = null;
        func_code = null;
        listType = parseList();
    }

    public MuaList(String value, ListType listType, String func_code, String[] paramList, SymbolList context) {
        this.value = value;
        this.listType = listType;
        this.func_code = func_code;
        this.paramList = paramList;
        this.context = context;
    }

    public String getFunc_code() {
        return func_code;
    }

    public String[] getParamList() {
        return paramList;
    }

    public SymbolList getContext() {
        return context;
    }

    public ListType getListType() {
        return listType;
    }

    @Override
    public String toString() {
        return value;
    }

    public String toStringForPrint() {
        Pattern pattern = Pattern.compile("\\s*\\[(.*)]\\s*");
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        else {
            return errorAndExit("Runtime error: failed to get list content.").toString();
        }
    }

    @NotNull
    public MuaType makeCopy() {
        return new MuaList(value, listType, func_code, paramList, context);
    }

    /**
     * @return
     * MUA_EMPTY_LIST if the list looks like [ ]
     * MUA_SINGLE_ITEM_LIST if the list looks like [ [ foo ] ] or [ some_single_thing ]
     * MUA_FUNCTION if the list looks like [ [ foo(param) ] [ foo(codes) ] ]
     * MUA_OTHER_LIST if none of above matches
     */
    private ListType parseList() {
        if (Pattern.matches("\\s*\\[\\s*]\\s*", value))
            return ListType.MUA_EMPTY_LIST;
        if (Pattern.matches("\\s*\\[\\s*\\[[^]]*]\\s*]\\s*", value))
            return ListType.MUA_SINGLE_ITEM_LIST;
        if (Pattern.matches("\\s*\\[\\s*\\S*\\s*]\\s*", value))
            return ListType.MUA_SINGLE_ITEM_LIST;
        if (Pattern.matches("\\s*\\[\\s*\\[([^]]*)]\\s*\\[(.*)]\\s*]\\s*", value)) {
            parseFunc();
            return ListType.MUA_FUNC;
        }
        return ListType.MUA_OTHER_LIST;
    }

    // for function:
    private void parseFunc() {
        Pattern pattern = Pattern.compile("\\s*\\[\\s*\\[([^]]*)]\\s*\\[(.*)]\\s*]\\s*");
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            String paramListStr = matcher.group(1);
            func_code = matcher.group(2);
            paramList = paramListStr.split("\\s+");
        }
        else {
            errorAndExit("Runtime error: failed to parse function.");
        }
    }

    // for closure:
    public Vector<String> getContextList() {
        Pattern pattern = Pattern.compile(":([^\\s\\[\\]:]+)|(\\s*thing\\s+)+([^\\s\\[\\]:]+)|([a-zA-Z0-9_]+)");
        Matcher matcher = pattern.matcher(func_code);
        Vector<String> contextList = new Vector<>();
        while (matcher.find()) {
            if (matcher.group(1) != null)
                contextList.add(matcher.group(1));
            else if (matcher.group(3) != null)
                contextList.add(matcher.group(3));
            else
                contextList.add(matcher.group(4));
        }
        if (contextList.isEmpty())
            return null;
        return contextList;
    }

    public void setContext(SymbolList context) {
        this.context = context;
    }
}
