import annotation.ServiceScan;
import netty.server.NettyServer;

import serializer.CommonSerializer;

@ServiceScan
public class TestServer {

    public static void main(String[] args) {
        NettyServer server = new NettyServer(
                "127.0.0.1",9998, CommonSerializer.DEFAULT_SERIALIZER);
        server.start();
    }
}
