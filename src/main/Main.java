package main;

import java.util.Random;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.Scanner;
import java.util.Enumeration;

/**
 *
 * @author theunknown
 */
public class Main {

    String Broadcast_id;
    int udp_port;
    String Username;

    final String PREFIX_MESSAGE = "MESSAGE:";
    final String PREFIX_CONNECT = "CONNECT:";
    final String PREFIX_CLOSE = "CLOSE:";
    int port = 3332;

    public Main(String BroadcastID, int udp_port) {
        this.Broadcast_id = BroadcastID;
        this.udp_port = udp_port;
    }

    public void start() {
        try {
            server();
        } catch (IOException ex) {
            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    /**
     * @param args the command line arguments
     */
    private class clientHandler implements Runnable {

        String message;

        Socket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;

        public clientHandler(Socket socket) {
            this.socket = socket;
            try {
                reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.err.println("Handler setup error: " + e.getMessage());
            }
        }

        public void sendMessage(String message) {
            this.message = PREFIX_MESSAGE + message;
        }

        @Override
        public void run() {
            try {
                while ((message = reader.readLine()) != null) {
                    if (message.startsWith(PREFIX_CONNECT)) {
//                        String[] message_part = message.substring(PREFIX_CONNECT.length()).split(":");
//                        writer.println(PREFIX_MESSAGE + message);
                        continue;
                    } else if (message.startsWith(PREFIX_MESSAGE)) {
                        String message_part = message.substring(PREFIX_CONNECT.length()).split(":").toString();
                        writer.println(message_part);
                        continue;
                    } else if (message.startsWith(PREFIX_CLOSE)) {

                        if (reader != null) {
                            reader.close();
                        }
                        if (writer != null) {
                            writer.close();
                        }
                        if (this.socket != null) {
                            socket.close();
                        }

                        return;
                    }
                }
            } catch (IOException ex) {
                System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
    }

public String getLocalIp() {
    Enumeration<NetworkInterface> nets = null;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    for (NetworkInterface netint : java.util.Collections.list(nets)) {
        try {
            if (netint.isUp() && !netint.isLoopback()) {
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : java.util.Collections.list(inetAddresses)) {
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
    return "127.0.0.1"; // fallback
}
    public void server() throws IOException {
        ServerSocket ser = new ServerSocket(port); // TCP server socket

        // Thread for accepting TCP client connections
        new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = ser.accept();
                    System.out.println("Peer connected: " + clientSocket.getInetAddress());
                    clientHandler clint = new clientHandler(clientSocket);
                    new Thread(clint).start();
                } catch (IOException e) {
                    System.out.println("Error accepting client: " + e.getMessage());
                }
            }
        }).start();

        // Thread for UDP broadcasting presence
        new Thread(() -> {
            String message = PREFIX_CONNECT + Username + ":" + getLocalIp() + ":" + port;
            byte[] messageByte = message.getBytes();
            DatagramSocket dSocket = null;
            try {
                dSocket = new DatagramSocket();
                dSocket.setBroadcast(true);
            } catch (Exception e) {
                System.out.println("Error creating UDP socket: " + e.getMessage());
                System.exit(100);
            }

            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(
                            messageByte,
                            messageByte.length,
                            InetAddress.getByName(Broadcast_id),
                            udp_port // UDP port for broadcasting
                    );
                    dSocket.send(packet);
                } catch (IOException e) {
                    System.out.println("Error sending UDP broadcast: " + e.getMessage());
                }
                try {
                    Thread.sleep(1000); // Sleep 1 second between broadcasts
                } catch (InterruptedException e) {
                    System.out.println("Broadcast thread interrupted");
                }
            }
        }).start();

        // Thread for receiving UDP broadcasts from peers
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(udp_port, InetAddress.getByName("0.0.0.0"))) {
                byte[] buffer = new byte[1024];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    if (received.startsWith(PREFIX_CONNECT)) {
                        String[] parts = received.substring(PREFIX_CONNECT.length()).split(":");
                        String username = parts[0];
                        String ip = parts[1];
                        int peerPort = Integer.parseInt(parts[2]);
                        System.out.println("Discovered peer: " + username + " at " + ip + ":" + peerPort);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error receiving UDP packets: " + e.getMessage());
            }
        }).start();
    }

    public static void main(String[] args) throws IOException {
        Main messenger = new Main("255.255.255.255", 6969);

        Scanner scan = new Scanner(System.in);

        System.out.println("Enter your username: ");
        String user_message = scan.nextLine();
        messenger.Username = user_message;

        messenger.start();

        System.out.println("Enter your message: ");
        user_message = scan.nextLine();
        String first_word = user_message.split(" ", 1).toString();
        while (true) {
            switch (first_word) {
                case "/message": {

                    break;
                }
            }
        }

    }

}
