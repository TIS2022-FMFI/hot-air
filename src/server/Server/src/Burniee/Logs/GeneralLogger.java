package Burniee.Logs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.Arrays;

public class GeneralLogger {
    private static BufferedWriter bufferedWriter;

    /**
     * Writes exception and stack trace to log file
     * @param e Exception to write
     */
    public static synchronized void writeExeption(Exception e) {
        try {
            BufferedWriter writer = getWriter();
            writer.newLine();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            writer.write(String.valueOf(timestamp));
            writer.newLine();
            writer.write(String.valueOf(e));
            writer.newLine();
            writer.write(Arrays.toString(e.getStackTrace()));
            writer.newLine();
            writer.flush();
        } catch (IOException exception){
            System.out.println("Errror");
            GeneralLogger.writeMessage("Errror");
        }
    }

    /**
     * Writes message to log file
     * @param s Message to write
     */
    public static synchronized void writeMessage(String s) {
        try {
            BufferedWriter writer = getWriter();
            writer.newLine();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            writer.write(String.valueOf(timestamp));
            writer.newLine();
            writer.write(s);
            writer.newLine();
            writer.flush();
        } catch (IOException exception){
            System.out.println("Errror");
            GeneralLogger.writeMessage("Errror");
        }
    }

    /**
     * Checks if log file is over 1000kB in size. If yes, renames it to logOld (and removes previous logOld if exists)
     * and creates new log.txt
     */
    public static void deleteOldLogs(){
        try {
            File log = new File("log.txt");
            long bytes = Files.size(log.toPath());
            if (bytes/1024 > 1000) {
                File oldLog = new File(log.getParent(), "logOld.txt");
                if (oldLog.exists()){
                    oldLog.delete();
                }
                log.renameTo(oldLog);
            }
        } catch (IOException e){
            System.out.println("Errror");
            GeneralLogger.writeMessage("Error wen deleting old logs");
        }
    }

    private static BufferedWriter getWriter(){
        try{
            if( bufferedWriter == null )
            {
                bufferedWriter =  new BufferedWriter(new FileWriter("log.txt", true));
            }
            return bufferedWriter;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
