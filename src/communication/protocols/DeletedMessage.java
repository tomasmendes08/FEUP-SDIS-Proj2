package communication.protocols;

import chord.ChordInfo;
import chord.ChordNode;
import files.FileC;

import java.net.InetSocketAddress;

public class DeletedMessage extends ProtocolMessage{
    private ChordInfo chordNodeSender;
    public DeletedMessage(InetSocketAddress address, FileC file, ChordInfo chordNodeSender) {
        super(address);
        this.file = file;
        this.chordNodeSender = chordNodeSender;
    }

    public ChordInfo getChordNodeSender() {
        return chordNodeSender;
    }

    @Override
    protected void generateMsg() {
        this.msgBody = "DELETED " + file.getOriginalName();
    }
}
