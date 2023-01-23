package Burniee.Logs;
import java.io.BufferedWriter;
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
    private final BufferedWriter bufferedWriter;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");

    public TemperatureLogger(String projectName) throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        sdf.format(timestamp);
        try {
            Files.createDirectory(Paths.get("temperature_logs"));

        } catch (FileAlreadyExistsException ignored){
            File dir = new File("temperature_logs");
            Date date = new Date();
            if (dir.isDirectory()){
                for (File f: Objects.requireNonNull(dir.listFiles())){
                    if (date.getTime() - f.lastModified() > 30L * 24 * 60 * 60 * 1000){
                        f.delete();
                    }
                }
            }
        }

        bufferedWriter = new BufferedWriter(new FileWriter("temperature_logs\\" + projectName
                + "_temperatures_" + sdf.format(timestamp) + ".csv", true));
    }

    public void logTemeperature(String phase, List<String> blowerIds, List<String> temps)
            throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        bufferedWriter.write(timestamp + ", " + phase);
        for (int i = 0; i < blowerIds.size(); i++){
            bufferedWriter.write(", " + blowerIds.get(i) + ", " + temps.get(i));
        }
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }
}

