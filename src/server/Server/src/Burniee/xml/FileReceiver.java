package Burniee.xml;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileReceiver {
    private static final String DIR_NAME = "projects";

    public static String acceptFile(byte[] bytes, String fileName) throws IOException {
        try {
            Files.createDirectory(Paths.get(DIR_NAME));
        } catch (FileAlreadyExistsException ignored) {}
        File file = new File(DIR_NAME + "\\" + fileName);
        try {
            file.createNewFile();
        } catch (FileAlreadyExistsException ignored) {}
        try (FileOutputStream fos = new FileOutputStream(DIR_NAME + "\\" + fileName)) {
            fos.write(bytes);
        }
        return file.getAbsolutePath();
    }
}
