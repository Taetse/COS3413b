import java.util.ArrayList;

public class Translator {
    private AbstractTree tree;
//    private ArrayList<String> intermediateCode = new ArrayList<>();
    private String intermediateCode = "";
    private int varCount = 0;
    private int labelCount = 0;
    private String endLabel;

    public Translator(AbstractTree tree) {
        this.tree = tree;

        endLabel = newLabel();
        intermediateCode = translateStatement(tree.root);
        intermediateCode += "\r\n" + endLabel + "\r\nEND\r\n";
    }

    public String getIntermediateCode() {
        return intermediateCode;
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
                intermediateCode = functionLabel + "\r\n" + functionCode + "RETURN";
                break;
            case CondBranch:
                String label1 = newLabel();
                String label2 = newLabel();
                String label3 = newLabel();
                String code1 = translateBoolean(abstractNode.children[0], label1, label2);
                String code2 = translateStatement(abstractNode.children[1]);
                if (abstractNode.children.length > 2) { //there is an else statement
                    String code3 = translateStatement(abstractNode.children[2]);
                    intermediateCode = code1 + label1 + "\r\n" + code2 + "GOTO " + label3 + "\r\n" + label2 + "\r\n" + code3 + label3;
                } else {
                    intermediateCode = code1 + label1 + "\r\n" + code2 + label2;
                }
                break;
            case WhileLoop:
                label1 = newLabel();
                label2 = newLabel();
                label3 = newLabel();
                code1 = translateBoolean(abstractNode.children[0], label2, label3);
                code2 = translateStatement(abstractNode.children[1]);
                intermediateCode = label1 + "\r\n" + code1 + label2 + "\r\n" + code2 + "GOTO " + label1 + "\r\n" + label3;
                break;
            case ForLoop:
                label1 = newLabel();
                label2 = newLabel();
                label3 = newLabel();
                String code0 = translateStatement(abstractNode.children[0]); //the assignment
                code1 = translateBoolean(abstractNode.children[1], label2, label3); //the condition
                code2 = translateStatement(abstractNode.children[3]); //the body
                String code3 = translateStatement(abstractNode.children[2]); //the increment
                intermediateCode = code0 + label1 + "\r\n" + code1 + label2 + "\r\n" + code2 + code3 + "GOTO " + label1 + "\r\n" + label3;
                break;
            case Assign:
                String place = abstractNode.children[0].val;
                intermediateCode = translateExpression(abstractNode.children[1], place);
                break;
            case Output:
            case Input:
                place = abstractNode.children[0].val;
                intermediateCode = translateExpression(abstractNode, place);
                break;
            default:
                for (AbstractNode childAbstractNode : abstractNode.children) {
                    intermediateCode += translateStatement(childAbstractNode);
                }
                break;
        }
        return intermediateCode + "";
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
        return "VAR" + varCount++;
    }

    private String newStringVar() {
        return "VAR" + varCount++ + "$";
    }

    private String newLabel() {
        return "LABEL" + labelCount++;
    }

    private String newFunctionLabel(String functionName) {
        return "LABEL" + functionName;
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
