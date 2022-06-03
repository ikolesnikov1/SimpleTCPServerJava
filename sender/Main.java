package sender;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

public class Main {

    public static void main(String[] args) {
        String fileName = args[0];
        String address = args[1];
        int port = Integer.parseInt(args[2]);

        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            File file = new File(fileName);
            if (file.exists()) {
                Thread senderThread = new Thread(new Sender(inetAddress, port, file));
                senderThread.start();
            }
            else {
                System.out.println("File '" + fileName + "' not found");
            }
        }
        catch (IOException ex) {
            System.out.println("Can't connect to server!");
        }
    }
}
