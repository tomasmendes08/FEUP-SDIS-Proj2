package chord;

import chord.communication.*;
import communication.Sender;
import communication.ServerSocket;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class ChordNode implements Serializable {
    private static final int KEYSIZE = 8;

    public static ChordInfo self;
    protected static ServerSocket serverSocket;
    public static ChordInfo predecessor;
    public static ChordInfo successor;
    public static InetSocketAddress peerAddress;
    private static InetSocketAddress bootPeerAddress;
    private static boolean isBootPeer = false;
    public static ChordInfo[] fingerTable = new ChordInfo[ChordNode.KEYSIZE];
    public static int fingerID = 1;
    public static ExecutorService exec = Executors.newFixedThreadPool(256);
    public static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public ChordNode(InetSocketAddress address, InetSocketAddress bootPeerAddress) throws IOException {
        peerAddress = address;
        ChordNode.bootPeerAddress = bootPeerAddress;
        serverSocket = new ServerSocket(address);

        self = new ChordInfo(-1, peerAddress);
        self.setNodeRef(this);
        successor = self;

        if (address.equals(bootPeerAddress))
            isBootPeer = true;
    }

    public static long getChordID() {
        return self.getChordID();
    }

    public ChordInfo getSelf() {
        return self;
    }

    public static ServerSocket getServerSocket() {
        return serverSocket;
    }

    public static InetSocketAddress getPeerAddress() {
        return peerAddress;
    }

    public static InetSocketAddress getBootPeerAddress() {
        return bootPeerAddress;
    }

    public static boolean getisBootPeer() {
        return isBootPeer;
    }

    public static ChordInfo[] getFingerTable() {
        return fingerTable;
    }

    public static ChordInfo getSuccessor() {
        return successor;
    }

    public static void setSuccessor(ChordInfo successor) {
        ChordNode.successor = successor;
        if (successor != null)
            ChordNode.fingerTable[0] = successor;
        else {
            ChordNode.successor = self;
            ChordNode.fingerTable[0] = self;
        }
    }

    public static void setPredecessor(ChordInfo predecessor) {
        ChordNode.predecessor = predecessor;
    }

    public void create(){
        predecessor = null;
        self.setChordID(generateChordID(peerAddress));
        fingerTable[0] = self;
    }

    public void join() throws IOException {
        if (isBootPeer) {
            this.create();
            //System.out.println("Boot Peer added to Chord with GUID: " + self.getChordID());
            return;
        }
        System.out.println("Joining Chord on: " + bootPeerAddress);
        ChordMessage msg = new JoinChord(peerAddress);
        //System.out.println(msg.getMsgBody());
        sendMessage(bootPeerAddress, msg);
        /*ChordMessage lookupMsg = new Lookup(peerAddress);
        sendMessage(this.bootPeerAddress, lookupMsg);
        System.out.println(lookupMsg.getMsgBody());*/
    }

    public void stabilize() {
        System.out.println("\nMy Chord ID is " + self.getChordID());
        System.out.println("My successor is " + successor.getChordID());
        if (predecessor != null)
            System.out.println("My predecessor is " + predecessor.getChordID());
        else
            System.out.println("My predecessor is " + null);
        if (successor.getChordID() == self.getChordID())
            return;
        //System.out.println("Stabilizing Peer...");

        String[] messageArray = this.getPredecessorFromSuccessor();
        long id = Long.parseLong(messageArray[2]);

        if (id != 0) {
            if(isChordIDBetween(self.getChordID(), id, successor.getChordID())) {
                InetSocketAddress socketAddressSuccessor = new InetSocketAddress(messageArray[3], Integer.parseInt(messageArray[4]));
                ChordNode.setSuccessor(new ChordInfo(id, socketAddressSuccessor));
            }
        }

        if (id != self.getChordID() || id == 0)
            this.notify(successor, self);
    }

    public void notify(ChordInfo successor, ChordInfo self) {
        ChordMessage msg = new UpdatePredecessor(peerAddress, self.getChordID());
        Sender sender = new Sender(successor.getSocketAddress(), msg);
        sender.sendMessage();
    }

    public String[] getPredecessorFromSuccessor() {
        if (successor != null) {
            try {
                ChordMessage msg = new GetPredecessor(peerAddress, successor.getChordID());
                ChordMessage response = sendMessage(successor.getSocketAddress(), msg);
                return response.getMsgBody().split(" ");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new String[0];
    }

    public void fixFingers() {
        try {
            long id = self.getChordID() + (int) Math.pow(2, fingerID - 1);
            id = (long) (id % Math.pow(2, ChordNode.KEYSIZE));
            ChordInfo finger = findSuccessor(id, ChordNode.getPeerAddress());
            this.updateFinger(fingerID - 1, finger);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateFinger(int index, ChordInfo reference) {
        fingerTable[index] = reference;
        fingerID++;
        fingerID = fingerID % ChordNode.KEYSIZE;
    }

    public void checkPredecessor() {
        System.out.println("\nChecking predecessor");
        if (predecessor != null) {
            ChordMessage msg = new CheckPredecessor(peerAddress, predecessor.getChordID());
            Sender sender = new Sender(predecessor.getSocketAddress(), msg);
            SSLSocket clientSocket = sender.sendMessage();
            if (clientSocket != null) {
                try {
                    ChordMessage response = (ChordMessage) serverSocket.getServerResponse(clientSocket);
                    exec.execute(new ChordMessageHandler(response, clientSocket));
                } catch (IOException | ClassNotFoundException ignored) {
                }
            } else {
                System.out.println("Predecessor is null");
            }
        }
    }

    public static ChordInfo findSuccessor(long guid, InetSocketAddress address) throws IOException {
        if(isChordIDBetween(self.getChordID(), guid, fingerTable[0].getChordID())) {
            return fingerTable[0];
        }
        else{
            ChordMessage message = null;
            while (message == null) {
                ChordInfo chordInfo = ChordNode.closestPrecedingNode(guid);

                ChordMessage lookupMsg = new Lookup(chordInfo.getSocketAddress(), guid);

                Sender sender = new Sender(chordInfo.getSocketAddress(), lookupMsg);
                SSLSocket clientSocket = sender.sendMessage();
                message = (ChordMessage) serverSocket.getServerResponse(clientSocket);
                if (message != null) {
                    List<String> msgBody = Arrays.asList(message.getMsgBody().split(" "));
                    chordInfo = new ChordInfo(Long.parseLong(msgBody.get(2)), message.getPeerAddress());
                    return chordInfo;
                }
            }
        }
        return null;
    }

    public static ChordInfo closestPrecedingNode(long guid){
        for (int i = fingerTable.length-1; i >= 0; i--) {
            if (fingerTable[i] != null) {
                if (isChordIDBetween(self.getChordID(), fingerTable[i].getChordID(), guid)) {
                    return fingerTable[i];
                }
            }
        }

        return self;
    }

    public static boolean isChordIDBetween(long lowerID, long guid, long higherID){
        if(higherID > lowerID) return guid < higherID && guid > lowerID;
        else return guid < higherID || guid > lowerID;
    }

    public void updateFingerTable(ChordInfo chordInfo){
        for (int i = fingerTable.length-1; i >= 0; i--){
            if (checkIfNodeIsCloser(i, chordInfo.getChordID())) fingerTable[i] = chordInfo;
        }
    }

    public long calculateDistance(long first, long second){
        if(second < first) return (long) (second - first + (Math.pow(2, ChordNode.KEYSIZE)));
        else return second - first;
    }

    public boolean checkIfNodeIsCloser(int fingerTablePlace, long guid){
        if(fingerTable[fingerTablePlace] == null) return true;

        long fingerGuid = (long) ((self.getChordID() + Math.pow(2, fingerTablePlace)) % Math.pow(2,ChordNode.KEYSIZE));
        long aux1 = calculateDistance(fingerGuid, guid);
        long aux2 = calculateDistance(fingerGuid, fingerTable[fingerTablePlace].getChordID());
        //System.out.println("Aux1: " + aux1 + "\n" + "Aux2: " + aux2 + "\n");
        return aux1 < aux2;
    }


    public static ChordMessage sendMessage(InetSocketAddress socket, ChordMessage msg) throws IOException {
        if (successor != null) {
            Sender sender = new Sender(socket, msg);
            SSLSocket clientSocket = sender.sendMessage();
            if (clientSocket != null) {
                ChordMessage response = (ChordMessage) serverSocket.getServerResponse(clientSocket);
                try {
                    exec.execute(new ChordMessageHandler(response, clientSocket));
                    return response;
                } catch (ClassNotFoundException e) {
                    if (!(msg instanceof CheckPredecessor))
                        e.printStackTrace();
                }
                //clientSocket.close();
            }
        }
        return null;
    }


    public static long convertByteArrayToInt(byte[] bytes) {
        return Math.abs(((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                ((bytes[3] & 0xFF)));
    }

    public static long generateChordID(InetSocketAddress socketAddress) {
        String data = socketAddress.getAddress() + ":" + socketAddress.getPort();
        long key = -1;
        try {
            MessageDigest encrypt = MessageDigest.getInstance("SHA-1");
            byte[] hashEnc = encrypt.digest(data.getBytes(StandardCharsets.UTF_8));
            long aux = convertByteArrayToInt(hashEnc);
            key = (long) (aux % (Math.pow(2, ChordNode.KEYSIZE)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return key;
    }

    public static void leave() {
        long predecessorID = 0, successorID = 0;
        InetSocketAddress predecessorSocketAddress = null, successorSocketAddress = null;

        if (predecessor != null) {
            predecessorID = predecessor.getChordID();
            predecessorSocketAddress = predecessor.getSocketAddress();
        }

        if (successor != null) {
            successorID = successor.getChordID();
            successorSocketAddress = successor.getSocketAddress();
        }

        ChordMessage msg = new ChordDisconnect(peerAddress, self.getChordID(), predecessorID, predecessorSocketAddress, successorID, successorSocketAddress);
        Sender senderSuccessor = new Sender(successor.getSocketAddress(), msg);
        senderSuccessor.sendMessage();

        if (predecessor != null) {
            Sender senderPredecessor = new Sender(predecessor.getSocketAddress(), msg);
            senderPredecessor.sendMessage();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ChordNode) {
            if (((ChordNode) obj).getSelf().getChordID() == this.getSelf().getChordID())
                return true;
            else return false;
        }
        else return false;
    }
}
