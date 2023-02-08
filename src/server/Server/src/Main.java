import Burniee.Server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            if (args.length > 0 && args[0].equals("instance")) {
                Server.getInstance().begin();
            } else {
                if (System.getProperty("os.name").startsWith("Linux")) {
                    Runtime.getRuntime().exec("gnome-terminal -- java -jar Server.jar instance");
                } else {
                    Runtime.getRuntime().exec("cmd java -jar Server.jar instance");
                }
            }
        } catch (IOException e) {
            System.exit(-1);
        }
    }
}
