package mua;

import mua.types.*;

import java.util.Scanner;
import java.util.regex.Pattern;

public class Main {
    private static Scanner scanner;
    private static SymbolList symbolList;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);
        symbolList = new SymbolList();
        muaProgram();
    }

    public static void errorAndExit(String msg) {
        System.out.println(msg);
        System.out.println("Program terminated.");
        System.exit(1);
    }

    public static Boolean isMuaName(MuaType obj) {
        return (obj instanceof MuaWord
             && Pattern.matches("[a-zA-Z][a-zA-Z0-9_]*", obj.toString()));
    }

    /*
     * Get the whole MUA program.
     */
    private static void muaProgram() {
        while (muaSentence());
    }

    /*
     * Get 1 sentence.
     * @return FALSE if there is no more input.
     */
    private static Boolean muaSentence() {
        if (!scanner.hasNext())
            return false;
        // TODO 1: make_func & return_stmt
        muaExpression(null, true);
        return true;
    }

    /*
     * Get 1 expression.
     * @param 1. The first string of this expression if read by the caller.
     * @param 2. Whether a string w/o quotation mark is regarded as a func call or a name
     * @return The return value of the expression.
     */
    private static MuaType muaExpression(String first, Boolean isNameFuncCall) {
        if (first == null) {
            first = scanner.next();
        }
        switch (first) {
            case "make": {
                return muaMake();
            }
            case "thing": {
//                String name = muaName();
//                return muaThing(name);
                return muaThing(null);
            }
            case "print": {
                MuaType val = muaExpression(null, true);
                if (val != null)
                    System.out.println(val.toString());
                else
                    errorAndExit("The expression to print returns null.");
                return val;
            }
            case "erase": {
                break;
            }
            case "run": {
                break;
            }
            case "if": {
                break;
            }
            case "export": {
                break;
            }
            case "add": case "sub": case "mul":
            case "div": case "mod": {
                return muaBinaryOperation(first);
            }
            case "random": {
                break;
            }
            case "int": {
                break;
            }
            case "sqrt": {
                break;
            }
            case "read": {
                String str = scanner.next();
                if (Pattern.matches("-?[0-9]+\\.?[0-9]*", str))
                    return new MuaNumber(Double.parseDouble(str));
                return new MuaWord(str);
            }
            case "load": {
                break;
            }
            case "erall": {
                break;
            }
            case "eq": case "gt": case "lt": {
                break;
            }
            case "and": case "or": {
                break;
            }
            case "isnumber": case "isword":
            case "islist": case "isbool": {
                break;
            }
            case "isempty": {
                break;
            }
            case "isname": {
                break;
            }
            case "not": {
                break;
            }
            case "sentence": {
                break;
            }
            case "list": {
                break;
            }
            case "join": {
                break;
            }
            case "butfirst": case "butlast":
            case "first": case "last": {
                break;
            }
            case "readlist": {
                break;
            }
            case "poall": {
                break;
            }
            case "word": {
                break;
            }
            case "save": {
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
                } else if (firstCh == '[') {    // <list>
                    String list = first;
                    while (countCharInString('[', list) != countCharInString(']', list)) {
                        if (!scanner.hasNext())
                            errorAndExit("Wrong list format.");
                        list = list + ' ' + scanner.next();
                    }
                    return new MuaList(list);
                } else if (firstCh == '"') {    // <word>
                    return new MuaWord(first.substring(1));
                } else if (Character.isDigit(firstCh)) {    // <number>
                    try {
                        double number = Double.parseDouble(first);
                        return new MuaNumber(number);
                    } catch (Exception e) {
                        errorAndExit("Invalid input: " + first + ".");
                    }
                } else if (Character.isLetter(firstCh)) {   // <name>
                    if (isNameFuncCall) {
                        // TODO 4: Function Call
                        return null;
                    } else {
//                        System.out.println("name: " + first);
                        return new MuaWord(first);
                    }
                } else {
                    errorAndExit("Unknown input " + first);
                }
            }
        }
        // TODO 5: After finishing all cases, delete the following:
        errorAndExit("Not implemented yet.");
        return null;
    }

    /*
     * make <name> <value> (with "make" read)
     * @return <value>
     */
    private static MuaType muaMake() {
//        String name = muaName();
//        MuaType val = muaExpression(null);
//        symbolList.addToList(name, val);
//        return val;
        MuaType name = muaExpression(null, true);
        MuaType val = muaExpression(null, true);
        if (isMuaName(name))
            symbolList.addToList(name.toString(), val);
        else
            errorAndExit("[Error] Invalid <name> for operation make.");
        return val;
    }

//    /*
//     * Read a name with check.
//     * <name> = "<name_wo_quo>
//     */
//    private static String muaName() {
//        String name = scanner.next();
//        if (name.charAt(0) != '"')
//            errorAndExit("String " + name + " is not a name (no leading \").");
//        return muaNameWithoutQuotationMark(name.substring(1));
//    }
//
//    /*
//     * Read a name w/o quotation mark with check.
//     * <name_wo_quo> = [a-zA-Z][a-zA-Z0-9_]*
//     * @param Name to be checked if read by the caller.
//     */
//    private static String muaNameWithoutQuotationMark(String str) {
//        if (str == null)
//            str = scanner.next();
//        if (!Pattern.matches("[a-zA-Z][a-zA-Z0-9_]*", str))
//            errorAndExit("String " + str + " is not a valid name.");
//        return str;
//    }

    /*
     *
     */
    private static MuaType muaThing(String first) {
//        MuaType result = symbolList.find(name);
//        if (result == null)
//            errorAndExit("Name " + name + " not found.");
//        return result;
        MuaType name = muaExpression(first, false);
//        System.out.println("name: " + name.toString());
        if (isMuaName(name)) {
            MuaType result = symbolList.find(name.toString());
//            System.out.println("result: " + result.toString());
            if (result == null)
                errorAndExit("Name " + name.toString() + " not found.");
            return result;
        } else {
            errorAndExit("[Error] Invalid <name> for operation thing.");
        }
        return null;
    }

    /*
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

    private static MuaNumber muaBinaryOperation(String op) {
        MuaType opr1 = muaExpression(null, true), opr2 = muaExpression(null, true);
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
                    } catch(ArithmeticException e) {
                        errorAndExit("Devide by 0.");
                    }
                }
                case "mod": {
                    return new MuaNumber(Math.round(((MuaNumber) opr1).getValue())
                                        % Math.round(((MuaNumber) opr2).getValue()));
                }
                default: {
                    errorAndExit("Invalid operation: " + op + ".");
                }
            }
        } else {
            errorAndExit("Operands of " + op + " are not numbers.");
        }
        return null;
    }
}
