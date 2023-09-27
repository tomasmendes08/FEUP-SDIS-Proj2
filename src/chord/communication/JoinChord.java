package chord.communication;

import chord.communication.ChordMessage;

import java.net.InetSocketAddress;

public class JoinChord extends ChordMessage {
    public JoinChord(InetSocketAddress address) {
        super(address);
        this.generateMsg();
    }

    @Override
    protected void generateMsg() {
        msgBody = "JOIN CHORD " + peerAddress;
    }
}
