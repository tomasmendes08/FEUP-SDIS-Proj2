package chord.communication;

import chord.ChordInfo;

import java.net.InetSocketAddress;

public class JoinChordReply extends ChordMessage {
    private long assignedId;
    private ChordInfo successor;

    public JoinChordReply(InetSocketAddress address, long id, ChordInfo successor) {
        super(address);
        this.assignedId = id;
        this.successor = successor;
        this.generateMsg();
    }

    public long getAssignedId() {
        return assignedId;
    }

    @Override
    protected void generateMsg() {
        msgBody = "JOIN ACCEPTED " + assignedId + " " + successor.getChordID() + " " + successor.getSocketAddress().getHostString() + " " + successor.getSocketAddress().getPort();
    }
}
