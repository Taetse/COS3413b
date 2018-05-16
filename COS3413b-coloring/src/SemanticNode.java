import jdk.nashorn.internal.runtime.regexp.joni.constants.NodeType;

import javax.naming.Name;

public class SemanticNode {
    int nodeId;

    public SemanticNode(int nodeId, AbstractNodeType nodeType) {
        this.nodeType = nodeType;
        this.nodeId = nodeId;
    }

    @Override
    public String toString() {
	    
		    
//        String representation = nodeId + " " + scopeId + "::" + nodeType + " " + snippet + " " + (usageSource == null? "" : "Declared in " + usageSource);
        String representation = scopeId + "::" + nodeType + " " + (snippet.equals("")? "" : snippet + " ") + (usageSource == null? "" : "From " + usageSource + ": ");
        if (errorMessage != null)
            representation += " [ERROR] " + errorMessage + " ";
        if (nameType != NameType.C) {
            switch (nameType) {
                case E:
                    representation += " [ERROR] Type mismatch!";
                    break;
                case N:
                    representation += "Number";
                    break;
                case S:
                    representation += "String";
                    break;
                case B:
                    representation += "Boolean";
                    break;
                case P:
                    representation += "Procedure";
                    break;
            }
        }
	
	if (!reachable) 
		    return representation+ " [UNREACHABLE]";

        switch (nodeType) {
            case Var:
            case True:
            case False:
            case Number:
            case String:
                representation += (flowStart == null? " [ERROR] No Value!" : "(Has Value)");
                break;
            case BoolDecl:
            case StrDecl:
            case NumDecl:
                representation += "(No Value)";
        }
        return representation;
    }

    boolean reachable = false;
    int scopeId;
    AbstractNodeType nodeType;
    Integer usageSource = null;
    String snippet = "";
    NameType nameType = NameType.E;
    String errorMessage = null;
    Integer flowStart = null;
}
