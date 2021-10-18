import types.MuaType;
import java.util.Scanner;

public class Main {
    private static Scanner scanner;
    private static SymbolList symbolList;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);
        symbolList = new SymbolList();
        muaProgram();
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
        muaExpression(null);
        return true;
    }

    /*
     * Get 1 expression.
     * @param The first string of this expression if read by the caller.
     * @return The return value of the expression.
     */
    private static MuaType muaExpression(String first) {
        if (first == null) {
            first = scanner.next();
        }
        switch (first) {
            case "make": {
                return muaMake();
            }
            case "thing": {

            }
            case "print": {

            }
            case "erase": {

            }
            case "run": {

            }
            case "if": {

            }
            case "export": {

            }
            case "add": case "sub": case "mul":
            case "div": case "mod": {

            }
            case "random": {

            }
            case "int": {

            }
            case "sqrt": {

            }
            case "read": {

            }
            case "load": {

            }
            case "erall": {

            }
            case "eq": case "gt": case "lt": {

            }
            case "and": case "or": {

            }
            case "isnumber": case "isword":
            case "islist": case "isbool": {

            }
            case "isempty": {

            }
            case "isname": {

            }
            case "not": {

            }
            case "sentence": {

            }
            case "list": {

            }
            case "join": {

            }
            case "butfirst": case "butlast"
            case "first": case "last": {

            }
            case "readlist": {

            }
            case "poall": {

            }
            case "word": {

            }
            case "save": {

            }
            default: {
                /*
                 * : OR <name> (func call) OR <list>
                 * OR <number> OR <bool> OR <word>
                 */
                if (first.charAt(0) == ':') {

                }
            }
        }
    }

    /*
     * make <name> <value> (with "make" read)
     * @return <value>
     */
    private static MuaType muaMake() {
        String name = muaName();
        MuaType val = muaExpression(null);
        symbolList.addToList(name, val);
        return val;
    }

    /*
     * <name> = "<letter>  (<letter> | <digit> | _)*
     */
    private static String muaName() {

    }
}
