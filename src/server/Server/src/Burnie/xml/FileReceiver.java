package Burnie.xml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileReceiver {
    public static String acceptFile(byte[] bytes, String fileName) throws IOException {
        File file = new File(fileName);
        file.createNewFile();
        BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get(fileName)));
        out.write(bytes);
//        byte[] buffer = new byte[4096];
//        int count;
//        while ((count = in.read(buffer, 0, buffer.length)) != -1) {
//            System.out.println(count);
//            out.write(buffer, 0, count);
//        }
        return file.getAbsolutePath();
    }
//    public static String acceptFile(BufferedInputStream in, String fileName) throws IOException {
//        File file = new File(fileName);
//        file.createNewFile();
//        BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get(fileName)));
//        byte[] buffer = new byte[5000];
//        int count;
//        while ((count = in.read(buffer, 0, buffer.length)) != -1) {
//            System.out.println(count);
//            out.write(buffer, 0, count);
//        }
//        return file.getAbsolutePath();
//    }
}
