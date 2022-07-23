package core.registry;


import java.net.InetSocketAddress;

public interface ServiceRegistry {

    /**
     * 将一个服务注册进注册表
     * @param serviceName 服务名称
     * @param inetSocketAddress 服务地址
     */
    void register(String serviceName, InetSocketAddress inetSocketAddress);



}
