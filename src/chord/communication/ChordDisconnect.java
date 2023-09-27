package chord.communication;

import chord.communication.ChordMessage;

import java.net.InetSocketAddress;

public class ChordDisconnect extends ChordMessage {
    private long peerID;
    private long predecessorID;
    private long successorID;
    private InetSocketAddress predecessorSocketAddress;
    private InetSocketAddress successorSocketAddress;

    public ChordDisconnect(InetSocketAddress address, long peerID, long predecessorID, InetSocketAddress predecessorSocketAddress, long successorID, InetSocketAddress successorSocketAddress) {
        super(address);
        this.peerID = peerID;
        this.predecessorID = predecessorID;
        this.successorID = successorID;
        this.predecessorSocketAddress = predecessorSocketAddress;
        this.successorSocketAddress = successorSocketAddress;
        this.generateMsg();
    }

    @Override
    protected void generateMsg() {
        msgBody = "CHORDDISCONNECT " + peerID;
        if (predecessorSocketAddress != null) {
            msgBody += " " + predecessorID + " " + predecessorSocketAddress.getHostName() + " " + predecessorSocketAddress.getPort();
        }
        else
            msgBody += " " + predecessorID + " " + null + " " + null;
        msgBody += " " + successorID + " " + successorSocketAddress.getHostName() + " " + successorSocketAddress.getPort();
    }
}
