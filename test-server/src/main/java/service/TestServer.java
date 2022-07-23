package service;

import core.annotation.ServiceScan;
import core.netty.server.NettyServer;

import core.serializer.CommonSerializer;

/**
 * @author huangrendi
 */
@ServiceScan
public class TestServer {

    public static void main(String[] args) {
        NettyServer server = new NettyServer(
                "127.0.0.1",9998, CommonSerializer.DEFAULT_SERIALIZER);
        server.start();
    }
}
