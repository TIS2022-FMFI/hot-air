package sk.uniba.fmph.Burnie;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            ClientHandler ch = new ClientHandler();
            System.out.println(ch.getNumberOfProjects());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
