package chord.communication;

import chord.ChordInfo;

import java.net.InetSocketAddress;

public class LookupReply extends ChordMessage{
    private long key;
    private long successorID;
    private ChordInfo successor;

    public LookupReply(InetSocketAddress address, long chordID, long successorID, ChordInfo successor) {
        super(address);
        this.key = chordID;
        this.successorID = successorID;
        this.successor = successor;
        this.generateMsg();
    }

    @Override
    protected void generateMsg() {
        msgBody = "LOOKUPREPLY " + key + " " + successorID + " " + peerAddress.getAddress().getHostAddress() + " " + peerAddress.getPort();
    }
}
