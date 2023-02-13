package Burniee.xml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileReceiver {
    private static final String DIR_NAME = "projects";

    public static String acceptFile(byte[] bytes, String fileName) throws IOException {
        Files.createDirectory(Paths.get(DIR_NAME));
        File file = new File(DIR_NAME + "\\" + fileName);
        file.createNewFile();
        try (FileOutputStream fos = new FileOutputStream(DIR_NAME + "\\" + fileName)) {
            fos.write(bytes);
        }
        return file.getAbsolutePath();
    }
}
