import client.RpcClient;
import client.RpcClientProxy;

public class TestClient {

    public static void main(String[] args) {
        RpcClientProxy rpcClientProxy = new RpcClientProxy("127.0.0.1",9000);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        String hello = helloService.hello(new HelloObject(123, "This is a test message!"));
        System.out.println("客户端收到回复:" + hello);
    }
}
