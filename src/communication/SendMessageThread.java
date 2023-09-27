package communication;

import chord.ChordNode;
import communication.protocols.ProtocolMessage;
import peers.Peer;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;

public class SendMessageThread implements Runnable {
    private final ProtocolMessage msg;
    private final SSLSocket originalSocket;
    private final InetSocketAddress clientAddress;

    public SendMessageThread(ProtocolMessage msg, SSLSocket originalSocket, InetSocketAddress clientAddress) {
        this.msg = msg;
        this.originalSocket = originalSocket;
        this.clientAddress = clientAddress;
    }

    @Override
    public void run() {
        MessageSender sender = new MessageSender(clientAddress, msg);
        SSLSocket clientSocket = sender.sendFile();
        if (clientSocket != null) {
            ProtocolMessage response = null;
            try {
                response = (ProtocolMessage) ChordNode.getServerSocket().getServerResponse(clientSocket);
                Peer.getExec().execute(new MessageHandler(response, originalSocket));
                //clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
