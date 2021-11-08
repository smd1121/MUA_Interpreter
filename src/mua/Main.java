package mua;

import mua.types.*;
import org.jetbrains.annotations.NotNull;

import javax.management.loading.MLet;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static Scanner scanner;
    private static SymbolList globalSymbolList;
    private static SymbolList localSymbolList;
    private static SymbolList contextSymbolList;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);
        globalSymbolList = new SymbolList();
        muaProgram();
    }

    @NotNull
    public static MuaException errorAndExit(String msg) {
        System.out.println(msg);
        System.out.println("Program terminated.");
        System.exit(1);
        return new MuaException();
    }

    /**
     * Get the whole MUA program.
     */
    private static MuaType muaProgram() {
        MuaType result = null, lastResult = null;
        while (true) {
            result = muaSentence();
            if (result == null)
                return lastResult;
            lastResult = result;
        }
    }

    private static boolean returnCalled = false;
    /**
     * Get 1 sentence.
     * @return null if there is no more input.
     */
    private static MuaType muaSentence() {
        if (!scanner.hasNext() || returnCalled) {
            returnCalled = false;
            return null;
        }
        return muaExpression(null, true);
    }

    /**
     * Get 1 expression.
     * @param first The first string of this expression if read by the caller.
     *           Normally, it should be null.
     * @param isNameFuncCall Whether a string w/o quotation mark is regarded as a func call or a name.
     *           Normally, it should be true.
     * @return The return value of the expression.
     */
    @SuppressWarnings("DuplicateBranchesInSwitch")
    @NotNull
    private static MuaType muaExpression(String first, Boolean isNameFuncCall) {
        if (first == null) {
            if (!scanner.hasNext())
                return errorAndExit("Invalid input.");
            first = scanner.next();
        }
        switch (first) {
            case "make": {
                return muaMake();
            }
            case "thing": {
                return muaThing(null);
            }
            case "print": {
                MuaType val = muaExpression(null, true);
                System.out.println(val.toString());
                return val;
            }
            case "erase": {
                return muaErase();
            }
            case "run": {
                MuaType val = muaExpression(null, true);
                if (val instanceof MuaList) {
                    return muaRun(getMuaListContent((MuaList) val), false);
                }
                else
                    return errorAndExit("Invalid operand for run.");
            }
            case "if": {
                MuaType condition = muaExpression(null, true),
                        ifTrue = muaExpression(null, true),
                        ifFalse = muaExpression(null, true);
                MuaBool parsedCond = muaParseBool(condition);
                if (parsedCond == null || !(ifTrue instanceof MuaList) || !(ifFalse instanceof MuaList))
                    return errorAndExit("Invalid operand for if.");
                if (parsedCond.getValue())
                    return muaRun(getMuaListContent((MuaList) ifTrue), false);
                else
                    return muaRun(getMuaListContent((MuaList) ifFalse), false);
            }
            case "return": {
                returnCalled = true;
                return muaExpression(null, true);
            }
            case "export": {
                MuaType name = muaExpression(null, true);
                MuaType val = localSymbolList.find(name.toString());
                if (val == null)
                    return errorAndExit("Local variable " + name.toString() + " not found.");
                muaAddSymbol(name.toString(), val, symbolTbl.MUA_GLOBAL);
                return val;
            }
            case "add":
            case "sub":
            case "mul":
            case "div":
            case "mod": {
                return muaBinaryOperation(first);
            }
            case "random": {
                MuaType num = muaExpression(null, true);
                if (!(num instanceof MuaNumber))
                    return errorAndExit("Invalid operand for random.");
                else
                    return new MuaNumber(Math.random() * ((MuaNumber) num).getValue());
            }
            case "int": {
                MuaType num = muaExpression(null, true);
                if (!(num instanceof MuaNumber))
                    return errorAndExit("Invalid operand for int.");
                else
                    return new MuaNumber(Math.floor(((MuaNumber) num).getValue()));
            }
            case "sqrt": {
                MuaType num = muaExpression(null, true);
                if (!(num instanceof MuaNumber))
                    return errorAndExit("Invalid operand for sqrt.");
                else
                    return new MuaNumber(Math.sqrt(((MuaNumber) num).getValue()));
            }
            case "read": {
                String str;
                if (scanner.hasNext())
                    str = scanner.next();
                else
                    return errorAndExit("Invalid input.");
                if (Pattern.matches("-?[0-9]+\\.?[0-9]*", str))
                    return new MuaNumber(Double.parseDouble(str));
                return new MuaWord(str);
            }
            case "load": {
                // TODO: load
                break;
            }
            case "erall": {
                // TODO: erall
                break;
            }
            case "eq":
            case "gt":
            case "lt": {
                MuaType opr1 = muaExpression(null, true);
                MuaType opr2 = muaExpression(null, true);
                if (opr1 instanceof MuaNumber && opr2 instanceof MuaNumber)
                    return new MuaBool(muaCompare(first, ((MuaNumber) opr1).getValue(), ((MuaNumber) opr2).getValue()));
                else
                    return new MuaBool(muaCompare(first, opr1.toString(), opr2.toString()));
            }
            case "and":
            case "or": {
                MuaType opr1 = muaExpression(null, true);
                MuaType opr2 = muaExpression(null, true);
                if (opr1 instanceof MuaBool && opr2 instanceof MuaBool)
                    if (first.equals("and"))
                        return new MuaBool(((MuaBool) opr1).getValue() && ((MuaBool) opr2).getValue());
                    else
                        return new MuaBool(((MuaBool) opr1).getValue() || ((MuaBool) opr2).getValue());
                else {
                    return errorAndExit("Invalid operands for " + first + ".");
                }
            }
            case "isnumber": {
                MuaType opr = muaExpression(null, true);
                return new MuaBool(opr instanceof MuaNumber);
            }
            case "isword": {
                MuaType opr = muaExpression(null, true);
                return new MuaBool(opr instanceof MuaWord);
            }
            case "islist": {
                MuaType opr = muaExpression(null, true);
                return new MuaBool(opr instanceof MuaList);
            }
            case "isbool": {
                MuaType opr = muaExpression(null, true);
                return new MuaBool(opr instanceof MuaBool);
            }
            case "isempty": {
                break;
            }
            case "isname": {
                return new MuaBool(isExistingMuaName(null));
            }
            case "not": {
                MuaType opr = muaExpression(null, true);
                if (opr instanceof MuaBool)
                        return new MuaBool(!((MuaBool) opr).getValue());
                else {
                    return errorAndExit("Invalid operands for " + first + ".");
                }
            }
            case "sentence": {
                // TODO: sentence
                break;
            }
            case "list": {
                // TODO: list
                break;
            }
            case "join": {
                // TODO: join
                break;
            }
            case "butfirst":
            case "butlast":
            case "first":
            case "last": {
                // TODO 4
                break;
            }
            case "readlist": {
                // TODO 4
                break;
            }
            case "poall": {
                // TODO 4
                break;
            }
            case "word": {
                // TODO 4
                break;
            }
            case "save": {
                // TODO 4
                break;
            }
            // boolean literal
            case "true": {
                return new MuaBool(true);
            }
            case "false": {
                return new MuaBool(false);
            }
            /* ":" OR <name> (func call)
             * OR <number> OR <list> OR <word>
             */
            default: {
                char firstCh = first.charAt(0);
                if (firstCh == ':') {   // ":"
                    return muaThing(first.substring(1));
                }
                else if (firstCh == '[') {    // <list>
                    String list = first;
                    while (countCharInString('[', list) != countCharInString(']', list)) {
                        if (!scanner.hasNext())
                            return errorAndExit("Wrong list format.");
                        list = list + ' ' + scanner.next();
                    }
                    return new MuaList(list);
                }
                else if (firstCh == '"') {    // <word>
                    return new MuaWord(first.substring(1));
                }
                else if (Character.isDigit(firstCh)) {    // <number>
                    try {
                        double number = Double.parseDouble(first);
                        return new MuaNumber(number);
                    } catch (Exception e) {
                        return errorAndExit("Invalid input: " + first + ".");
                    }
                }
                else if (Character.isLetter(firstCh)) {   // <name>
                    if (isNameFuncCall) {
                        // TODO 4: Function Call
                        MuaType func = muaGetSymbol(first, symbolTbl.MUA_LOCAL);
                        if (!(func instanceof MuaList) || ((MuaList) func).getListType() != MuaList.ListType.MUA_FUNC)
                            return errorAndExit(first + "doesn't name a function.");
                        return muaCallFunc((MuaList) func);
                    } else {
                        return new MuaWord(first);
                    }
                }
                else {
                    return errorAndExit("Unknown input " + first);
                }
            }
        }
        // TODO 5: After finishing all cases, delete the following:
        return errorAndExit("Not implemented yet.");
    }

    /**
     * make <name> <value> (with "make" read)
     * @return <value>
     */
    @NotNull
    private static MuaType muaMake() {
        MuaType name = muaExpression(null, true);
        MuaType val = muaExpression(null, true);
        if (isValidMuaName(name))
            muaAddSymbol(name.toString(), val, symbolTbl.MUA_LOCAL);
        else
            return errorAndExit("[Error] Invalid <name> for operation make.");

        // if is an inner function, save the required context
        if (val instanceof MuaList
                && ((MuaList) val).getListType() == MuaList.ListType.MUA_FUNC
                && localSymbolList != null) {
            muaSaveContext((MuaList) val);
        }

        return val;
    }

    private static void muaSaveContext(MuaList func) {
        Vector<String> contextList = func.getContextList();
        if (contextList == null)
            return;

        SymbolList symbolList = new SymbolList();
        for (String name : contextList) {
            MuaType val = muaGetSymbol(name, symbolTbl.MUA_GLOBAL);
            if (val == null) {
                errorAndExit("Name " + name + " not found.");
            }
            else {
                symbolList.put(name, val.makeCopy());
            }
        }

        func.setContext(symbolList);
    }

    /**
     *
     */
    @NotNull
    private static MuaType muaThing(String first) {
        MuaType name = muaExpression(first, false);
        if (isValidMuaName(name)) {
            MuaType result = muaGetSymbol(name.toString(), symbolTbl.MUA_LOCAL);
            if (result == null) {
                return errorAndExit("Name " + name.toString() + " not found.");
            }
            return result;
        } else {
            return errorAndExit("[Error] Invalid <name> for operation thing.");
        }
    }

    @NotNull
    private static MuaNumber muaBinaryOperation(String op) {
        MuaType opr1 = muaExpression(null, true);
        MuaType opr2 = muaExpression(null, true);
        if (opr1 instanceof MuaWord) {
            try {
                double number = Double.parseDouble(opr1.toString());
                opr1 = new MuaNumber(number);
            } catch (Exception e) {
                errorAndExit("Operands " + opr1.toString() + " of " + op + " is not a number.");
            }
        }
        if (opr2 instanceof MuaWord) {
            try {
                double number = Double.parseDouble(opr2.toString());
                opr2 = new MuaNumber(number);
            } catch (Exception e) {
                errorAndExit("Operands " + opr2.toString() + " of " + op + " is not a number.");
            }
        }

        if (opr1 instanceof MuaNumber && opr2 instanceof MuaNumber) {
            switch (op) {
                case "add": {
                    return new MuaNumber(((MuaNumber) opr1).getValue() + ((MuaNumber) opr2).getValue());
                }
                case "sub": {
                    return new MuaNumber(((MuaNumber) opr1).getValue() - ((MuaNumber) opr2).getValue());
                }
                case "mul": {
                    return new MuaNumber(((MuaNumber) opr1).getValue() * ((MuaNumber) opr2).getValue());
                }
                case "div": {
                    try {
                        return new MuaNumber(((MuaNumber) opr1).getValue() / ((MuaNumber) opr2).getValue());
                    } catch (ArithmeticException e) {
                        errorAndExit("Divided by 0.");
                    }
                }
                case "mod": {
                    return new MuaNumber(Math.round(((MuaNumber) opr1).getValue())
                            % Math.round(((MuaNumber) opr2).getValue()));
                }
                default: {
                    errorAndExit("Invalid operation: " + op + ".");
                    return new MuaNumber(Double.NaN);
                }
            }
        } else {
            errorAndExit("Operands of " + op + " are not numbers.");
            return new MuaNumber(Double.NaN);
        }
    }

    @NotNull
    private static MuaType muaErase() {
        MuaType name = muaExpression(null, true);
        MuaType result = muaRemoveSymbol(name.toString());
        if (result == null) {
            return errorAndExit("Name " + name.toString() + " not found.");
        }
        return result;
    }

    /**
     * @Return The number of char "ch" in String "str"
     */
    private static int countCharInString(char ch, String str) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (ch == str.charAt(i))
                count++;
        }
        return count;
    }

    public static Boolean isValidMuaName(MuaType obj) {
        return (obj instanceof MuaWord
                && Pattern.matches("[a-zA-Z][a-zA-Z0-9_]*", obj.toString()));
    }

    private static Boolean isExistingMuaName(String name) {
        if (name == null) {
            name = muaExpression(null, true).toString();
        }
        return muaGetSymbol(name, symbolTbl.MUA_LOCAL) != null;
    }

    private static Boolean muaCompare(String op, double val1, double val2) {
        switch (op) {
            case "eq":
                return Double.doubleToLongBits(val1) == Double.doubleToLongBits(val2);
            case "gt":
                return Double.doubleToLongBits(val1) > Double.doubleToLongBits(val2);
            case "lt":
                return Double.doubleToLongBits(val1) < Double.doubleToLongBits(val2);
        }
        return false;
    }

    private static Boolean muaCompare(String op, String val1, String val2) {
        int result = val1.compareTo(val2);
        switch (op) {
            case "eq":
                return result == 0;
            case "gt":
                return result > 0;
            case "lt":
                return result < 0;
        }
        return false;
    }

    private static void muaAddSymbol(String name, MuaType val, symbolTbl tbl) {
        if (tbl == symbolTbl.MUA_GLOBAL || localSymbolList == null) {
            globalSymbolList.put(name, val);
        }
        else {
            localSymbolList.put(name, val);
        }
    }

    private static MuaType muaGetSymbol(String name, symbolTbl startTbl) {
        if (startTbl == symbolTbl.MUA_GLOBAL)
            return globalSymbolList.find(name);

        MuaType result = null;
        if (localSymbolList != null)
            result = localSymbolList.find(name);
        if (result == null && contextSymbolList != null)
            result = contextSymbolList.find(name);
        if (result == null)
            result = globalSymbolList.find(name);
        return result;
    }

    private static MuaType muaRemoveSymbol(String name) {
        MuaType result = null;
        if (localSymbolList != null)
            result = localSymbolList.remove(name);
        if (result == null && contextSymbolList != null)
            result = contextSymbolList.remove(name);
        if (result == null)
            result = globalSymbolList.remove(name);
        return result;
    }

    private static MuaType muaRun(String code, boolean hasCreatedLocalTbl) {
        Scanner oldScanner = scanner;
        scanner = new Scanner(code);
        MuaType result;

        if (!hasCreatedLocalTbl) {
            SymbolList oldLocalTbl = localSymbolList;
            localSymbolList = new SymbolList();
            result = muaProgram();
            localSymbolList = oldLocalTbl;
        }
        else {
            result = muaProgram();
        }
        scanner = oldScanner;

        return result;
    }

    /**
     * @param val to be parsed
     * @return null if cannot parse
     */
    private static MuaBool muaParseBool(MuaType val) {
        if (val instanceof MuaBool)
            return (MuaBool) val;
        if (val instanceof MuaList) {
            if (((MuaList) val).getListType() == MuaList.ListType.MUA_SINGLE_ITEM_LIST) {
                Pattern pattern = Pattern.compile("\\s*\\[\\s*(\\S*)\\s*]\\s*");
                Matcher matcher = pattern.matcher(val.toString());
                if (!matcher.matches())
                    errorAndExit("Failed to parse list " + val.toString());
                if (matcher.group(1).equalsIgnoreCase("true"))
                    return new MuaBool(true);
                else if (matcher.group(1).equalsIgnoreCase("false"))
                    return new MuaBool(false);
            }
            return null;
        }
        if (val instanceof MuaWord) {
            if (((MuaWord) val).getValue().equalsIgnoreCase("true"))
                return new MuaBool(true);
            else if (((MuaWord) val).getValue().equalsIgnoreCase("false"))
                return new MuaBool(false);
            else
                return null;
        }
        return null;
    }

    @NotNull
    private static String getMuaListContent(MuaList val) {
        Pattern pattern = Pattern.compile("\\s*\\[(.*)]\\s*");
        Matcher matcher = pattern.matcher(val.toString());
        if(!matcher.matches())
            errorAndExit("Failed to get the content of list " + val.toString());
        return matcher.group(1);
    }

    @NotNull
    private static MuaType muaCallFunc(MuaList func) {
        SymbolList oldLocalTbl = localSymbolList;
        localSymbolList = new SymbolList();
        for (String name : func.getParamList()) {
            MuaType val = muaExpression(null, true);
            muaAddSymbol(name, val, symbolTbl.MUA_LOCAL);
        }

        contextSymbolList = func.getContext();

        MuaType result = muaRun(func.getFunc_code(), true);

        contextSymbolList = null;
        localSymbolList = oldLocalTbl;

        return result;
    }

    private enum symbolTbl {MUA_LOCAL, MUA_GLOBAL}
}
