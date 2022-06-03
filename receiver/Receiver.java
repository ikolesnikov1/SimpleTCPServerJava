package receiver;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Timer;

public class Receiver implements Runnable {
    ServerSocket socket;

    public Receiver(int port) throws IOException {
        socket = new ServerSocket(port, 0, InetAddress.getLocalHost());
        System.out.println("Server started. Address: " + socket.getLocalSocketAddress());
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket newSenderSocket = socket.accept();
                Thread newSenderThread = new Thread(new SenderThread(newSenderSocket));
                newSenderThread.start();
                System.out.println("New sender. Address: " + newSenderSocket.getLocalSocketAddress());
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                socket.close();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}

class SenderThread implements Runnable {
    private final Socket socket;
    private final String fileDir = System.getProperty("user.dir") + "/uploads";
    private String fileName;
    private long fileSize;
    private final InputStream in;
    private Timer timer;

    public SenderThread(Socket socket) throws IOException {
        this.socket = socket;
        in = socket.getInputStream();
    }

    @Override
    public void run() {
        try {
            receiveHeader();
            receiveFile();
        }
        catch (IOException | IndexOutOfBoundsException ex) {
            System.out.println("Connection to sender of '" + fileName +
                    "' lost. File didn't receive");
        }
        finally {
            timer.cancel();
            try {
                socket.close();
                System.out.println("Socket of '" + fileName + "' closed");
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    void receiveHeader() throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());

        byte[] fileNameLengthBuf = new byte[2];
        in.readFully(fileNameLengthBuf);
        int fileNameLength = ByteBuffer.wrap(fileNameLengthBuf).getShort();

        byte[] fileNameBuf = new byte[fileNameLength];
        in.readFully(fileNameBuf);
        fileName = new String(fileNameBuf);

        byte[] fileSizeBuf = new byte[8];
        in.readFully(fileSizeBuf);
        fileSize = ByteBuffer.wrap(fileSizeBuf).getLong();
        System.out.println("Header received: '" + fileName + "', " + fileSize + " bytes");
    }

    void receiveFile() throws IOException, IndexOutOfBoundsException {
        new File(fileDir).mkdirs();
        File outFile = new File(fileDir + "/" + fileName);
        outFile.createNewFile();
        if (!outFile.exists()) throw new IOException();
        FileOutputStream outFileStream = new FileOutputStream(outFile);
        int BUF_SIZE = 4096;
        byte[] fileBuf = new byte[BUF_SIZE];

        timer = new Timer();
        long bytesReceived = 0;
        long bytesRemain = fileSize;
        long startTime = System.currentTimeMillis();
        while (bytesRemain > 0) {
            int bytesReceivedNow = in.read(fileBuf, 0,
                    bytesRemain < BUF_SIZE ? (int) bytesRemain : BUF_SIZE);
            bytesReceived += bytesReceivedNow;
            bytesRemain -= bytesReceivedNow;
            outFileStream.write(fileBuf, 0, bytesReceivedNow);
        }
        outFileStream.flush();
        long endTime = System.currentTimeMillis();

        System.out.printf("File '%s' received! Average speed: %.3f Mb/s\n", fileName,
                (fileSize * 1000.0 / (endTime - startTime) / 8 / 1024 / 1024));
    }
}