package Burniee;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length == 2) {
            try {
                Client client = new Client(Integer.parseInt(args[1]));
                if (args[0].charAt(0) == '$') {
                    client.performEndOfSegment(args[0].substring(1));
                } else {
                    client.performInit(args[0]);
                }

//                if (args.length > 1) {
//                    client.performEndOfSegment(args[0], args[1]);
//                } else {
//                    client.performInit(args[0]);
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Usage:\n" +
                    "java -jar EXE.jar \"path_to_xml\" -> to start a new project\n" +
                    "java -jar EXE.jar \"$path_to_xml\" -> to tell server that a phase has come to an end");
        }
    }
}
