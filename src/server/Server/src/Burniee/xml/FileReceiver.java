package Burniee.xml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileReceiver {
    public static String acceptFile(byte[] bytes, String fileName) throws IOException {
        File file = new File(fileName);
        file.createNewFile();
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(bytes);
        }
//        BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get(fileName)));
//        out.write(bytes);
        return file.getAbsolutePath();
    }
}
