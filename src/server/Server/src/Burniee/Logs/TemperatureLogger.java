package Burniee.Logs;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class TemperatureLogger {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
    private String fileName = "";

    /**
     * Creates new instance of Temperature logger. Create one instance per xml project
     *
     * @param projectName Used as a part of the name of created log file
     * @param projectPath Specifies where folder with temperature logs is created.
     *                    Use path also with .../filename.xml.
     *                    If specified path doesn't exist, folder is created in current location
     * @throws IOException
     */
    public TemperatureLogger(String projectName, String projectPath) throws IOException {
        projectPath = new File(projectPath).getParent() + "\\";
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        sdf.format(timestamp);
        try {
            Files.createDirectory(Paths.get(projectPath + "temperature_logs"));
        } catch (FileAlreadyExistsException ignored){
        } catch (NoSuchFileException e){
            projectPath = "";
            try {
                Files.createDirectory(Paths.get(projectPath + "temperature_logs"));
            } catch (FileAlreadyExistsException ignored){}
        }
        fileName = projectPath + "temperature_logs\\" + projectName
                + "_temperatures_" + sdf.format(timestamp) + ".csv";
        File newLogFile = new File(fileName);
        newLogFile.createNewFile();
    }

    /**
     * Appends one line to temperature log file. Make sure that values in Lists are in the same order
     * (val in blowerIds on index 1 corresponds to index 1 in temps)
     * @param phase name of current phase
     * @param blowerIds List of IDs of blowers
     * @param temps List of actual temperatures of blowers
     * @param target List of target temperatures of blowers
     * @throws IOException
     */
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

