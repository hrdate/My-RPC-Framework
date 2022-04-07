import netty.client.NettyClient;
import netty.client.RpcClient;
import netty.client.RpcClientProxy;
import serializer.CommonSerializer;

public class TestClient {

    public static void main(String[] args) {
        RpcClient client = new NettyClient(CommonSerializer.PROTOBUF_SERIALIZER);
        RpcClientProxy rpcClientProxy = new RpcClientProxy(client);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        String hello = helloService.hello(new HelloObject(123, "This is a test message!"));
        System.out.println("客户端收到回复:" + hello);
    }
}
