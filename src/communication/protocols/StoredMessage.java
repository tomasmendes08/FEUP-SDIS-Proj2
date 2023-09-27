package communication.protocols;

import files.FileC;

import java.net.InetSocketAddress;

public class StoredMessage extends ProtocolMessage {
    public StoredMessage(InetSocketAddress address, FileC file) {
        super(address);
        this.file = file;
    }

    @Override
    protected void generateMsg() {

    }
}
