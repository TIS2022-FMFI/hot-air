import Burniee.Server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            if (args.length > 0 && args[0].equals("instance")) { //this is so that a terminal is launched when .jar is double-clicked
                Server.getInstance().begin();
            } else {
                if (System.getProperty("os.name").startsWith("Linux")) { //on linux it works perfectly (as all things do)
                    Runtime.getRuntime().exec("gnome-terminal -- java -jar Server.jar instance");
                } else { //on windows..., you may need to look into Windows terminal
                    Runtime.getRuntime().exec("wt java -jar Server.jar instance");
                }
            }
        } catch (IOException e) {
            System.exit(-1);
        }
    }
}
