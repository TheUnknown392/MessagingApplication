package main;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 * @author theunknown
 */
public class Main {
// String Key = peerUser + ":" + peerIp +":"+ peerPort;

    Set<String> clients = ConcurrentHashMap.newKeySet();
    
    String Broadcast_id;
    int udp_port;
    String Username;
    String Username_file;
    
    final String FUSERNAME = "/.messagingAppUsername";
    
    final String PREFIX_MESSAGE = "MESSAGE:";
    final String PREFIX_CONNECT = "CONNECT:";
    final String PREFIX_CLOSE = "CLOSE:";
    int port = 3332;
    
    public Main(String BroadcastID, int udp_port) {
        this.Broadcast_id = BroadcastID;
        this.udp_port = udp_port;
        this.Username_file = System.getProperty("user.home") + FUSERNAME;
    }
    
    public void start() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(this.Username_file));
            this.Username = reader.readLine();
        } catch (FileNotFoundException e) {
            Scanner scan = new Scanner(System.in);
            System.out.println("Enter your username: ");
            String username = scan.nextLine();
            
            FileWriter writer = null;
            try {
                writer = new FileWriter(this.Username_file);
                writer.write(username);
                this.Username = username;
            } catch (IOException f) {
                System.out.println("An error occurred while writing to the file.");
                e.printStackTrace();
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException f) {
                    System.out.println("An error occurred while closing the file.");
                    e.printStackTrace();
                }
            }
            
        } catch (IOException e) {
            System.out.println("Can't read from the file");
            e.printStackTrace();
        }
        
        try {
            server();
        } catch (IOException ex) {
            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    /**
     * sending requests to peers we found from broadcast
     *
     * @param ip
     * @param peerServerPort
     */
    private void sendConnectionRequest(String username, String ip, int peerServerPort) { // client side socket
        new Thread(() -> {
            Socket host = null;
            try {
                host = new Socket();
                host.connect(new InetSocketAddress(ip, peerServerPort)); // 3000 timeout

                String requestKey = PREFIX_CONNECT + this.Username + ":" + getLocalIp() + ":" + this.port;
                PrintWriter writer = new PrintWriter(host.getOutputStream(), true);
                writer.println(requestKey);
                System.out.println("Connection request sent to: (sendConnectionRequest) " + ip);
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(host.getInputStream()));
                String acceptMessage = "Accepted";
                if (reader.readLine().equals(acceptMessage)) {
                    String Key = username + ":" + ip + ":" + peerServerPort;
                    clients.add(Key);
                    System.out.println("Addded key (sendConnectionRequest): " + Key);
                }
            } catch (IOException ex) {
                try {
                    host.close();
                } catch (IOException ex1) {
                }
                System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }).start();
        
    }

    /**
     * handles clients incomplete
     */
    private class clientHandler implements Runnable {
        
        String message;
        String username;
        
        Socket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;
        
        public clientHandler(Socket socket, String peerUser) {
            System.out.println("connection request recieved from: (clientHandler) " + peerUser + socket.getInetAddress());
            
            this.socket = socket;
            this.username = peerUser;
            try {
                reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                // notifying the client that connection request was accepted
                writer.println("Accepted");
            } catch (IOException e) {
                System.err.println("Handler setup error: " + e.getMessage());
            }
            System.out.println("Conntected client: " + username);
        }
        
        public void sendMessage(String message) {
            if (writer != null) {
                writer.println(PREFIX_MESSAGE + message);
            }
        }
        
        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        message = reader.readLine();
                        if (message == null) {
                            break; // connection closed by peer
                        }
                    } catch (SocketTimeoutException ste) {
                        // Timeout, no data, continue listening
                        continue;
                    }
                    
                    if (message.startsWith(PREFIX_CONNECT)) {
                        continue;
                    } else if (message.startsWith(PREFIX_MESSAGE)) {
                        String[] message_part = message.substring(PREFIX_MESSAGE.length()).split(":");
                        String formattedMessage = String.join(":", message_part);
                        writer.println(formattedMessage);
                        continue;
                    } else if (message.startsWith(PREFIX_CLOSE)) {
                        break;
                    }
                }
            } catch (IOException ex) {
                System.out.println("Error  in run of client handeler");
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                    if (writer != null) {
                        writer.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * gets local IP
     *
     * @return IP
     */
    public String getLocalIp() {
        Enumeration<NetworkInterface> nets = null;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        if (nets == null) {
            return "127.0.0.1";
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

    /**
     * starts main server
     *
     * @throws IOException
     */
    public void server() throws IOException {
        
        ServerSocket ser = new ServerSocket(port);
        System.out.println("your ip is: " + getLocalIp());
        System.out.println("your username: " + this.Username);
        System.out.println("your port: " + this.port);

        // TCP server thread (accept incoming connections)
        new Thread(() -> { // server side socket
            while (true) {
                try {
                    Socket incoming = ser.accept();
                    
                    BufferedReader in = new BufferedReader(new InputStreamReader(incoming.getInputStream()));

//                    incoming.setSoTimeout(5000);
                    String peerIdentity = in.readLine(); // PREFIX_CONNECT:username:ip:port

                    if (peerIdentity != null && peerIdentity.startsWith(PREFIX_CONNECT)) {
                        String[] parts = peerIdentity.substring(PREFIX_CONNECT.length()).split(":");
                        String peerUser = parts[0];
                        String peerIp = parts[1];
                        String peerPort = parts[2];
                        
                        String Key = peerUser + ":" + peerIp + ":" + peerPort;

                        // still handle dublicates
                        if (!(clients.contains(Key) && peerUser.equals(this.Username) && peerIp.equals(getLocalIp()))) {
                            clients.add(Key);
                            System.out.println("Accepted Request: (serverThread)" + getLocalIp());
                            System.out.println("Added Key (ServerThread): " + Key);
                            new Thread(new clientHandler(incoming, peerUser)).start();
                        } else {
                            incoming.close();
                        }
                        
                    } else {
                        incoming.close();
                    }
                } catch (IOException e) {
                    System.out.println("Error accepting client: " + e.getMessage());
                }
            }
        }).start();

        // UDP broadcasting presence
        new Thread(() -> {
            String message = PREFIX_CONNECT + Username + ":" + getLocalIp() + ":" + this.port;
            System.out.println("broadcasting this message in bytes: " + message);
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
                            udp_port
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

        // UDP receiving thread (discover peers)
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(udp_port, InetAddress.getByName("0.0.0.0"))) {
                byte[] buffer = new byte[1024];
                
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String received = new String(packet.getData(), 0, packet.getLength());
                    if (received != null && received.startsWith(PREFIX_CONNECT)) {
                        String[] parts = received.substring(PREFIX_CONNECT.length()).split(":");
                        String username = parts[0];
                        String ip = parts[1];
                        int peerPort = Integer.parseInt(parts[2]);
                        
                        if (!(username.equals(this.Username) && ip.equals(this.getLocalIp()))) {
                            System.out.println("Discovered peer: " + username + " at " + ip + ":" + peerPort);
                            if (!(clients.contains(username + ":" + ip + ":" + peerPort))) {
                                sendConnectionRequest(username, ip, peerPort);
                            }
                        }
                    }
                    
                }
            } catch (IOException e) {
                System.out.println("Error receiving UDP packets: " + e.getMessage());
            }
        }).start();
    }
    
    public static void main(String[] args) throws IOException {
        Main messenger = new Main("255.255.255.255", 64444);
        
        Scanner scan = new Scanner(System.in);
        
        messenger.start();
        
        System.out.println("Enter your message: ");
        String user_message = scan.nextLine();
        String first_word = user_message.split(" ")[0];
        while (true) {
            switch (first_word) {
                case "/message": {
                    break;
                }
            }
        }
    }
}
