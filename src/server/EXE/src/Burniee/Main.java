package Burniee;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            try {
                Client client = new Client();
                if (args.length > 1) {
                    client.performEndOfSegment(args[0], args[1]);
                } else {
                    client.performInit(args[0]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Usage:\n" +
                    "java -jar EXE.jar \"path_to_xml\"\n" +
                    "java -jar EXE.jar \"name_of_block\" \"path_to_xml\"");
        }
    }
}
