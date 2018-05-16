import java.io.*;
import java.util.ArrayList;


/**
 * Created by Victor on 3/29/2018.
 */

public class Main {
    public static void main(String strings[]) {

//        try {
//            String[] args = new String[] {".\\re-parse.bat", "input.txt"};
//            Process proc = new ProcessBuilder(args).start();
//        }
//        catch (Exception e)
//        {
//            System.out.println("Could not re-parse");
//        }

        String abstractFileName = (strings.length > 0? strings[0] : "at.txt");
        ArrayList<String> lines = getAbstractFile(abstractFileName);

        AbstractTree abstractTree = new AbstractTree(lines);
        SemanticTable semanticTable = new SemanticTable(abstractTree);
        Translator translator = new Translator(abstractTree, semanticTable);

        System.out.println(semanticTable.toString());
        System.out.println(translator.getIntermediateCode());
        saveToFile("./auxiliary.txt", translator.getIntermediateCode());
        System.out.println(translator.getFinalIntermediateCode());
        saveToFile("./output.bas", translator.getFinalIntermediateCode());

        LivenessAnalyzer la = new LivenessAnalyzer();

        System.out.println("\n\n"+la.printLivesness());
        System.out.println("\n\n"+la.printInOut());
        System.out.println(("\n\n"+la.cg.printGraph()));

}

    private static void saveToFile(String fileName, String content) {
        try {
            PrintWriter out = new PrintWriter(fileName);
            out.print(content);
            out.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private static ArrayList<String> getAbstractFile(String fileName) {
        ArrayList<String> lines = new ArrayList<>();
        try {
            File file = new File(fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null)
                lines.add(line);
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }
}