package communication;

import java.net.InetSocketAddress;

public interface Message {
    InetSocketAddress peerAddress = null;
    String protocol = null;
}
