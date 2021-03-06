package mua;

import mua.types.*;
//import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Scanner;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mua.types.MuaList.ListType.MUA_EMPTY_LIST;

public class Main {
    private static Scanner scanner;
    private static SymbolList globalSymbolList;
    private static SymbolList localSymbolList;
    private static Stack<SymbolList> contextSymbolListStack;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);
        globalSymbolList = new SymbolList();
        contextSymbolListStack = new Stack<>();

        globalSymbolList.put("pi", new MuaNumber(3.14159));

        muaProgram();
    }

    //@NotNull
    public static MuaException errorAndExit(String msg) {
        System.out.println("[Error] " + msg);
        System.out.println("Next several operations:");
        for (int i = 0; i < 5 && scanner.hasNext(); i++) {
            System.out.print("\t" + scanner.next());
        }
        System.out.println("\nProgram terminated.");
        System.exit(1);
        return new MuaException();
    }

    /**
     * Get the whole MUA program.
     */
    private static MuaType muaProgram() {
        MuaType result, lastResult = null;
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
    //@NotNull
    private static MuaType muaExpression(String first, Boolean isNameFuncCall) {
        if (first == null) {
            if (!scanner.hasNext())
                return errorAndExit("Invalid input.");
            first = scanner.next();
        }
        switch (first) {
            case "___DEBUG___": {
                return new MuaVoid();
            }
            case "make": {
                return muaMake();
            }
            case "thing": {
                return muaThing(null);
            }
            case "print": {
                MuaType val = muaExpression(null, true);
                if (val instanceof MuaList)
                    System.out.println(((MuaList) val).toStringForPrint());
                else
                    System.out.println(val.toString());
                return val;
            }
            case "erase": {
                return muaErase();
            }
            case "run": {
                MuaType val = muaExpression(null, true);
                if (val instanceof MuaList) {
                    return muaRun(getMuaListContent((MuaList) val));
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
                MuaList toRun = (MuaList)(parsedCond.getValue() ? ifTrue : ifFalse);
                if (toRun.getListType() == MUA_EMPTY_LIST)
                    return toRun;

                String toRunContent = getMuaListContent(toRun);
                if (toRun.getListType() == MuaList.ListType.MUA_SINGLE_ITEM_LIST)
                    return new MuaWord(toRunContent.trim());

                return muaRun(toRunContent);
            }
            case "return": {
                MuaType result = muaExpression(null, true);
                returnCalled = true;
                return result;
            }
            case "export": {
                MuaType name = muaExpression(null, true);
                if (localSymbolList == null)
                    return errorAndExit("Invalid usage for export.");
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
                // TODO f: read in run may cause error
                if (scanner.hasNext())
                    str = scanner.next();
                else
                    return errorAndExit("Invalid input.");
                if (Pattern.matches("-?[0-9]+\\.?[0-9]*", str))
                    return new MuaNumber(Double.parseDouble(str));
                return new MuaWord(str);
            }
            case "load": {
                MuaType fileName = muaExpression(null, true);
                return muaLoad(fileName.toString());
            }
            case "erall": {
                // TODO f: erall for context
                globalSymbolList.clear();
                if (localSymbolList != null)
                   localSymbolList.clear();
                return new MuaBool(true);
            }
            case "eq":
            case "gt":
            case "lt": {
                MuaType opr1 = muaExpression(null, true);
                MuaType opr2 = muaExpression(null, true);
                if (muaIsnumber(opr1.toString()) && muaIsnumber(opr2.toString()))
                    return new MuaBool(muaCompare(first, Double.parseDouble(opr1.toString()),
                            Double.parseDouble(opr2.toString())));
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
                return new MuaBool(muaIsnumber(opr.toString()));
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
                return new MuaBool(muaParseBool(opr) != null);
            }
            case "isempty": {
                MuaType val = muaExpression(null, true);
                if (val instanceof MuaList)
                    return new MuaBool(((MuaList) val).getListType() == MUA_EMPTY_LIST);
                if (val instanceof MuaWord)
                    return new MuaBool(((MuaWord) val).getValue().isEmpty());
                return errorAndExit("Invalid operand for isempty.");
            }
            case "isname": {
                return new MuaBool(isExistingMuaName());
            }
            case "not": {
                MuaType opr = muaExpression(null, true);
                if (opr instanceof MuaBool)
                        return new MuaBool(!((MuaBool) opr).getValue());
                else {
                    return errorAndExit("Invalid operands for " + first + ".");
                }
            }
            case "sentence": { // if an operand is a list, open it up
                StringBuilder listContent = new StringBuilder("[");
                for (int i = 0; i < 2; i++) {
                    MuaType value = muaExpression(null, true);
                    if (value instanceof MuaList)
                        listContent.append(((MuaList) value).toStringForPrint());
                    else
                        listContent.append(value.toString());
                    if (i == 0)
                        listContent.append(" ");
                }
                listContent.append("]");
                return new MuaList(listContent.toString());
            }
            case "list": { // if an operand is a list, do not open it up
                StringBuilder listContent = new StringBuilder("[");
                for (int i = 0; i < 2; i++) {
                    MuaType value = muaExpression(null, true);
                    listContent.append(value.toString());
                    if (i == 0)
                        listContent.append(" ");
                }
                listContent.append("]");
                return new MuaList(listContent.toString());
            }
            case "join": {
                MuaType list  = muaExpression(null, true),
                        value = muaExpression(null, true);
                if (!(list instanceof MuaList))
                    return errorAndExit("Operand 1 is not a list.");
                if (((MuaList) list).getListType() != MUA_EMPTY_LIST)
                    return new MuaList("[" + ((MuaList) list).toStringForPrint() +
                        " " + value.toString() + "]");
                else
                    return new MuaList("[" + value.toString() + "]");
            }
            case "butfirst":{
                MuaType operand = muaExpression(null, true);
                return muaFirst(operand, true);
            }
            case "first": {
                MuaType operand = muaExpression(null, true);
                return muaFirst(operand, false);
            }
            case "butlast":{
                MuaType operand = muaExpression(null, true);
                return muaLast(operand, true);
            }
            case "last": {
                MuaType operand = muaExpression(null, true);
                return muaLast(operand, false);
            }
            case "readlist": {
                return muaReadList();
            }
            case "word": {
                MuaType firstWord = muaExpression(null, true),
                        secondWord = muaExpression(null, true);
                return new MuaWord(firstWord.toString() + secondWord.toString());
            }
            case "save": {
                MuaType fileName = muaExpression(null, true);
                muaSave(fileName.toString());
                return fileName;
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
                    MuaList val = new MuaList(list);
                    if (val.getListType() == MuaList.ListType.MUA_FUNC && localSymbolList != null) {
                        muaSaveContext(val);
                    }
                    return val;
                }
                else if (firstCh == '"') {    // <word>
                    return new MuaWord(first.substring(1));
                }
                else if (Character.isDigit(firstCh) || firstCh == '-') {    // <number>
                    try {
                        double number = Double.parseDouble(first);
                        return new MuaNumber(number);
                    } catch (Exception e) {
                        return errorAndExit("Invalid input: " + first + ".");
                    }
                }
                else if (Character.isLetter(firstCh) || firstCh == '_') {   // <name>
                    if (isNameFuncCall) {
                        MuaType func = muaGetSymbol(first);
                        if (!(func instanceof MuaList) || ((MuaList) func).getListType() != MuaList.ListType.MUA_FUNC)
                            return errorAndExit(first + " = " + (func == null ? null : func.toString()) + " doesn't name a function.");
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
    }

    /**
     * make <name> <value> (with "make" read)
     * @return <value>
     */
    //@NotNull
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

        //System.out.println("Add context to " + func.toString());
        SymbolList symbolList = new SymbolList();
        for (String name : contextList) {
            MuaType val = muaGetSymbol(name);
            if (val != null) {
                symbolList.put(name, val.makeCopy());
            }
            //System.out.println(name + " <=> " + (val == null ? null : val.toString()));
        }

        func.setContext(symbolList);
    }

    //@NotNull
    private static MuaType muaReadList() {
        // TODO f: read in run
        String content;
        if (scanner.hasNext())
            content = scanner.nextLine();
        else
            return errorAndExit("Invalid input.");
        return new MuaList("[" + content + "]");
    }

    /**
     *
     */
    //@NotNull
    private static MuaType muaThing(String first) {
        MuaType name = muaExpression(first, false);
        MuaType result = muaGetSymbol(name.toString());
        if (result == null) {
            return errorAndExit("Name " + name.toString() + " not found.");
        }
        return result;
    }

    //@NotNull
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
            errorAndExit("Operands of " + op + "(" +  opr1.toString() + ", " + opr2.toString() + ") are not numbers.");
            return new MuaNumber(Double.NaN);
        }
    }

    //@NotNull
    private static MuaType muaErase() {
        MuaType name = muaExpression(null, true);
        MuaType result = muaRemoveSymbol(name.toString());
        if (result == null) {
            return errorAndExit("Name " + name.toString() + " not found.");
        }
        return result;
    }

    /**
     * @return The number of char "ch" in String "str"
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

    private static Boolean isExistingMuaName() {
        String name = muaExpression(null, true).toString();
        return muaGetSymbol(name) != null;
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

    private static MuaType muaGetSymbol(String name) {
        MuaType result = null;
        if (localSymbolList != null)
            result = localSymbolList.find(name);
        if (result == null && !contextSymbolListStack.empty()) {
            for (SymbolList contextSymbolList : contextSymbolListStack) {
                MuaType tempResult = contextSymbolList.find(name);
                if (tempResult != null) result = tempResult;
            }
        }
        if (result == null)
            result = globalSymbolList.find(name);
        return result;
    }

    private static MuaType muaRemoveSymbol(String name) {
        // TODO f: remove symbol for context
        MuaType result = null;
        if (localSymbolList != null)
            result = localSymbolList.remove(name);
        if (result == null)
            result = globalSymbolList.remove(name);
        return result;
    }

    private static MuaType muaRun(String code) {
        Scanner oldScanner = scanner;
        scanner = new Scanner(code);

        MuaType result;
        result = muaProgram();

        scanner = oldScanner;
        return result == null ? new MuaVoid() : result;
    }

    private static MuaType muaLoad(String fileName) {
        File file = new File(fileName);
        if (!file.canRead())
            return errorAndExit("Open failed!");

        Scanner oldScanner = scanner;

        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            return errorAndExit(e.getMessage());
        }

        MuaType result;
        result = muaProgram();

        scanner = oldScanner;
        return result == null ? new MuaVoid() : result;
    }

    private static void muaSave(String fileName) {
        // TODO f: save for context
        try {
            PrintWriter os = new PrintWriter(fileName);
            os.print(globalSymbolList.toString());
            if (localSymbolList != null)
                os.print(localSymbolList.toString());
            os.close();
        } catch (IOException e) {
            errorAndExit(e.getMessage());
        }
    }

    private static MuaType muaFirst(MuaType operand, boolean isBut) {
        if (operand instanceof MuaList) {
            Pattern pattern = Pattern.compile("\\s*(\\[.*])\\s*(.*)");
            Matcher matcher = pattern.matcher(((MuaList) operand).toStringForPrint());
            if (matcher.matches()) {
                if (isBut)
                    return new MuaList("[" + matcher.group(2) + "]");
                else
                    return new MuaList(matcher.group(1));
            }
            pattern = Pattern.compile("\\s*(\\S*)\\s*(.*)");
            matcher = pattern.matcher(((MuaList) operand).toStringForPrint());
            if (matcher.matches()) {
                if (isBut)
                    return new MuaList("[" + matcher.group(2) + "]");
                else
                    return new MuaWord(matcher.group(1));
            }
            return errorAndExit("Get first failed.");
        }
        else {
            if (isBut)
                return new MuaWord(operand.toString().substring(1));
            else
                return new MuaWord(operand.toString().substring(0, 1));
        }
    }

    private static MuaType muaLast(MuaType operand, boolean isBut) {
        if (operand instanceof MuaList) {
            Pattern pattern = Pattern.compile("\\s*(.*)(\\[.*])");
            Matcher matcher = pattern.matcher(((MuaList) operand).toStringForPrint());
            if (matcher.matches()) {
                if (isBut)
                    return new MuaList("[" + matcher.group(1) + "]");
                else
                    return new MuaList(matcher.group(2));
            }
            String[] items = ((MuaList) operand).toStringForPrint().split(" ");
            if (isBut) {
                StringBuilder contents = new StringBuilder();
                for (int i = 0; i < items.length - 1; i++) {
                    contents.append(items[i]).append(" ");
                }
                contents.deleteCharAt(contents.length() - 1);
                return new MuaList("[" + contents + "]");
            }
            else
                return new MuaWord(items[items.length - 1]);
        }
        else {
            int length = operand.toString().length();
            if (isBut)
                return new MuaWord(operand.toString().substring(0, length - 1));
            else
                return new MuaWord(operand.toString().substring(length - 1, length));
        }
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

    private static boolean muaIsnumber(String str) {
        try {
            Double.parseDouble(str);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    //@NotNull
    private static String getMuaListContent(MuaList val) {
        Pattern pattern = Pattern.compile("\\s*\\[(.*)]\\s*");
        Matcher matcher = pattern.matcher(val.toString());
        if(!matcher.matches())
            errorAndExit("Failed to get the content of list " + val.toString());
        return matcher.group(1);
    }

    //@NotNull
    private static MuaType muaCallFunc(MuaList func) {
        SymbolList oldLocalTbl = localSymbolList;
        SymbolList newLocalTbl = new SymbolList();
        for (String name : func.getParamList()) {
            if (name.isEmpty() || name.isBlank())
                continue;
            MuaType val = muaExpression(null, true);
            newLocalTbl.put(name, val);
            //muaAddSymbol(name, val, symbolTbl.MUA_LOCAL);
        }
        localSymbolList = newLocalTbl;

        if (func.getContext() != null)
            contextSymbolListStack.push(func.getContext());

        MuaType result = muaRun(func.getFunc_code());

        /*if (result instanceof MuaList
                && ((MuaList) result).getListType() == MuaList.ListType.MUA_FUNC
                && ((MuaList) result).getParamList().length > 0)
            muaCallFunc((MuaList) result);*/

        if (func.getContext() != null)
            contextSymbolListStack.pop();
        localSymbolList = oldLocalTbl;

        return result;
    }

    private enum symbolTbl {MUA_LOCAL, MUA_GLOBAL}
}
