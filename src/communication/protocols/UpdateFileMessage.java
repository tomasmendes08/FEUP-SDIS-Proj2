package communication.protocols;

import files.FileC;

import java.net.InetSocketAddress;

public class UpdateFileMessage extends ProtocolMessage {
    public UpdateFileMessage(InetSocketAddress address, FileC file) {
        super(address);
        this.file = file;
    }

    @Override
    protected void generateMsg() {

    }
}
