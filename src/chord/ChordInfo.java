package chord;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class ChordInfo implements Serializable {
    private long chordID;
    private InetSocketAddress socketAddress;
    private ChordNode nodeRef;

    public ChordInfo(long chordID, InetSocketAddress socketAddress) {
        this.socketAddress = new InetSocketAddress(socketAddress.getHostString(), socketAddress.getPort());
        this.chordID = chordID;
        //this.chordID = generateChordID(this.socketAddress);
    }

    public void setNodeRef(ChordNode nodeRef) {
        this.nodeRef = nodeRef;
    }

    public ChordNode getNodeRef() {
        return nodeRef;
    }

    public long getChordID() {
        return chordID;
    }

    public void setChordID(long chordID) {
        this.chordID = chordID;
    }

    public InetSocketAddress getSocketAddress() {
        return this.socketAddress;
    }

    public void setSocketAddress(InetSocketAddress inetSocketAddress) {
        this.socketAddress = inetSocketAddress;
    }

    @Override
    public String toString() {
        return "ChordInfo{" +
                "chordID=" + chordID +
                ", socketAddress=" + socketAddress +
                ", nodeRef=" + nodeRef +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChordInfo chordInfo = (ChordInfo) o;
        return chordID == chordInfo.chordID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chordID, socketAddress, nodeRef);
    }
}
