import jdk.nashorn.internal.runtime.regexp.joni.constants.NodeType;

import java.util.*;

public class SemanticTable {
    public SemanticNode table[];
    private ArrayList<HashMap<String, Integer>> symbolTable = new ArrayList<>();
    private ArrayList<HashMap<String, Integer>> flowStartTable = new ArrayList<>();
    private AbstractTree tree;

    private int scopeCount = 0;
    private int variableCount = 0;
    private int processCount = 0;
    private int symbolBindOffset = 0;

    public SemanticTable(AbstractTree tree) {
        table = new SemanticNode[tree.nodes.length];
        this.tree = tree;
        populateTable(tree.root, 0); //populate semantic table
        reName(tree.root); //rename all variables
        pushFlowStack();
        establishFlow(tree.root, null); //check variable flow
        popFlowStack();
    }

    private void populateTable(AbstractNode abstractNode, int scopeId) {
        if (abstractNode == null)
            return;

        AbstractNodeType nodeType = abstractNode.type;
        SemanticNode node;
        if (table[abstractNode.id] != null) {
            node = table[abstractNode.id];
            scopeId = node.scopeId;
        } else {
            node = new SemanticNode(abstractNode.id, nodeType);
            node.scopeId = scopeId;
            node.snippet = abstractNode.val;
        }
        table[abstractNode.id] = node;
        switch (nodeType) {
            case Call:
                //Access of name
                Integer declarationSource = getDeclarationSource(abstractNode.val);
                node.usageSource = declarationSource;
                break;
            case Var:
                //Access of name
                declarationSource = getDeclarationSource(abstractNode.val);
                node.usageSource = declarationSource;
                if (declarationSource != null)
                    node.nameType = table[declarationSource].nameType;
                break;
            case NumDecl:
            case BoolDecl:
            case StrDecl:
                if (reDeclaration(abstractNode.val))
                    table[abstractNode.id].errorMessage = "Redeclaration of Variable!";
                node.usageSource = abstractNode.id; //find declaration node of variable
                symbolTable.get(symbolTable.size() - 1 + symbolBindOffset).put(abstractNode.val, abstractNode.id); //insert declaration in symbol table
                break;
            case Prog:
                symbolTable.add(new HashMap<>()); //push first layer of symbol table
                if (abstractNode.children.length > 1) //if the prog has proc defs scan them first
                    populateTableProcedures(abstractNode.children[1], scopeId);
                for (AbstractNode childAbstractNode : abstractNode.children)
                    populateTable(childAbstractNode, scopeId); //recursively call on children
                symbolTable.remove(symbolTable.size() - 1); //pop layer of symbol table
                break;
            case ForLoop:
                node.nameType = NameType.N;
                symbolTable.add(new HashMap<>()); //push symbol table scope layer
                node.usageSource = abstractNode.id; //forloop is variable declaration
                node.snippet = abstractNode.children[0].children[0].val; //get name of variable
                symbolTable.get(symbolTable.size() - 1).put(abstractNode.children[0].children[0].val, abstractNode.id); //insert for loop control variable in its own scope
                symbolBindOffset = -1; //negative offset that new variables wont be declared in control variable scope layer
                for (AbstractNode childAbstractNode : abstractNode.children)
                    populateTable(childAbstractNode, scopeId); //recursive call with increased scope id


                symbolTable.remove(symbolTable.size() - 1); //pop symbol table scope layer
                symbolBindOffset = 0; //remove offset so new variables will be declared in top scope layer
                break;
            default:
                for (AbstractNode childAbstractNode : abstractNode.children)
                    populateTable(childAbstractNode, scopeId); //recursively call on children
                break;
        }

        establishType(abstractNode);
    }

    private void populateTableProcedures(AbstractNode abstractNode, int scopeId) {
        if (abstractNode == null)
            return;
        AbstractNodeType nodeType = abstractNode.type;
        SemanticNode node = new SemanticNode(abstractNode.id, nodeType);
        node.scopeId = scopeId;
        node.snippet = abstractNode.val;
        table[abstractNode.id] = node;
        switch (nodeType) {
            case ProcDefs:
                for(int a = abstractNode.children.length - 1; a >= 0; a--)
                    populateTableProcedures(abstractNode.children[a], ++scopeCount);
                break;
            case Proc:
                if (reDeclaration(abstractNode.val))
                    table[abstractNode.id].errorMessage = "Redeclaration of Procedure!";
                node.usageSource = abstractNode.id;
                symbolTable.get(symbolTable.size() - 1 + symbolBindOffset).put(abstractNode.val, abstractNode.id); //insert declaration in symbol table
        }
	
	establishType(abstractNode);
    }

    private boolean establishFlow(AbstractNode abstractNode, AbstractNode parent) {
	boolean halt = false;
        AbstractNodeType nodeType = abstractNode.type;
        SemanticNode node = table[abstractNode.id];
        switch (nodeType) {
            case Var:
                //Continue flow
                node.flowStart = getFlowStart(abstractNode.val);
                break;
            case Number:
            case String:
            case True:
            case False:
                //Start flow
                node.flowStart = abstractNode.id;
                break;
            case Prog:
//                pushFlowStack();
                if (abstractNode.children.length > 1) //if the prog has proc defs scan them first
                    establishFlow(abstractNode.children[1], abstractNode);
                for (AbstractNode childAbstractNode : abstractNode.children)
                    establishFlow(childAbstractNode, abstractNode); //recursively call on children
//                popFlowStack();
                break;
            case Code:
                if (table[parent.id].nodeType != AbstractNodeType.Prog) { //if the node is a local block scope
                    pushFlowStack();
                    for (AbstractNode childAbstractNode : abstractNode.children)
			            if (!halt)
				            halt = halt || establishFlow(childAbstractNode, abstractNode); //recursive call
                    popFlowStack();
                } else {
                    for (AbstractNode childAbstractNode : abstractNode.children)
                        if (!halt)
                            halt = halt || establishFlow(childAbstractNode, abstractNode); //recursive call
                }
                break;
            case Input:
                for (AbstractNode childAbstractNode : abstractNode.children)
                    establishFlow(childAbstractNode, abstractNode); //recursive call
                AbstractNode inputVar = abstractNode.children[0];
                node.flowStart = abstractNode.id;
                table[inputVar.id].flowStart = abstractNode.id;
                putFlowStart(inputVar.val, abstractNode.id);
                break;
            case Assign:
                for (AbstractNode childAbstractNode : abstractNode.children)
                    establishFlow(childAbstractNode, abstractNode); //recursively call on children

                AbstractNode leftOperand = abstractNode.children[0];
                AbstractNode rightOperand = abstractNode.children[1];
                AbstractNodeType rightOperandNodeType = table[rightOperand.id].nodeType;
                switch (rightOperandNodeType) { //inspect rhs operand
                    case String:
                    case Number:
                    case True:
                    case False:
                    case Var:
                    case EqExpr:
                    case GreaterExpr:
                    case LessExpr:
                    case AddExpr:
                    case SubExpr:
                    case MultExpr:
                    case AndExpr:
                    case OrExpr:
                        node.flowStart = table[rightOperand.id].flowStart;
                        table[leftOperand.id].flowStart = table[rightOperand.id].flowStart;
                        if (table[rightOperand.id].flowStart != null)  //if rhs operand has value
                            putFlowStart(leftOperand.val, rightOperand.id);
                        break;
                }
                break;
            case EqExpr:
            case GreaterExpr:
            case LessExpr:
            case AddExpr:
            case SubExpr:
            case MultExpr:
            case AndExpr:
            case OrExpr:
                for (AbstractNode childAbstractNode : abstractNode.children)
                    establishFlow(childAbstractNode, abstractNode); //recursively call on children

                leftOperand = abstractNode.children[0];
                rightOperand = abstractNode.children[1];
                if (table[leftOperand.id].flowStart != null && table[rightOperand.id].flowStart != null)
                    node.flowStart = abstractNode.id;
                break;
            case Output:
            case NotExpr:
            case CondBranch:
            case WhileLoop:
                for (AbstractNode childAbstractNode : abstractNode.children)
                    establishFlow(childAbstractNode, abstractNode); //recursively call on children

                AbstractNode guard = abstractNode.children[0];

                if (table[guard.id].flowStart != null)
                    node.flowStart = guard.id;
                break;
            case ForLoop:
                for (AbstractNode childAbstractNode : abstractNode.children)
                    establishFlow(childAbstractNode, abstractNode); //recursively call on children

                AbstractNode assign = abstractNode.children[0];
                AbstractNode compare = abstractNode.children[1];
                AbstractNode increment = abstractNode.children[2];

                if (table[assign.id].flowStart != null && table[compare.id].flowStart != null && table[increment.id].flowStart != null)
                    node.flowStart = abstractNode.id;
                break;
            case Call:
                Integer bodyId = table[abstractNode.id].usageSource;
                if (bodyId != null) {
                    AbstractNode bodyAbstractNode = tree.nodes[bodyId];
                    establishFlow(bodyAbstractNode, abstractNode);
                }
                break;
            case Proc:
                if (table[parent.id].nodeType != AbstractNodeType.Call) {
                    pushFlowStack();
                    for (AbstractNode childAbstractNode : abstractNode.children)
                        establishFlow(childAbstractNode, abstractNode); //recursively call on children
                    popFlowStack();
                } else {
                    for (AbstractNode childAbstractNode : abstractNode.children)
                        establishFlow(childAbstractNode, abstractNode); //recursively call on children
                }
                break;
            case Halt:
                node.reachable = true;
                return true;
            default:
                for (AbstractNode childAbstractNode : abstractNode.children)
                    establishFlow(childAbstractNode, abstractNode); //recursively call on children
                break;
        }
        node.reachable = true;
        return halt;
    }

    private void establishType(AbstractNode abstractNode) {
        AbstractNodeType nodeType = abstractNode.type;
        SemanticNode node = table[abstractNode.id];
        switch (nodeType) {
            case Output:
            case Input:
                NameType nameType = table[abstractNode.children[0].id].nameType;
                if ((nameType == NameType.B || nameType == NameType.N || nameType == NameType.S))
                    node.nameType = nameType;
                break;
            case Call:
                if (node.usageSource != null) {
                    nameType = table[node.usageSource].nameType;
                    if (nameType == NameType.P)
                        node.nameType = NameType.P;
                }
                break;
            case Halt:
            case ProcDefs:
            case Code:
            case Prog:
                node.nameType = NameType.C;
                break;
            case StrDecl:
            case String:
                node.nameType = NameType.S;
                break;
            case Proc:
                node.nameType = NameType.P;
                break;
            case Number:
            case NumDecl:
                node.nameType = NameType.N;
                break;
            case BoolDecl:
            case True:
            case False:
                node.nameType = NameType.B;
            case Var:
                if (node.usageSource != null) {
                    nameType = table[node.usageSource].nameType;
                    node.nameType = nameType;
                }
                break;
            case Assign:
                NameType leftOperandType = table[abstractNode.children[0].id].nameType;
                NameType rightOperandType = table[abstractNode.children[1].id].nameType;
                if (leftOperandType == rightOperandType)
                    node.nameType = leftOperandType;
                break;
            case AddExpr:
            case SubExpr:
            case MultExpr:
                leftOperandType = table[abstractNode.children[0].id].nameType;
                rightOperandType = table[abstractNode.children[1].id].nameType;
                if (leftOperandType == rightOperandType && leftOperandType == NameType.N)
                    node.nameType = leftOperandType;
                break;
            case CondBranch:
                NameType guard = table[abstractNode.children[0].id].nameType;
                if (guard == NameType.B)
                    node.nameType = NameType.C;
                break;
            case EqExpr:
                leftOperandType = table[abstractNode.children[0].id].nameType;
                rightOperandType = table[abstractNode.children[1].id].nameType;
                if (leftOperandType == rightOperandType)
                    node.nameType = NameType.B;
                break;
            case LessExpr:
            case GreaterExpr:
                leftOperandType = table[abstractNode.children[0].id].nameType;
                rightOperandType = table[abstractNode.children[1].id].nameType;
                if (leftOperandType == rightOperandType && leftOperandType == NameType.N)
                    node.nameType = NameType.B;
                break;
            case NotExpr:
                guard = table[abstractNode.children[0].id].nameType;
                if (guard == NameType.B)
                    node.nameType = guard;
                break;
            case AndExpr:
            case OrExpr:
                leftOperandType = table[abstractNode.children[0].id].nameType;
                rightOperandType = table[abstractNode.children[1].id].nameType;
                if (leftOperandType == rightOperandType && leftOperandType == NameType.B)
                    node.nameType = NameType.B;
                break;
            case ForLoop:
                NameType assignment = table[abstractNode.children[0].id].nameType;
                NameType comparison = table[abstractNode.children[0].id].nameType;
                NameType increment = table[abstractNode.children[0].id].nameType;

                if (assignment == comparison && comparison == increment && assignment == NameType.N)
                    node.nameType = NameType.N;
                break;
            case WhileLoop:
                guard = table[abstractNode.children[0].id].nameType;
                if (guard == NameType.B)
                    node.nameType = NameType.C;
                break;
        }
    }

    private Integer getDeclarationSource(String name) {
        for (int a = symbolTable.size() - 1; a >= 0; a--) { //loop through scope layer stack
            if (symbolTable.get(a).get(name) != null) //lookup variable in scope layer symbol table
                return symbolTable.get(a).get(name); //if found
        }
        return null;
    }

    private boolean reDeclaration(String name) {
        return symbolTable.get(symbolTable.size() - 1).get(name) != null; //lookup variable in scope layer symbol table
    }

    private Integer getFlowStart(String name) {
        for (int a = flowStartTable.size() - 1; a >= 0; a--) { //loop through scope layer stack
            if (flowStartTable.get(a).get(name) != null) //lookup variable in scope layer symbol table
                return flowStartTable.get(a).get(name); //if found
        }
        return null;
    }

    private void popFlowStack() {
        flowStartTable.remove(flowStartTable.size() - 1);
    }

    private void pushFlowStack() {
        flowStartTable.add(new HashMap<>());
    }

    private void putFlowStart(String name, int flowStart) {
        flowStartTable.get(flowStartTable.size() - 1).put(name, flowStart); //insert declaration in flow table
    }

    private void reNameProcesses(AbstractNode abstractNode) {
        AbstractNodeType nodeType = abstractNode.type;
        switch (nodeType) {
            case ProcDefs:
                for(int a = abstractNode.children.length - 1; a >= 0; a--)
                    reNameProcesses(abstractNode.children[a]);
                break;
            case Proc:
                table[abstractNode.id].snippet = tree.nodes[abstractNode.id].val = ("P" + processCount++);
        }
    }

    private void reName(AbstractNode abstractNode) {
        if (abstractNode == null)
            return;
        AbstractNodeType nodeType = abstractNode.type;
        switch (nodeType) {
            case Prog:
                if (abstractNode.children.length > 1)
                    reNameProcesses(abstractNode.children[1]);
                for (AbstractNode childAbstractNode : abstractNode.children)
                    reName(childAbstractNode);
                break;
            case Call:
            case Var:
                Integer source = table[abstractNode.id].usageSource;
                table[abstractNode.id].snippet = tree.nodes[abstractNode.id].val = (source == null? "U" : tree.nodes[source].val);
                break;
            case NumDecl:
            case BoolDecl:
            case ForLoop:
            case StrDecl:
                table[abstractNode.id].snippet = tree.nodes[abstractNode.id].val = ("V" + variableCount++);
            default:
                for (AbstractNode childAbstractNode : abstractNode.children)
                    reName(childAbstractNode);
                break;
        }
    }

    public String toString() {
        return toString(tree.root, "");
    }

    public String toString(AbstractNode node, String indent) {
        String string = "";
        switch (table[node.id].nodeType) {
//            case Prog:
//            case Proc:
//            case ForLoop:
//            case WhileLoop:
//                string = String.format("%-3d|", node.id) + indent + table[node.id].toString() + " {\r\n";
//                for (AbstractNode abstractNode : node.children)
//                    string += toString(abstractNode, indent + "|  ");
//                string += "   |" + indent + "}\r\n";
//                break;
            default:
                string = String.format("%-3d|", node.id) + indent + table[node.id].toString() + "\r\n";

                for (int a = 0; a < node.children.length; a++) {
                    string += toString(node.children[a], indent + (a == (node.children.length - 1)? "   " : "|  "));
                }
        }


        return string;
    }
}
