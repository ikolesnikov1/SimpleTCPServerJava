package receiver;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        try {
            Thread receiverThread = new Thread(new Receiver(port));
            receiverThread.start();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}