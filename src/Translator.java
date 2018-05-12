import java.util.ArrayList;

public class Translator {
    private AbstractTree tree;
//    private ArrayList<String> intermediateCode = new ArrayList<>();
    private String intermediateCode = "";
    private int varCount = 0;
    private int labelCount = 0;

    public Translator(AbstractTree tree) {
        this.tree = tree;

        intermediateCode = translateStatement(tree.root);
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
            case CondBranch:
                String label1 = newLabel();
                String label2 = newLabel();
                String label3 = newLabel();
                String code1 = translateBoolean(abstractNode.children[0], label1, label2);
                String code2 = translateStatement(abstractNode.children[1]);
                if (abstractNode.children.length > 2) { //there is an else statement
                    String code3 = translateStatement(abstractNode.children[2]);
                    intermediateCode = code1 + "LABEL " + label1 + "\r\n" + code2 + "GOTO " + label3 + "\r\nLABEL " + label2 + "\r\n" + code3 + "LABEL " + label3;
                } else {
                    intermediateCode = code1 + "LABEL " + label1 + "\r\n" + code2 + "LABEL " + label2;
                }
                break;
            case Assign:
                String place = abstractNode.children[0].val;
                intermediateCode = translateExpression(abstractNode.children[1], place);
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
            case EqExpr:
            case GreaterExpr:
            case LessExpr:
            case AddExpr:
            case SubExpr:
            case MultExpr:
                String place1 = newNumVar();
                String place2 = newNumVar();
                String code1 = translateExpression(abstractNode.children[0], place1);
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
            case True:
            case False:
            case Var:
                String place = newNumVar();
                String code = translateExpression(abstractNode, place);
                intermediateCode = code + "IF " + place + " THEN GOTO " + labelTrue + "\r\nGOTO " + labelFalse;
                break;
            case EqExpr:
            case GreaterExpr:
            case LessExpr:
            case AddExpr:
            case OrExpr:
                String place1 = newNumVar();
                String place2 = newNumVar();
                String code1 = translateExpression(abstractNode.children[0], place1);
                String code2 = translateExpression(abstractNode.children[1], place2);
                String op = translateOp(abstractNode);
                intermediateCode = code1 + code2 + "IF " + place1 + " " + op + " " + place2 + " THEN GOTO " + labelTrue + "\r\nGOTO " + labelFalse;
        }
        return intermediateCode + "\r\n";
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
