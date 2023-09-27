package chord.communication;

import chord.communication.ChordMessage;

import java.net.InetSocketAddress;

public class PredecessorOnline extends ChordMessage {
    private long id;

    public PredecessorOnline(InetSocketAddress address, long id) {
        super(address);
        this.id = id;
        this.generateMsg();
    }

    @Override
    protected void generateMsg() {
        msgBody = "ONLINE " + id;
    }
}
