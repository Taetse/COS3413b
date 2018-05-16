import java.util.ArrayList;

/**
 * Created by Victor on 3/29/2018.
 */
public class AbstractNode {
    String val = "";
    int id;

    AbstractNodeType type;
    AbstractNode parent;
    private int childrenArray[] = null;
    public AbstractNode children[] = new AbstractNode[0];
    AbstractNode nodes[];

    public AbstractNode(int id, AbstractNodeType type, int childArray[], AbstractNode nodes[]) {
        this.nodes = nodes;
        this.childrenArray = childArray;
        this.type = type;
        this.id = id;
    }

    public AbstractNode(int id, String val, AbstractNodeType type, int childArray[], AbstractNode nodes[]) {
        this(id, type, childArray, nodes);
        this.val = val;
    }

    public void construct() {
        if (childrenArray != null) {
            children = new AbstractNode[childrenArray.length];
            int a = 0;
            for (int childIndex : childrenArray) {
                AbstractNode childNode = nodes[childIndex];
                childNode.parent = this;
                children[a++] = childNode;
                childNode.construct();
            }
        }
    }
}
