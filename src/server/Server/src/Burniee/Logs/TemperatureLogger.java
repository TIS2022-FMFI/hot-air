package Burniee.Logs;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class TemperatureLogger {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
    private String fileName = "";

    public TemperatureLogger(String projectName, String projectPath) throws IOException {
        projectPath = new File(projectPath).getParent();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        sdf.format(timestamp);
        try {
            Files.createDirectory(Paths.get(projectPath + "temperature_logs"));

        } catch (FileAlreadyExistsException ignored){
        }
        fileName = "temperature_logs\\" + projectName
                + "_temperatures_" + sdf.format(timestamp) + ".csv";
        File newLogFile = new File(fileName);
        newLogFile.createNewFile();
    }

    public static int numFilesToDelete(){
        return 0;
//        int counter = 0;
//        File dir = new File( "temperature_logs");
//        Date date = new Date();
//        if (dir.isDirectory()){
//            for (File f: Objects.requireNonNull(dir.listFiles())){
//                if (date.getTime() - f.lastModified() > 30L * 24 * 60 * 60 * 1000){
//                    counter++;
//                }
//            }
//        }
//        return counter;
    }

    public static void deleteFiles(){
        return;
//        File dir = new File("temperature_logs");
//        Date date = new Date();
//        if (dir.isDirectory()){
//            for (File f: Objects.requireNonNull(dir.listFiles())){
//                if (date.getTime() - f.lastModified() > 30L * 24 * 60 * 60 * 1000){
//                    f.delete();
//                }
//            }
//        }
    }

    public void logTemperature(String phase, List<String> blowerIds, List<String> temps, List<String> target)
            throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        FileWriter writer = new FileWriter(fileName, true);
        writer.write(timestamp + ", " + phase);
        for (int i = 0; i < blowerIds.size(); i++){
            writer.write(", " + blowerIds.get(i) + ", " + temps.get(i) + ", " + target.get(i));
        }
        writer.write("\n");
        writer.close();
    }

    public String getFileName(){
        return fileName;
    }
}

