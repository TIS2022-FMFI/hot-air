package sk.uniba.fmph.Burnie.xml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileReceiver {
    public static void acceptFile(BufferedInputStream in, String fileName) throws IOException {
        File file = new File(fileName);
        file.createNewFile();
        BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get(fileName)));
        byte[] buffer = new byte[4096];
        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }

        in.close();
        out.close();
    }
}
