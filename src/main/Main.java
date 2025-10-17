package main;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import database.*;
import crypto.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author theunknown
 */
public class Main {

    Boolean debug = false;

    String PREFIX_CONNECT = "CONNECT:";
    String PREFIX_REQUEST_INFORMATION = "REQUEST:";
    String PREFIX_REPLY_INFORMATION = "REPLY:";

    protected static Boolean EXIT = false;

    // String Key = md5 + ":" + peerIp +":"+ peerPort;
    protected static ConcurrentHashMap<String, Socket> clients = new ConcurrentHashMap<>();

    // list of clients who couldn't connect. We wait for them
    protected static ConcurrentHashMap<String, Long> lastConnectAttempt = new ConcurrentHashMap<>();
    private static final long CONNECT_COOLDOWN_MS = 10000; // 10s cooldown

    private UserInfo user = null;

    private String Broadcast_id;
    private int udp_port;

    private final int port = 9332;
    private final int portAsk = port +1;

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
        Query query = new Query(this.debug);

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
            query.closeConnection();
            return false;
        }

        System.out.println("Input your password: ");
        String password = scan.nextLine();

        boolean verification = new CryptoPassword(this.debug).verifyPassword(user, password);
        query.closeConnection();
        return verification;
    }

    /**
     * receives Key and accepts the connection if not exist
     *
     * @param received
     */
    private void sendConnectionRequest(String received) { // client side socket
        ConnectionKey connectionKey = new ConnectionKey(received, PREFIX_CONNECT);
        // ignore if invalid data
        if (connectionKey.ip == null) {
            return;
        }
        // ignore if already connected
        if (clients.containsKey(connectionKey.toString())) {
            return;
        }
        // ignore if not in our database
        if (falseConnection(connectionKey)) {
            lastConnectAttempt.putIfAbsent(connectionKey.toString(), System.currentTimeMillis());
            if(debug) lastConnectAttempt.forEach((key, value) ->{
                System.out.println(key);
            });
            return;
        }

        int peerServerPort = Integer.parseInt(connectionKey.port);
        
        System.out.println("send Connection request to(sendConnectionRequest): "+ connectionKey.toString());

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
                    clients.putIfAbsent(connectionKey.toString(), host);
                    if (debug) {
                        System.out.println("Addded key (sendConnectionRequest): " + connectionKey);
                    }
                    // listen to message from the thread
                    new Thread(new get_message(host, connectionKey.md5)).start();

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
     * returns true if there is a false connection.
     *
     * @param key
     * @return
     */
    private boolean falseConnection(ConnectionKey connectionKey) {
        Query query = new Query(this.debug);
        if (!query.hasSender(connectionKey.md5)) {
            query.closeConnection();
            return true;
        }
        query.closeConnection();
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

    private void newSenderInPermission(Scanner scan) {
        System.out.println("Enter y or n: ");
        try {
            ServerSocket askServer = new ServerSocket(portAsk);
            Socket socket = askServer.accept();
            
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            String peerIdentity = in.readLine(); 

            if (peerIdentity.startsWith(PREFIX_REQUEST_INFORMATION)) {
                RequestInformation request = new RequestInformation(peerIdentity, PREFIX_REQUEST_INFORMATION);
                System.out.println("Request by: " + request.username); // TODO: some abstraction/polymorphism?
                try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)){
                    String choice = scan.nextLine().toLowerCase();
                    if (!(choice.equals("y") || choice.equals("yes"))) {
                        writer.println("REJECTED:");
                    } else {
                        String accepted = PREFIX_REPLY_INFORMATION + user.username + ":" + CryptoRSA.publicKeyToString(user.getPublicKey());
                        writer.println(accepted);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            in.close();
            socket.close();
        }catch(ConnectException e){
            System.out.println("Connection refused: make /openSender");
        } 
        catch (IOException ex) {
            
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
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
                        if (clients.containsKey(connectionKey.toString())) {
                            if (debug) {
                                System.out.println("Duplicate incoming connection for " + connectionKey + ", closing");
                            }
                            incoming.close();
                            continue;
                        }
                        // allow connection with known user only
                        if (falseConnection(connectionKey)) {
                            System.out.println("connection closed for (server): " + peerIdentity);
                            lastConnectAttempt.putIfAbsent(connectionKey.toString(), System.currentTimeMillis());
                            incoming.close();
                            continue;
                        }

                        if (debug) {
                            System.out.println("key server: " + connectionKey);
                        }

                        // otherwise register and start handler
                        clients.put(connectionKey.toString(), incoming);

                        if (!debug) {
                            System.out.println("Accepted Request: (serverThread)" + getLocalIp());
                            System.out.println("Added Key (ServerThread): " + connectionKey);
                        }
                        new Thread(new get_message(incoming, connectionKey.md5)).start();
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

            if (!debug) {
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

        System.out.println("send_message: " + userkey);
        System.out.println("send_message: " + connectionKey);

        if (!clients.containsKey(connectionKey.toString())) {
            System.err.println("User does not exist");
            return;
        }

        Socket activeSocket = clients.get(connectionKey.toString());

        try {
            PrintWriter writer = new PrintWriter(activeSocket.getOutputStream(), true);
            writer.println(message);
        } catch (IOException e) {
            System.err.println("Could not create a new reader and writer to send message: " + e.getMessage());
        }
    }

    protected void connectNewSender() {
        long now = System.currentTimeMillis();
        lastConnectAttempt.entrySet().removeIf(
                (entry) -> entry.getValue() < (now - CONNECT_COOLDOWN_MS)
        );
        final List<String> availableKeys = new ArrayList<>();

        // Show only entries within cooldown period
        lastConnectAttempt.forEach((key, value) -> {
            if (value >= (now - CONNECT_COOLDOWN_MS)) {
                System.out.println(availableKeys.size() + ": " + key);
                availableKeys.add(key);
            }
        });

        if (availableKeys.isEmpty()) {
            System.out.println("No active connections in the last " + (CONNECT_COOLDOWN_MS / 1000) + " seconds");
            return;
        }

        System.out.print("Input the number to connect the Sender: ");
        Scanner scan = new Scanner(System.in);
        int choice = scan.nextInt();

        if (choice < 0 || choice >= availableKeys.size()) {
            System.out.println("Invalid choice.");
            return;
        }
        String chosenKey = availableKeys.get(choice);
        getSenderInfo(chosenKey);
    }

    protected void getSenderInfo(String stringKey) {
        ConnectionKey senderConnectionKey = new ConnectionKey(stringKey, null);
        int peerServerPort = Integer.parseInt(senderConnectionKey.port)+1; // convention that the ask port will be +1 of the main server port
        System.out.println("getSenderInfo: " + senderConnectionKey);
        Socket host = null;
        try {
            host = new Socket();
            host.connect(new InetSocketAddress(senderConnectionKey.ip, peerServerPort), (int) CONNECT_COOLDOWN_MS);
            //TODO: not let users have ':' in their name
            String requestKey = PREFIX_REQUEST_INFORMATION + user.username + ":" + CryptoRSA.publicKeyToString(user.getPublicKey()) + ":" + getLocalIp() + ":" + this.port;

            PrintWriter writer = new PrintWriter(host.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(host.getInputStream()));
            writer.println(requestKey);

            if (!debug) {
                System.out.println("Connection request sent to: (getSenderInfo) " + senderConnectionKey.ip);
            }

            if (host.isConnected()) {
                String userInfoString = reader.readLine();

                if (userInfoString.equals("REJECTED:")) {
                    System.out.println("request rejected");
                    return;
                }
                NewConnection newConnection = new NewConnection(userInfoString, PREFIX_REPLY_INFORMATION);
                System.out.println("Reply: " + newConnection);
                Query query = new Query(this.debug);
                query.saveNewSender(new SenderInfo(newConnection.username, newConnection.publicKey));
                query.closeConnection();
            }
        } catch (SocketTimeoutException ex) {
            System.err.println("Did not respond");
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
                    case "/message": { // this should have /message /<md5> hello
                        if (parsed_message.length > 1 && parsed_message[1].contains("/")) {
                            messenger.send_message(parsed_message[1].substring(1), String.join(" ", Arrays.copyOfRange(parsed_message, 2, parsed_message.length)));
                            i = parsed_message.length + 1;
                        } else {
                            System.out.println("Usage: /message /<username> <message>");
                            i = parsed_message.length + 1; // exit out
                        }
                        break;
                    }
                    case "/newSender": {
                        messenger.connectNewSender();
                        break;
                    }
                    case "/openSender":{
                        messenger.newSenderInPermission(scan);
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
