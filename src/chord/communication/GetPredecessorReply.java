package chord.communication;

import chord.ChordInfo;
import chord.communication.ChordMessage;

import java.net.InetSocketAddress;

public class GetPredecessorReply extends ChordMessage {
    private long id;
    private ChordInfo predecessor;

    public GetPredecessorReply(InetSocketAddress address, long id, ChordInfo predecessor) {
        super(address);
        this.id = id;
        this.predecessor = predecessor;
        this.generateMsg();
    }

    @Override
    protected void generateMsg() {
        if (predecessor != null)
            msgBody = "GETPREDECESSORREPLY " + id + " " + predecessor.getChordID() + " " + predecessor.getSocketAddress().getHostString() + " " + predecessor.getSocketAddress().getPort();
        else
            msgBody = "GETPREDECESSORREPLY " + id + " 0" + " " + peerAddress.getAddress() + " " + peerAddress.getPort();
    }
}
