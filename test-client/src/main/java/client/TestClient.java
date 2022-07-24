package client;

import api.ByeService;
import api.HelloObject;
import api.HelloService;
import core.netty.client.NettyClient;
import core.netty.client.RpcClient;
import core.netty.client.RpcClientProxy;
import core.serializer.CommonSerializer;

/**
 * @author huangrendi
 */
public class TestClient {

    public static void main(String[] args) throws InterruptedException {
        RpcClient client = new NettyClient(CommonSerializer.PROTOBUF_SERIALIZER);
        RpcClientProxy rpcClientProxy = new RpcClientProxy(client);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        String hello = helloService.hello(new HelloObject(123, "This is a test message!"));
        System.out.println("客户端收到回复:" + hello);
        Thread.sleep(5000);
        ByeService byeService = rpcClientProxy.getProxy(ByeService.class);
        String bye = byeService.bye("This is a test message for bye!");
        System.out.println("客户端收到回复:" + bye);
    }
}
