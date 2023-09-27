package communication.protocols;

import communication.Message;
import files.FileC;

import java.io.Serializable;
import java.net.InetSocketAddress;

public abstract class ProtocolMessage implements Serializable, Message {
    protected InetSocketAddress peerAddress;
    protected String msgBody;
    protected FileC file;

    public ProtocolMessage(InetSocketAddress address){
        this.peerAddress = address;
    }

    public InetSocketAddress getPeerAddress() {
        return peerAddress;
    }

    public String getMsgBody() {
        return msgBody;
    }

    public FileC getFile() {
        return file;
    }

    protected abstract void generateMsg();

    @Override
    public String toString() {
        return "ProtocolMessage{" +
                "peerAddress=" + peerAddress +
                ", msgBody='" + msgBody + '\'' +
                ", file=" + file +
                '}';
    }
}
