package communication.protocols;

import files.FileC;

import java.net.InetSocketAddress;

public class RestoreMessage extends ProtocolMessage {
    public RestoreMessage(InetSocketAddress address, FileC file) {
        super(address);
        this.file = file;
    }

    @Override
    protected void generateMsg() {

    }
}
