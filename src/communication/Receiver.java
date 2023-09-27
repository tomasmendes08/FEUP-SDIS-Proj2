package communication;

import chord.ChordNode;
import chord.communication.ChordMessage;
import chord.communication.ChordMessageHandler;
import chord.communication.JoinChord;
import chord.communication.JoinChordReply;
import communication.protocols.ProtocolMessage;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.InetSocketAddress;

public class Receiver implements Runnable {
    private SSLServerSocket serverSocket;
    private InetSocketAddress socketAddress;

    public Receiver(SSLServerSocket socket, InetSocketAddress address) {
        this.serverSocket = socket;
        this.socketAddress = address;
    }

    @Override
    public void run() {
        while(true) {
            try {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                Message receivedMessage = (Message) in.readObject();

                if (receivedMessage instanceof ChordMessage) {
                    ChordMessageHandler messageHandler = new ChordMessageHandler((ChordMessage) receivedMessage, clientSocket);
                    ChordNode.exec.execute(messageHandler);
                }
                else if (receivedMessage instanceof ProtocolMessage) {
                    MessageHandler messageHandler = new MessageHandler((ProtocolMessage) receivedMessage, clientSocket);
                    ChordNode.exec.execute(messageHandler);
                }

            } catch (IOException | ClassNotFoundException e) {
                //e.printStackTrace();
            }
        }
    }
}
