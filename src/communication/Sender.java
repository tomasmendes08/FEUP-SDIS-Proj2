package communication;

import chord.ChordNode;
import chord.communication.CheckPredecessor;
import chord.communication.ChordMessage;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Sender {
    private InetSocketAddress socketAddress;
    private ChordMessage msg;

    public Sender(InetSocketAddress socket, ChordMessage msg) {
        this.socketAddress = socket;
        this.msg = msg;
    }

    public SSLSocket sendMessage() {
        // Truststore
        System.setProperty("javax.net.ssl.trustStore", "truststore");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");

        // Keystore
        System.setProperty("javax.net.ssl.keyStore", "clientKeyStore");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket clientSocket = (SSLSocket) factory.createSocket(socketAddress.getAddress(), socketAddress.getPort());
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.writeObject(msg);
            return clientSocket;
        } catch (IOException e) {
            if (msg instanceof CheckPredecessor) {
                System.out.println("Predecessor disconnected");
                if (ChordNode.predecessor == ChordNode.successor)
                    ChordNode.setSuccessor(null);
                ChordNode.setPredecessor(null);
            }
        }
        return null;
    }
}
