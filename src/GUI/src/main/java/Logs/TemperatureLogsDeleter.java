package Logs;

import java.io.File;
import java.util.Date;
import java.util.Objects;

public class TemperatureLogsDeleter {
    public static void deleteFiles(){
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
}
