package communication.protocols;

import files.FileC;

import java.net.InetSocketAddress;

public class BackupMessage extends ProtocolMessage {
    public BackupMessage(InetSocketAddress address, FileC file) {
        super(address);
        this.file = file;
    }

    @Override
    protected void generateMsg() {

    }
}
