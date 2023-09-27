package chord.communication;

import chord.communication.ChordMessage;

import java.net.InetSocketAddress;

public class CheckPredecessor extends ChordMessage {
    private long id;

    public CheckPredecessor(InetSocketAddress address, long id) {
        super(address);
        this.id = id;
        this.generateMsg();
    }

    @Override
    protected void generateMsg() {
        msgBody = "CHECKPREDECESSOR " + id;
    }
}
