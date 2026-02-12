package main;

import frontend.MessageManager;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import database.*;
import crypto.*;
import frontend.FrameUi;
import frontend.LandingUi;
import java.security.PrivateKey;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author theunknown
 */
// TODO: improve adding new users and such.
public class Main {

    Boolean debug = false;
    
    //TODO: make some enums
    public static final String PREFIX_CONNECT = "CONNECT:";
    public static final String PREFIX_REQUEST_INFORMATION = "REQUEST:";
    public static final String PREFIX_REPLY_INFORMATION = "REPLY:";

    protected static Boolean EXIT = false;

    // String Key = md5 + ":" + peerIp +":"+ peerPort;
    public static ConcurrentHashMap<String, Socket> clients = new ConcurrentHashMap<String, Socket>();

    // list of clients who aren't in our database
    protected static ConcurrentHashMap<String, Long> unknownConnection = new ConcurrentHashMap<>();
    public static ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<Message>();
    protected static final long CONNECT_COOLDOWN_MS = 30000; // 30s cooldown
    // TODO: make ports increment if port buisy
    protected static final int port = 9332;
    protected static final int portAsk = port + 1; // convention?

    FrameUi gui = null;
    public UserInfo user = null;

    protected String Broadcast_id;
    protected int udp_port;

    public Main(String BroadcastID, int udp_port) {
        this.Broadcast_id = BroadcastID;
        this.udp_port = udp_port;
    }

    public void start() {

        getCurrentUser();
        this.gui = new FrameUi(this.user);
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
    private void getCurrentUser() {
        LandingUi landingUi = new LandingUi(null);
        this.user = landingUi.showDialog();
        if (this.user == null) {
            System.exit(0);
        }
    }

    /**
     * receives Key and accepts the connection if not exist
     *
     * @param received
     */
    private void sendConnectionRequest(String received) { // client side socket
        System.out.println("send Connection Request: " + received);;
        ConnectionKey connectionKey = new ConnectionKey(received, PREFIX_CONNECT);
        System.out.println("connectionKey: " + connectionKey);
        // ignore if invalid data
        if (connectionKey.ip == null) {
            return;
        }
        // ignore if Senders are not known
        if (unknownConnection.containsKey(connectionKey)) {
            return;
        }
        // ignore if already connected
        if (clients.containsKey(connectionKey.md5)) {
            return;
        }
        // ignore if not in our database
        if (falseConnection(connectionKey)) {
            unknownConnection.putIfAbsent(connectionKey.toString(), System.currentTimeMillis());
            if (debug) {
                unknownConnection.forEach((key, value) -> {
                    System.out.println(key);
                });
            }
            return;
        }

        if (connectionKey.port.equals("") || connectionKey.port == null) {
            System.out.println("it's null sendConnectionRequest()");
        }
        int peerServerPort = Integer.parseInt(connectionKey.port);

        if (debug) {
            System.out.println("send Connection request to(sendConnectionRequest): " + connectionKey.toString());
        }

        new Thread(() -> {
            Socket host = null;
            try {
                host = new Socket();
                host.connect(new InetSocketAddress(connectionKey.ip, peerServerPort));

                String requestKey = PREFIX_CONNECT + user.getMd5() + ":" + getLocalIp() + ":" + this.port;

                PrintWriter writer = new PrintWriter(host.getOutputStream(), true);
                writer.println(requestKey);

                if (debug) {
                    System.out.println("Connection request sent to: (sendConnectionRequest) " + connectionKey.ip);
                }

                if (host.isConnected()) {

                    clients.putIfAbsent(connectionKey.md5, host);
                    if (debug) {
                        System.out.println("Addded key (sendConnectionRequest): " + connectionKey);
                    }

                    Query query = new Query(debug);
                    SenderInfo sender = query.getSender(connectionKey.md5);
                    query.closeConnection();

                    // listen to message from the thread
                    new Thread(new get_message(host, sender, this.debug)).start();

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
     * returns true if there Sender is not known in our database.
     *
     * @param key
     * @return
     */
    private boolean falseConnection(ConnectionKey connectionKey) {
        // TODO: sometimes, this shows false positive so some connection loops in connecting and disconnecting
        Query query = new Query(this.debug);
        if (!query.hasCommunication(user.getId(), connectionKey.md5)) {
            query.closeConnection();
            return true;
        }
        query.closeConnection();
        return false;
    }

    /**
     * gets local IP
     *
     * @return IP
     */
    public static String getLocalIp() {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress().toString();
        } catch (Exception e) {
        }
        return "127.0.0.1";
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
            System.out.println("Your fingerpring: " + user.getMd5());
            System.out.println("your port: " + this.port);
        }

        // TCP server thread (accept incoming connections)
        new Thread(() -> { // server side socket
            while (true) {
                try {
                    Socket incoming = ser.accept();

                    BufferedReader in = new BufferedReader(new InputStreamReader(incoming.getInputStream()));

//                    incoming.setSoTimeout(5000);
                    String peerIdentity = in.readLine(); //PREFIX:md5:ip:port
                    if (peerIdentity.startsWith(PREFIX_CONNECT)) {
                        ConnectionKey connectionKey = new ConnectionKey(peerIdentity, PREFIX_CONNECT);
                        // if connection was invalid empty
                        if (connectionKey.ip == null) {
                            incoming.close();
                            continue;
                        }
                        // if connection were us that probably won't happen now
                        if (connectionKey.md5.equals(user.getMd5()) && connectionKey.ip.equals(getLocalIp())) {
                            incoming.close();
                            continue;
                        }

                        // if already connected, close duplicate incoming connection
                        if (clients.containsKey(connectionKey.md5)) {
                            if (debug) {
                                System.out.println("Duplicate incoming connection for " + connectionKey + ", closing");
                            }
                            incoming.close();
                            continue;
                        }
                        // allow connection with known user only
                        if (falseConnection(connectionKey)) {
                            System.out.println("connection closed for (server): " + peerIdentity);
                            unknownConnection.putIfAbsent(connectionKey.toString(), System.currentTimeMillis());
                            incoming.close();
                            continue;
                        }

                        if (debug) {
                            System.out.println("key server: " + connectionKey);
                        }

                        Query query = new Query(debug);
                        SenderInfo sender = query.getSender(connectionKey.md5);
                        query.closeConnection();

                        // otherwise register and start handler
                        clients.putIfAbsent(connectionKey.md5, incoming);

                        if (!debug) {
                            System.out.println("Accepted Request: (serverThread)" + getLocalIp());
                            System.out.println("Added Key (ServerThread): " + connectionKey);
                        }
                        new Thread(new get_message(incoming, sender, this.debug)).start();
                    }

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
            String message = PREFIX_CONNECT + user.getMd5() + ":" + getLocalIp() + ":" + this.port;

            if (true) {
                System.out.println("broadcasting this message in bytes: " + message);
            }

            byte[] messageByte = message.getBytes();
            DatagramSocket dSocket = null;

            try {
                dSocket = new DatagramSocket(this.udp_port+1,InetAddress.getByName(getLocalIp()));
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
                    System.out.println("packets sent: " + message);
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
                    if (!received.equals(PREFIX_CONNECT + user.getMd5() + ":" + getLocalIp() + ":" + this.port)) {
                        sendConnectionRequest(received);
                        System.out.println("recieved packates :" + received);
                    }

                    if (EXIT) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error receiving UDP packets: " + e.getMessage());
            }
        }).start();
    }

    protected void send_message(String userkey, String message) {
        ConnectionKey connectionKey = new ConnectionKey(userkey, null);
        // TODO: properly handel the END_MESSAGE
        final String END_MESSAGE = ":END_OF_MESSAGE:";

        message.replace("\n", "//n");

        System.out.println("send_message: " + connectionKey);

        if (!clients.containsKey(connectionKey.toString())) {
            System.err.println("User does not exist");
            return;
        }

        Socket activeSocket = clients.get(connectionKey.toString());

        try {
            PrintWriter writer = new PrintWriter(activeSocket.getOutputStream(), true);
            writer.println(message);
            writer.println(END_MESSAGE);
        } catch (IOException e) {
            System.err.println("Could not create a new reader and writer to send message: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {

        Main messenger = new Main("255.255.255.255", 64444);

        Scanner scan = new Scanner(System.in);

        messenger.start();
        // TODO: FrameUi gui = new FrameUi(Bridge bridge);?
        System.out.println("Enter your message: ");
        while (true) {
            String user_message = scan.nextLine();
            String[] parsed_message = user_message.split(" ");

            for (int i = 0; i < parsed_message.length; i++) {
                switch (parsed_message[0]) {
                    case "/message": { // this should have /message /<md5> hello
                        if (parsed_message.length > 1 && parsed_message[1].contains("/")) {
                            messenger.send_message(parsed_message[1].substring(1), String.join(" ", Arrays.copyOfRange(parsed_message, 2, parsed_message.length)));
                            i = parsed_message.length + 1;
                        } else {
                            System.out.println("Usage: /message /<username> <message>");
                            i = parsed_message.length + 1;
                        }
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
