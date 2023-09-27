package chord.communication;

import java.net.InetSocketAddress;

public class Lookup extends ChordMessage{
    private long key;
    public Lookup(InetSocketAddress address, long key) {
        super(address);
        this.key = key;
        this.generateMsg();
    }

    @Override
    protected void generateMsg() {
        //System.out.println("Outro node vai procurar");
        msgBody = "LOOKUP " + key+ " " + peerAddress.getAddress() + " " + peerAddress.getPort();
    }
}
