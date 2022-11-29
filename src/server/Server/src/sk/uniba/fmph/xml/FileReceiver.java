package sk.uniba.fmph.xml;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileReceiver {
    public static void acceptFile(BufferedReader in, String fileName) throws IOException {
        File file = new File(fileName);
        file.createNewFile();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8)); //TODO -> get some file name
        char[] buffer = new char[4096];
        int count;
        while ((count = in.read(buffer)) > 0) {
            out.write(buffer, 0, count);
        }

        in.close();
        out.close();
    }
}
