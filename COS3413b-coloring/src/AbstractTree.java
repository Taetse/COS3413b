import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbstractTree {
    public AbstractNode root;
    public AbstractNode nodes[];

    AbstractTree(ArrayList<String> lines) {
        nodes = new AbstractNode[lines.size()];
        for (String line : lines) {
            Matcher matcher = Pattern.compile("[0-9]+").matcher(line);
            matcher.find();
            int id = Integer.parseInt(matcher.group(0));

            matcher = Pattern.compile("([a-z]|[A-Z])+(\\(.*\\))?").matcher(line);
            matcher.find();
            String nodeDecl = matcher.group(0);

            matcher = Pattern.compile("^([a-z]|[A-Z])+").matcher(nodeDecl);
            matcher.find();
            AbstractNodeType nodeType = AbstractNodeType.valueOf(matcher.group(0));

            matcher = Pattern.compile("([0-9]*| )*$").matcher(line);
            matcher.find();

            String childrenString = matcher.group(0).trim();
            int children[] = childrenString.length() == 0? null : Arrays.asList(childrenString.split(" ")).stream().mapToInt(Integer::parseInt).toArray();
            switch (nodeType) {
                
                case Call:
                case Number:
                case NumDecl:
                case Proc:
                case Var:
		case StrDecl:
		case BoolDecl:
                    matcher = Pattern.compile("([0-9]|[a-z]|[A-Z]| )+").matcher(nodeDecl);
                    matcher.find();
                    matcher.find();
                    String val = matcher.group(0);
                    nodes[id] = new AbstractNode(id, val, nodeType, children, nodes);
                    break;
		case String:
			matcher = Pattern.compile("\"([0-9]|[a-z]|[A-Z]| )*\"").matcher(nodeDecl.substring(nodeDecl.indexOf("(")));
			    //~ matcher.find();
			    matcher.find();
			    val = matcher.group(0);
			    nodes[id] = new AbstractNode(id, val, nodeType, children, nodes);
			break;
                default:
                    nodes[id] = new AbstractNode(id, nodeType, children, nodes);
                    break;
            }
        }
        root = nodes[0];
        root.construct();
    }
}
