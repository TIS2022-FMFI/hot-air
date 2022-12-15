package sk.uniba.fmph.xml;

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
        System.out.println("FileCreated");
        while ((count = in.read(buffer)) != -1) {
            System.out.println("Reading");
            out.write(buffer, 0, count);
        }
        System.out.println("Reading done");

        in.close();
        out.close();
    }
}
