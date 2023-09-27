package chord.communication;

import communication.Message;

import java.io.Serializable;
import java.net.InetSocketAddress;

public abstract class ChordMessage implements Serializable, Message {
    protected InetSocketAddress peerAddress;
    protected String msgBody;

    public ChordMessage(InetSocketAddress address){
        this.peerAddress = address;
    }

    public InetSocketAddress getPeerAddress() {
        return peerAddress;
    }

    public String getMsgBody() {
        return msgBody;
    }

    protected abstract void generateMsg();

    @Override
    public String toString() {
        return "ChordMessage{" +
                "peerAddress=" + peerAddress +
                ", msgBody='" + msgBody + '\'' +
                '}';
    }
}
