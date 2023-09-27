package chord.communication;

import chord.ChordNode;
import chord.communication.ChordMessage;

import java.net.InetSocketAddress;

public class UpdatePredecessor extends ChordMessage {
    private long id;

    public UpdatePredecessor(InetSocketAddress address, long id) {
        super(address);
        this.id = id;
        this.generateMsg();
    }

    @Override
    protected void generateMsg() {
        msgBody = "UPDATEPREDECESSOR " + id + " " + peerAddress.getAddress() + " " + peerAddress.getPort() + " " + ChordNode.self.getChordID();
    }
}
