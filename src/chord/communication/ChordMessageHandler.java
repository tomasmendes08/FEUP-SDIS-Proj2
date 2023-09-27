package chord.communication;

import chord.ChordInfo;
import chord.ChordNode;
import communication.Sender;
import peers.Peer;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.Collections;

public class ChordMessageHandler implements Runnable {
    private final ChordMessage received;
    private SSLSocket clientSocket;

    public ChordMessageHandler(ChordMessage received, SSLSocket clientSocket) throws IOException, ClassNotFoundException {
        this.received = received;
        this.clientSocket = clientSocket;
    }


    @Override
    public void run() {
        if(received.getMsgBody() != null) {
            String[] messageArray = received.getMsgBody().split(" ");
            //System.out.println("\nMessage Received: " + received.getMsgBody());
            switch (messageArray[0]) {
                case "JOIN":
                    if(messageArray[1].equals("CHORD")) {
                        try {
                            handleJoin();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else if(messageArray[1].equals("ACCEPTED"))
                        handleJoinAccepted(messageArray);
                    break;
                case "LOOKUP":
                    try {
                        handleLookup(messageArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "LOOKUPREPLY":
                    try {
                        handleLookupReply(messageArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "GETPREDECESSOR":
                    try {
                        handleGetPredecessor(messageArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "GETPREDECESSORREPLY":
                    try {
                        handleGetPredecessorReply(messageArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "UPDATEPREDECESSOR":
                    handleUpdatePredecessor(messageArray);
                    break;
                case "CHECKPREDECESSOR":
                    try {
                        handleCheckPredecessor(messageArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "ONLINE":
                    break;
                case "CHORDDISCONNECT":
                    try {
                        handleDisconnect(messageArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + messageArray[0]);
            }
        }
    }

    private void handleDisconnect(String[] message) throws IOException {
        long key = Long.parseLong(message[1]);
        long newID;
        InetSocketAddress newAddress;

        if (ChordNode.predecessor != null && key == ChordNode.predecessor.getChordID()) {
            if (Long.parseLong(message[2]) != 0) {
                newID = Long.parseLong(message[2]);
                newAddress = new InetSocketAddress(message[3], Integer.parseInt(message[4]));
                ChordNode.setPredecessor(new ChordInfo(newID, newAddress));
            }
            else ChordNode.setPredecessor(null);
        }

        if (ChordNode.successor != null && key == ChordNode.successor.getChordID()) {
            if (Long.parseLong(message[5]) != 0) {
                newID = Long.parseLong(message[5]);
                newAddress = new InetSocketAddress(message[6], Integer.parseInt(message[7]));
                ChordNode.setSuccessor(new ChordInfo(newID, newAddress));
            }
            else ChordNode.setSuccessor(null);
        }
        //clientSocket.close();
    }

    private void handleCheckPredecessor(String[] message) throws IOException {
        long key = Long.parseLong(message[1]);
        if (key == ChordNode.self.getChordID()) {
            ChordMessage msg = new PredecessorOnline(ChordNode.getPeerAddress(), key);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.writeObject(msg);
        }
        //clientSocket.close();
    }

    private void handleUpdatePredecessor(String[] message) {
        long key = Long.parseLong(message[1]);
        InetSocketAddress successorSocket = new InetSocketAddress(message[2], Integer.parseInt(message[3]));
        ChordNode.setPredecessor(new ChordInfo(key, successorSocket));
        //clientSocket.close();
    }

    private void handleGetPredecessor(String[] message) throws IOException {
        long key = Long.parseLong(message[1]);
        ChordMessage msg = new GetPredecessorReply(ChordNode.getPeerAddress(), key, ChordNode.predecessor);
        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
        out.writeObject(msg);
    }

    private void handleGetPredecessorReply(String[] message) throws IOException {
        long key = Long.parseLong(message[2]);
        if (ChordNode.predecessor == null && key != 0 && message[3] != null && message[4] != null) {
            String address = message[3];
            int port = Integer.parseInt(message[4]);
            ChordInfo predecessor = new ChordInfo(key, new InetSocketAddress(address, port));
            ChordNode.setPredecessor(predecessor);
        }
    }

    private void handleLookup(String[] message) throws IOException {
        long key = Long.parseLong(message[1]);
        ChordInfo successor = ChordNode.findSuccessor(key, received.getPeerAddress());
        ChordMessage msg = new LookupReply(new InetSocketAddress(successor.getSocketAddress().getAddress(), successor.getSocketAddress().getPort()), key, successor.getChordID(), successor);
        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
        out.writeObject(msg);
    }

    private void handleLookupReply(String[] message) throws IOException {
        long key = Long.parseLong(message[1]);
        InetSocketAddress socketAddressSuccessor = new InetSocketAddress(message[3], Integer.parseInt(message[4]));
        ChordInfo successor = new ChordInfo(Long.parseLong(message[2]), socketAddressSuccessor);
        ChordMessage msg = null;
        msg = new LookupReply(received.getPeerAddress(), key, successor.getChordID(), successor);

        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
        out.writeObject(msg);
        System.out.println("New Peer Joined: " + key);
        /*ChordInfo successor = ChordNode.actualChord.getNodeRef().findSuccessor(key);
        System.out.println("successor is " + successor.getChordID());
        ChordMessage msg = new LookupReply(new InetSocketAddress(clientSocket.getInetAddress(), clientSocket.getPort()), key);
        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
        out.writeObject(msg);*/
    }

    private void handleJoinAccepted(String[] message) {
        long chordID = Long.parseLong(message[2]);
        ChordNode.self.setChordID(chordID);
        InetSocketAddress socketAddressSuccessor = new InetSocketAddress(message[4], Integer.parseInt(message[5]));
        ChordInfo successor = new ChordInfo(Long.parseLong(message[3]), socketAddressSuccessor);
        System.out.println("Chord ID: " + ChordNode.self.getChordID());
        System.out.println("Successor ID: " + successor.getChordID());
        ChordNode.fingerTable[0] = successor;
        ChordNode.setSuccessor(successor);
    }

    private void handleJoin() throws IOException {
        InetSocketAddress clientSocketAddress = received.getPeerAddress();
        long key = ChordNode.generateChordID(received.getPeerAddress());
        //System.out.println(ChordNode.fingerTable.length);
        ChordInfo successor = ChordNode.findSuccessor(key, ChordNode.getPeerAddress());
        //ChordNode.self.getNodeRef().updateFingerTable(new ChordInfo(key, clientSocketAddress));
        //successor.setSocketAddress(new InetSocketAddress(successor.getSocketAddress().getHostString(), successor.getSocketAddress().getPort()));

        JoinChordReply msg = new JoinChordReply(received.getPeerAddress(), key, successor);

        if (ChordNode.successor.getChordID() == ChordNode.self.getChordID()) {
            ChordNode.setSuccessor(new ChordInfo(msg.getAssignedId(), msg.getPeerAddress()));
        }
        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
        out.writeObject(msg);
        System.out.println("New Peer Joined: " + key);
    }
}
