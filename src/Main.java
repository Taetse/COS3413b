import java.io.*;
import java.util.ArrayList;


/**
 * Created by Victor on 3/29/2018.
 */

public class Main {
    public static void main(String strings[]) {
        String abstractFileName = (strings.length > 0? strings[0] : "parser/at.txt");
        ArrayList<String> lines = getAbstractFile(abstractFileName);

        AbstractTree abstractTree = new AbstractTree(lines);
        SemanticTable semanticTable = new SemanticTable(abstractTree);
        Translator translator = new Translator(abstractTree);

        System.out.println(semanticTable.toString());
        System.out.println(translator.getIntermediateCode());
        saveToFile("./auxiliary.txt", translator.getIntermediateCode());
        System.out.println(translator.getFinalIntermediateCode());
        saveToFile("./finalOutput.txt", translator.getFinalIntermediateCode());
    }

    private static void saveToFile(String fileName, String content) {
        try {
            PrintWriter out = new PrintWriter(fileName);
            out.print(content);
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