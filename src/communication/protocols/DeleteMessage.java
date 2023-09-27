package communication.protocols;

import files.FileC;

import java.net.InetSocketAddress;

public class DeleteMessage extends ProtocolMessage{
    public DeleteMessage(InetSocketAddress address, FileC file) {
        super(address);
        this.file = file;
    }

    @Override
    protected void generateMsg() {
        this.msgBody = "DELETE " + file.getOriginalName();
    }
}
