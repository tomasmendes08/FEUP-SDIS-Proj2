package communication;

import communication.protocols.ProtocolMessage;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;

public class MessageSender {
    private InetSocketAddress socketAddress;
    private ProtocolMessage msg;

    public MessageSender(InetSocketAddress socket, ProtocolMessage msg) {
        this.socketAddress = socket;
        this.msg = msg;
    }

    public SSLSocket sendFile() {
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
            System.out.println("\nUnable to contact peer with socket address " + msg.getPeerAddress());
        }
        return null;
    }
}
