import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class LivenessAnalyzer {

    static ArrayList<LivenessLine>lines;

    boolean changed = false;
    ColorGraph cg;

    public LivenessAnalyzer()
    {
        try {
            Scanner sc = new Scanner(new File("output.bas"));
            String line;
            lines = new ArrayList<>();

            for(int ln = 0; sc.hasNextLine(); ln++)
                lines.add(new LivenessLine(ln,sc.nextLine()));

            sc.close();

            calculateInOutSets();


            cg = new ColorGraph();


        } catch (FileNotFoundException e) {
            System.out.println("No 'output.bas' file detected!\nExiting...");
        }
    }

    private void calculateInOutSets()
    {
        changed = true;
        while(changed)
        {
            System.out.println("iterating..");
            changed = false;
            for(int i = 0; i < lines.size();i++)
            {
//                if(lines.get(i).modified==false)
                    calculateInSet(i);
            }
        }

    }


    private TreeSet<String> calculateInSet(int line) {

        if(lines.get(line).modified)
            return lines.get(line).in;

        LivenessLine temp;
        TreeSet<String> tempOut;
        TreeSet<String> tempIn = new TreeSet<>();

        //add gen
        temp = lines.get(line);
        tempIn.addAll(temp.gen);

        //get Out
        tempOut = calculateOutSet(line);

        //remove kill from out
        if(tempOut!=null)
        {
//            changed = true;
            tempOut.removeAll(temp.kill);
            tempIn.addAll(tempOut);
        }

        if(!changed && !tempIn.equals(temp.in))
            changed = true;

        temp.in = tempIn;
        temp.modified = true;
        return tempIn;

    }

    private TreeSet<String> calculateOutSet(int index)
    {
        TreeSet<String> tempOut = new TreeSet<>();

        if(index == lines.size()-1)
            return lines.get(index).out = tempOut;

        for(int line: lines.get(index).succ)
        {
            if(line>=index)///////////////////////////////////////////THIS IS A TEMP SOLUTION TO the recursive loop
                tempOut.addAll(calculateInSet(line));/////////////////CAUSED BY IMBEDDED PROCS!!!

        }

        if(!changed && !tempOut.equals(lines.get(index).out))
            changed=true;

        lines.get(index).out = tempOut;
        lines.get(index).modified=true;
        return tempOut;
    }


    public String printLivesness()
    {
        String out = "";

        for (LivenessLine l: lines)
            out+=l+"\n";

        return out;
    }

    public String printInOut()
    {
        String out = "";

        for (LivenessLine l: lines)
            out+=l.printInOutSets()+"\n";

        return out;
    }
}

class ColorGraph
{
    ArrayList<Edge> edges = new ArrayList<>();
    ArrayList<Color> colors = new ArrayList<>();
    Stack<Vertex> vertices = new Stack<>();


    public ColorGraph()
    {
        //generate graph

        for(String x: LivenessLine.vars)
            for(String y: LivenessLine.vars)
            {
                if(x.equals(y))
                    continue;

                for (LivenessLine l: LivenessAnalyzer.lines)
                    if(l.kill.contains(x) && l.out.contains(y))
                        edges.add(new Edge(x,y));

            }


        //color graph
        colorGraph();

    }

    private void colorGraph()
    {
        vertices.sort(new Comparator<Vertex>() {
            @Override
            public int compare(Vertex v1, Vertex v2) {
                return v1.egdes.size()-v2.egdes.size();
            }
        });

        //here we go
        while(!vertices.isEmpty())
        {
            ArrayList<Color> tempColors = new ArrayList<>(colors);
            Vertex v = vertices.pop();

            for(Edge e: v.egdes)
            {
                if(e.x==v)
                    tempColors.remove(e.y.col);
                else
                    tempColors.remove(e.x.col);
            }

            if(tempColors.isEmpty())
                v.col = new Color();
            else
                v.col = tempColors.get(tempColors.size()-1);
        }
    }


    class Color
    {
        int uid;
        int r,g,b;


        Color()
        {
//            r = (int) (Math.random()*255);
//            g = (int) (Math.random()*255);
//            b = (int) (Math.random()*255);
            uid = colors.size();
            colors.add(this);
        }

        public String toString()
        {
            return ""+uid;
        }
    }

    private class Vertex
    {
        String var;
        Color col;
        ArrayList<Edge> egdes= new ArrayList<>();

        public Vertex(String var)
        {
            this.var = var;
        }

        @Override
        public String toString() {
            return var+ " := Color: "+col;
        }
    }

    private class Edge
    {
        Vertex x,y;

        public Edge(String x, String y)
        {
            for(Vertex v: vertices)
                if(v.var.equals(x))
                    this.x = v;
                else
                if(v.var.equals(y))
                    this.y = v;

            if(this.x==null)
                vertices.add(this.x=new Vertex(x));

            if(this.y == null)
                vertices.add(this.y=new Vertex(y));

            this.x.egdes.add(this);
            this.y.egdes.add(this);

        }

        public boolean contains(String s)
        {
            return x.equals(s) || y.equals(s);
        }

        public String toString()
        {
            return x + " <-> " + y;
        }

    }

    public String printGraph()
    {
        String out="\n-----GRAPH-----\n";
        for (Edge e: edges)
            out+=e+"\n";

        return out;
    }

}


class LivenessLine{

    int lineNumber;
    String op;

    TreeSet<Integer> succ;
    TreeSet<String> gen;
    TreeSet<String> kill;

    TreeSet<String>in;
    TreeSet<String>out;

    static ArrayList<Integer> gosubReturns;
    static TreeSet<String> vars = new TreeSet<>();

    boolean modified = false;

    public LivenessLine(int ln, String inputLine)
    {
        String[] inputLineArray = inputLine.split("\\s");

        op = inputLineArray[1];
        lineNumber = ln;

        gen = new TreeSet<>();
        kill = new TreeSet<>();
        succ = new TreeSet<>();

        switch (op)
        {
            case "LET":
                //assign statement
                kill.add(inputLineArray[2]);
                vars.add(inputLineArray[2]);

                //move after = and check it is var
                if(Character.isLetter(inputLineArray[4].charAt(0))) {
                    gen.add(inputLineArray[4]);
//                    vars.add(inputLineArray[4]);
                }

                //if still more - means an operation
                if((inputLineArray.length==7)  && Character.isLetter(inputLineArray[6].charAt(0)))
                {
                    gen.add(inputLineArray[6]);
//                    vars.add(inputLineArray[6]);
                }

                //update successor
                succ.add(lineNumber+1);
                break;
            case "IF":
                //if statement
                gen.add(inputLineArray[2]);
                gen.add(inputLineArray[4]);

                succ.add(Integer.parseInt(inputLineArray[7]));
                succ.add(lineNumber+1);
                break;
            case "INPUT":
                //input statement
                kill.add(inputLineArray[2]);

                vars.add(inputLineArray[2]);

                succ.add(lineNumber+1);
                break;
            case "PRINT":
                //output statement
                gen.add(inputLineArray[2]);

                succ.add(lineNumber+1);
                break;
            case "GOTO":
                succ.add(Integer.parseInt(inputLineArray[2]));
                break;
            case "GOSUB":

                if(gosubReturns==null)
                    gosubReturns = new ArrayList<>();

                gosubReturns.add(lineNumber);

                succ.add(Integer.parseInt(inputLineArray[2]));
                break;
            case "RETURN":
                succ.add(gosubReturns.remove(0)+1);
                break;
            case "END":
                //nothing
                break;
        }
    }

    public String toString()
    {
        String out  = "Succ["+lineNumber+"] = {";

        for(int s: succ)
            out+=","+s;

        out+="} --|-- Gen["+lineNumber+"] = {";

        for(String v: gen)
            out+=","+v;

        out+="} --|-- Kill["+lineNumber+"] = {";

        for(String v: kill)
            out+=","+v;

        out+="} --|--";

        return out;
    }

    public String printInOutSets()
    {
        String temp  = "In["+lineNumber+"] = {";

        for(String s: in)
            temp+=","+s;

        temp+="} --|-- Out["+lineNumber+"] = {";

        for(String v: out)
            temp+=","+v;

        return temp+"}";
    }

}
