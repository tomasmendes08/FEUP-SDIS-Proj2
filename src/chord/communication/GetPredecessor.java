package chord.communication;

import chord.communication.ChordMessage;

import java.net.InetSocketAddress;

public class GetPredecessor extends ChordMessage {
    private long id;

    public GetPredecessor(InetSocketAddress address, long id) {
        super(address);
        this.id = id;
        this.generateMsg();
    }

    @Override
    protected void generateMsg() {
        msgBody = "GETPREDECESSOR " + id;
    }
}
