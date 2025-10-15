package main;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import database.*;
import crypto.*;

/**
 *
 * @author theunknown
 */
public class Main {

    Boolean debug = false;

    final String PREFIX_CONNECT = "CONNECT:";

    protected static Boolean EXIT = false;

    // String Key = peerUser + ":" + peerIp +":"+ peerPort;
    protected static ConcurrentHashMap<ConnectionKey, Socket> clients = new ConcurrentHashMap<>();

    // list of clients who couldn't connect. We wait for them
    protected static ConcurrentHashMap<ConnectionKey, Long> lastConnectAttempt = new ConcurrentHashMap<>();
    private static final long CONNECT_COOLDOWN_MS = 30000; // 30s cooldown

    private UserInfo user = null;

    private String Broadcast_id;
    private int udp_port;

    private final int port = 9332;

    public Main(String BroadcastID, int udp_port) {
        this.Broadcast_id = BroadcastID;
        this.udp_port = udp_port;
    }

    public void start() {

        while (getCurrentUser());
        try {
            server();
        } catch (IOException ex) {
            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    /**
     * returns false if sucessfully user created or password verified.
     *
     * @return
     */
    private boolean getCurrentUser() {
        GetConnectionDB conn = new GetConnectionDB("localhost", "3306", "messagedb", "user", "1234", this.debug);
        conn.getConnection();
        Query query = new Query(conn.conn, this.debug);

        Scanner scan = new Scanner(System.in);
        System.out.println("Enter your username: ");

        String username = scan.nextLine();

        user = query.getUser(username);

        if (user == null) {
            System.out.println("New user detected: ");
            System.out.println("Input your new password and don't forget it: ");

            String password = scan.nextLine();

            user = query.createUser(username, password);

            if (query.saveNewUser(user)) {
                System.exit(1);
            }
            return false;
        }

        System.out.println("Input your password: ");
        String password = scan.nextLine();

        boolean verification = new CryptoPassword(this.debug).verifyPassword(user, password);
        return verification;
    }

    /**
     * receives Key and accepts the connection if not exist
     *
     * @param received
     */
    private void sendConnectionRequest(String received) { // client side socket
        ConnectionKey connectionKey = new ConnectionKey(received, PREFIX_CONNECT);
        if (connectionKey.ip == null) {
            return;
        }
        int peerServerPort = Integer.parseInt(connectionKey.port);
        // ignore our own broadcast
        if (connectionKey.username.equals(user.username) && connectionKey.ip.equals(getLocalIp())) {
            return;
        }
        // only attempt if not already connected
        if (clients.containsKey(connectionKey)) {
            return;
        }

        if (falseConnection(connectionKey)) {
            return;
        }
        System.out.println(connectionKey);

        new Thread(() -> {
            Socket host = null;
            try {
                host = new Socket();
                host.connect(new InetSocketAddress(connectionKey.ip, peerServerPort));

                String requestKey = PREFIX_CONNECT + user.username + ":" + getLocalIp() + ":" + this.port;
                
                PrintWriter writer = new PrintWriter(host.getOutputStream(), true);
                writer.println(requestKey);

                if (debug) {
                    System.out.println("Connection request sent to: (sendConnectionRequest) " + connectionKey.ip);
                }

                if (host.isConnected()) {
                    lastConnectAttempt.remove(connectionKey);
                    clients.put(connectionKey, host);
                    if (debug) {
                        System.out.println("Addded key (sendConnectionRequest): " + connectionKey);
                    }
                    // listen to message from the thread
                    new Thread(new get_message(host, connectionKey.username)).start();

                }
            } catch (NoRouteToHostException e) {
                if (debug) {
                    System.err.println("no route to host error caught");
                }
            } catch (IOException ex) {
                if (host != null) {
                    try {
                        host.close();
                    } catch (IOException ignore) {
                    }
                }
                System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }).start();
    }

    /**
     * returns true if there is a false connection. true if we are already
     * connected or if it's in cooldown list
     *
     * @param key
     * @return
     */
    private boolean falseConnection(ConnectionKey connectionKey) {
        final long now = System.currentTimeMillis();
        Long last = lastConnectAttempt.get(connectionKey);

        if (clients.containsKey(connectionKey)) {
            if (debug) {
                System.out.println("Already connected to " + connectionKey);
            }
            return true;
        }
        if (last != null && now - last < CONNECT_COOLDOWN_MS) {
            if (debug) {
                System.out.println("Skipping recent attempt to " + connectionKey);
            }
            return true;
        }
        lastConnectAttempt.put(connectionKey, now);
        return false;
    }

    /**
     * handles clients incomming messages and sends connection requests to
     * clients
     */
    private class get_message implements Runnable {

//        String message;
        String username;

        Socket socket;
        BufferedReader reader = null;
        PrintWriter writer = null;

        public get_message(Socket socket, String peerUser) {
            if (debug) {
                System.out.println("ready to recieved message from: (clientHandler): " + peerUser + ":" + socket.getInetAddress().toString().substring(1));
            }

            this.socket = socket;
            this.username = peerUser;
            try {
                reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.err.println("Handler setup error: " + e.getMessage());
                cleanup();
            }
            if (debug) {
                System.out.println("Conntected client: " + username);
            }
        }

        @Override
        public void run() {
            try {
                // TODO handel through EncryptedMessage
                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println(username + ": " + message);
                    if (EXIT) {
                        break;
                    }
                }
            } catch (IOException ex) {
                System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {
            }

            if (writer != null) {
                writer.close();
            }

            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {
            }

            // remove this socket from clients list map thingie
            clients.entrySet().removeIf(e -> e.getValue() == this.socket);
            if (!debug) {
                System.out.println("Connection closed for: " + username);
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
        if (true) { // needed while making
            System.out.println("your ip is: " + getLocalIp());
            System.out.println("your username: " + user.username);
            System.out.println("your port: " + this.port);
        }

        // TCP server thread (accept incoming connections)
        new Thread(() -> { // server side socket
            while (true) {
                try {
                    Socket incoming = ser.accept();

                    BufferedReader in = new BufferedReader(new InputStreamReader(incoming.getInputStream()));

//                    incoming.setSoTimeout(5000);
                    String peerIdentity = in.readLine(); // PREFIX_CONNECT:username:ip:port
                    ConnectionKey connectionKey = new ConnectionKey(peerIdentity, PREFIX_CONNECT);
                    // if connection was invalid empty
                    if (connectionKey.ip == null) {
                        incoming.close();
                        continue;
                    }
                    // if connection were us
                    if (connectionKey.username.equals(user.username) && connectionKey.ip.equals(this.getLocalIp())) {
                        incoming.close();
                        continue;
                    }
                    

                    // if already connected, close duplicate incoming connection
                    if (clients.containsKey(connectionKey)) {
                        if (debug) {
                            System.out.println("Duplicate incoming connection for " + connectionKey + ", closing");
                        }
                        incoming.close();
                        continue;
                    }
                    
                    if (debug) {
                        System.out.println("key server: " + connectionKey);
                    }

                    // otherwise register and start handler
                    clients.put(connectionKey, incoming);
                    if (debug) {
                        System.out.println("Accepted Request: (serverThread)" + getLocalIp());
                        System.out.println("Added Key (ServerThread): " + connectionKey);
                    }
                    new Thread(new get_message(incoming, connectionKey.username)).start();
                } catch (IOException e) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
                if (EXIT) {
                    break;
                }
            }
        }).start();

        // UDP broadcasting presence
        new Thread(() -> {
            String message = PREFIX_CONNECT + user.username + ":" + getLocalIp() + ":" + this.port;

            if (debug) {
                System.out.println("broadcasting this message in bytes: " + message);
            }

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
                // TODO use multicast instead of Broadcasting everywhere
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
                    Thread.sleep(5000); // Sleep 5 second between broadcasts
                } catch (InterruptedException e) {
                    System.out.println("Broadcast thread interrupted");
                }
                if (EXIT) {
                    break;
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
                    sendConnectionRequest(received);
                    if (EXIT) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error receiving UDP packets: " + e.getMessage());
            }
        }).start();
    }

    private static void send_message(String userkey, String message) {
        if (!clients.containsKey(userkey)) {
            System.err.println("User does not exist");
            return;
        }

        Socket activeSocket = clients.get(userkey);

        try {
            PrintWriter writer = new PrintWriter(activeSocket.getOutputStream(), true);
            writer.println(message);
        } catch (IOException e) {
            System.err.println("Could not create a new reader and writer to send message: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        Main messenger = new Main("255.255.255.255", 64444);

        Scanner scan = new Scanner(System.in);

        messenger.start();
        System.out.println("Enter your message: ");
        while (true) {
            String user_message = scan.nextLine();
            String[] parsed_message = user_message.split(" ");

            for (int i = 0; i < parsed_message.length; i++) {
                switch (parsed_message[0]) {
                    case "/message": { // this should have /message /<username> hello
                        if (parsed_message.length > 1 && parsed_message[1].contains("/")) {
                            send_message(parsed_message[1].substring(1), String.join(" ", Arrays.copyOfRange(parsed_message, 2, parsed_message.length)));
                            i = parsed_message.length + 1;
                        } else {
                            System.out.println("Usage: /message /<username> <message>");
                            i = parsed_message.length + 1; // exit out
                        }
                        break;
                    }
                    case "/newUser": {

                        break;
                    }
                    case "/exit": {
                        EXIT = true;
                        // TODO: closing all the socket properly before exiting
                        System.exit(0);
                    }
                }
            }

        }
    }
}
