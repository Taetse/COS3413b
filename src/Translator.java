import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Translator {
    private AbstractTree tree;
    private SemanticTable semanticTable;
    private String intermediateCode;
    private String finalIntermediateCode;
//    private int varCount = 0;
    private int varChars[] = new int[]{0,0};
    private int labelCount = 0;
    private String endLabel;
    private HashMap<String, String> variableMap = new HashMap<>();

    public Translator(AbstractTree tree, SemanticTable semanticTable) {
        this.tree = tree;
        this.semanticTable = semanticTable;

        endLabel = newLabel();
        intermediateCode = translateStatement(tree.root);
        intermediateCode += endLabel + "\r\nEND\r\n";
        finalIntermediateCode = sequentializeIntermediateCode(splitIntermediateCode());
    }

    private String sequentializeIntermediateCode(String[] lines) {
        HashMap<String, Integer> labelMap = new HashMap<>();
        String sequentializedIntermediateCode = "";
        Pattern pattern = Pattern.compile("^%P?[0-9]+"); //tag regex

        int index = 0;
        for (String line : lines) {
            if (line.length() == 0)
                continue;
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) { //line is a label
                labelMap.put(matcher.group(), index);
            } else {
                index++;
            }
        }

        index = 0;
        pattern = Pattern.compile("%P?[0-9]+"); //tag regex
        for (String line : lines) {
            if (line.length() == 0 || line.charAt(0) == '%') //is label line
                continue;
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String label = matcher.group();
                String lineIndex = labelMap.get(label).toString();
                line = line.replace(label, lineIndex);
            }
            sequentializedIntermediateCode += index++ + " " + line + "\r\n";
        }

        return sequentializedIntermediateCode;
    }

    private String[] splitIntermediateCode() {
        return intermediateCode.split("\r\n");
    }

    public String getIntermediateCode() {
        return intermediateCode;
    }

    public String getFinalIntermediateCode() {
        return finalIntermediateCode;
    }

    private String translateStatement(AbstractNode abstractNode) {
        AbstractNodeType nodeType = abstractNode.type;
        String intermediateCode = "";

        switch (nodeType) {
            case StrDecl:
            case NumDecl:
            case BoolDecl:
                break;
            case Halt:
                intermediateCode = "GOTO " + endLabel + "\r\n";
                break;
            case Call:
                String functionLabel = newFunctionLabel(abstractNode.val);
                intermediateCode = "GOSUB " + functionLabel + "\r\n";
                break;
            case Proc:
                functionLabel = newFunctionLabel(abstractNode.val);
                String functionCode = translateStatement(abstractNode.children[0]);
                intermediateCode = functionLabel + "\r\n" + functionCode + "RETURN" + "\r\n";
                break;
            case CondBranch:
                String label1 = newLabel();
                String label2 = newLabel();
                String label3 = newLabel();
                String code1 = translateBoolean(abstractNode.children[0], label1, label2);
                String code2 = translateStatement(abstractNode.children[1]);
                if (abstractNode.children.length > 2) { //there is an else statement
                    String code3 = translateStatement(abstractNode.children[2]);
                    intermediateCode = code1 + label1 + "\r\n" + code2 + "GOTO " + label3 + "\r\n" + label2 + "\r\n" + code3 + label3 + "\r\n";
                } else {
                    intermediateCode = code1 + label1 + "\r\n" + code2 + label2 + "\r\n";
                }
                break;
            case WhileLoop:
                label1 = newLabel();
                label2 = newLabel();
                label3 = newLabel();
                code1 = translateBoolean(abstractNode.children[0], label2, label3);
                code2 = translateStatement(abstractNode.children[1]);
                intermediateCode = label1 + "\r\n" + code1 + label2 + "\r\n" + code2 + "GOTO " + label1 + "\r\n" + label3 + "\r\n";
                break;
            case ForLoop:
                label1 = newLabel();
                label2 = newLabel();
                label3 = newLabel();
                String code0 = translateStatement(abstractNode.children[0]); //the assignment
                code1 = translateBoolean(abstractNode.children[1], label2, label3); //the condition
                code2 = translateStatement(abstractNode.children[3]); //the body
                String code3 = translateStatement(abstractNode.children[2]); //the increment
                intermediateCode = code0 + label1 + "\r\n" + code1 + label2 + "\r\n" + code2 + code3 + "GOTO " + label1 + "\r\n" + label3 + "\r\n";
                break;
            case Assign:
                String place;
                if (semanticTable.table[abstractNode.id].nameType == NameType.S)
                    place = translateStringVar(abstractNode.children[0].val);
                else
                    place = translateNumVar(abstractNode.children[0].val);
                intermediateCode = translateExpression(abstractNode.children[1], place);
                break;
            case Output:
            case Input:
                if (semanticTable.table[abstractNode.id].nameType == NameType.S)
                    place = translateStringVar(abstractNode.children[0].val);
                else
                    place = translateNumVar(abstractNode.children[0].val);
                intermediateCode = translateExpression(abstractNode, place);
                break;
            case ProcDefs:
                String defsLabel = newLabel();
                intermediateCode = "GOTO " + defsLabel + "\r\n";
                for (AbstractNode childAbstractNode : abstractNode.children)
                    intermediateCode += translateStatement(childAbstractNode);
                intermediateCode += defsLabel + "\r\n";
                break;
            default:
                for (AbstractNode childAbstractNode : abstractNode.children)
                    intermediateCode += translateStatement(childAbstractNode);
                break;
        }
        return intermediateCode;
    }

    private String translateExpression(AbstractNode abstractNode, String place) {
        AbstractNodeType nodeType = abstractNode.type;
        String intermediateCode = "";

        switch (nodeType) {
            case True:
                intermediateCode = "LET " + place + " = 1";
                break;
            case False:
                intermediateCode = "LET " + place + " = 0";
                break;
            case Var:
                intermediateCode = "LET " + place + " = " +
                        (semanticTable.table[abstractNode.id].nameType == NameType.S?
                                translateStringVar(abstractNode.val) : translateNumVar(abstractNode.val));
                break;
            case String:
            case Number:
                intermediateCode = "LET " + place + " = " + abstractNode.val;
                break;
            case Input:
                intermediateCode = "INPUT " + place;
                break;
            case Output:
                intermediateCode = "PRINT " + place;
                break;
            case NotExpr:
            case AndExpr:
            case OrExpr:
            case EqExpr:
                String label1 = newLabel();
                String label2 = newLabel();
                String code1 = translateBoolean(abstractNode, label1, label2);
                intermediateCode = "LET " + place + " = 0\r\n" + code1 + label1 + " LET " + place + " = 1 " + label2;
                break;
            case GreaterExpr:
            case LessExpr:
            case AddExpr:
            case SubExpr:
            case MultExpr:
                String place1 = newNumVar();
                String place2 = newNumVar();
                code1 = translateExpression(abstractNode.children[0], place1);
                String code2 = translateExpression(abstractNode.children[1], place2);
                String op = translateOp(abstractNode);
                intermediateCode = code1 + code2 + "LET " + place + " = " + place1 + " " + op + " " + place2;
                break;
        }

        return intermediateCode + "\r\n";
    }

    private String translateBoolean(AbstractNode abstractNode, String labelTrue, String labelFalse) {
        AbstractNodeType nodeType = abstractNode.type;
        String intermediateCode = "";

        switch (nodeType) {
            case Var:
                String place = newNumVar();
                String code = translateExpression(abstractNode, place);
                intermediateCode = code + "IF " + place + " THEN GOTO " + labelTrue + "\r\nGOTO " + labelFalse + "\r\n";
                break;
            case True:
                intermediateCode = "GOTO " + labelTrue + "\r\n";
                break;
            case False:
                intermediateCode = "GOTO " + labelFalse + "\r\n";
                break;
            case EqExpr:
            case GreaterExpr:
            case LessExpr:
                String place1 = newNumVar();
                String place2 = newNumVar();
                String code1 = translateExpression(abstractNode.children[0], place1);
                String code2 = translateExpression(abstractNode.children[1], place2);
                String op = translateOp(abstractNode);
                intermediateCode = code1 + code2 + "IF " + place1 + " " + op + " " + place2 + " THEN GOTO " + labelTrue + "\r\nGOTO " + labelFalse + "\r\n";
                break;
            case OrExpr:
                String arg2 = newLabel();
                code1 = translateBoolean(abstractNode.children[0], labelTrue, arg2);
                code2 = translateBoolean(abstractNode.children[1], labelTrue, labelFalse);
                intermediateCode = code1 + arg2 + "\r\n" + code2;
                break;
            case AndExpr:
                arg2 = newLabel();
                code1 = translateBoolean(abstractNode.children[0], arg2, labelFalse);
                code2 = translateBoolean(abstractNode.children[1], labelTrue, labelFalse);
                intermediateCode = code1 + arg2 + "\r\n" + code2;
                break;
            case NotExpr:
                intermediateCode = translateBoolean(abstractNode.children[0], labelFalse, labelTrue);
                break;
        }
        return intermediateCode;
    }

    private String newNumVar() {
        char char1 = (char)(varChars[0] + 'A');
        char char2 = (char)(varChars[1] + 'A');
        String varName = String.valueOf(char1) + String.valueOf(char2);

        varChars[1]++;
        if (varChars[1] >= 26) {
            varChars[0]++;
            varChars[1] = 0;
        }
        return varName;
    }

    private String newStringVar() {
        return newNumVar() + "$";
    }

    private String translateNumVar(String var) {
        if (variableMap.get(var) == null)
            variableMap.put(var, newNumVar());
        return variableMap.get(var);
    }

    private String translateStringVar(String var) {
        if (variableMap.get(var) == null)
            variableMap.put(var, newStringVar());
        return variableMap.get(var);
    }

    private String newLabel() {
        return "%" + labelCount++;
    }

    private String newFunctionLabel(String functionName) {
        return "%" + functionName;
    }

    private String translateOp(AbstractNode abstractNode) {
        AbstractNodeType nodeType = abstractNode.type;
        String binOpName = "";
        switch (nodeType) {
            case EqExpr:
                binOpName = "=";
                break;
            case LessExpr:
                binOpName = "<";
                break;
            case GreaterExpr:
                binOpName = ">";
                break;
            case NotExpr:
                binOpName = "NOT";
                break;
            case AndExpr:
                binOpName = "AND";
                break;
            case OrExpr:
                binOpName = "OR";
                break;
            case AddExpr:
                binOpName = "+";
                break;
            case SubExpr:
                binOpName = "-";
                break;
            case MultExpr:
                binOpName = "*";
                break;
            case Assign:
                binOpName = "=";
                break;
        }
        return binOpName;
    }
}
