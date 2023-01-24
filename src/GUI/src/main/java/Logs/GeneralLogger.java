package Logs;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;

public class GeneralLogger {
    private static BufferedWriter bufferedWriter;

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
        }
    }

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
