package communication;

import chord.communication.ChordMessage;
import communication.Receiver;
import communication.protocols.ProtocolMessage;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerSocket {
    protected InetSocketAddress address;
    protected SSLServerSocket socket;
    protected Receiver receiver;
    public static ExecutorService exec = Executors.newFixedThreadPool(64);

    public ServerSocket(InetSocketAddress address) throws IOException {
        this.address = address;

        // Truststore
        System.setProperty("javax.net.ssl.trustStore", "keys/truststore");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");

        // Keystore
        System.setProperty("javax.net.ssl.keyStore", "keys/serverKeyStore");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try {
            socket = (SSLServerSocket) ssf.createServerSocket(address.getPort());
        }
        catch(IOException e) {
            System.out.println("Server - Failed to create SSLServerSocket");
            e.printStackTrace();
        }

        receiver = new Receiver(socket, address);
        exec.execute(receiver);

    }

    public SSLServerSocket getSocket() {
        return socket;
    }

    public Message getServerResponse(SSLSocket socket) throws IOException {
        ObjectInputStream inputStream;
        Message msg = null;

        try {
            inputStream = new ObjectInputStream(socket.getInputStream());
            msg = (Message) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Peer disconnected...");
        }

        return msg;
    }
}
