package common.util;


import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import common.enumeration.RpcConfigEnum;
import common.enumeration.RpcError;
import common.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;

/**
 * 管理Nacos连接等工具类
 */
public class NacosUtil {

    private static final Logger logger = LoggerFactory.getLogger(NacosUtil.class);

    private static final NamingService namingService;
    private static final Set<String> serviceNames = new HashSet<>();
    private static InetSocketAddress address;

    private static final String DEFAULT_NACOS_SERVER_ADDR = "127.0.0.1:8848";

    static {
        namingService = getNacosNamingService();
    }

    /**
     * 先通过工具栏尝试获取配置文件中的nacos地址,若无则采用默认值
     * @return 连接注册中心nacos的服务
     */
    public static NamingService getNacosNamingService() {
        try {
            Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
            String nacosAddress = properties != null && properties.getProperty(RpcConfigEnum.NACOS_ADDRESS.getPropertyValue()) != null ? properties.getProperty(RpcConfigEnum.NACOS_ADDRESS.getPropertyValue()) : DEFAULT_NACOS_SERVER_ADDR;
            return NamingFactory.createNamingService(nacosAddress);
        } catch (NacosException e) {
            logger.error("连接到Nacos时有错误发生: ", e);
            throw new RpcException(RpcError.FAILED_TO_CONNECT_TO_SERVICE_REGISTRY);
        }
    }

    /**
     * 将一个服务注册进注册表
     * @param serviceName 服务名
     * @param address 服务地址
     * @throws NacosException
     */
    public static void registerService(String serviceName, InetSocketAddress address) throws NacosException {
        namingService.registerInstance(serviceName, address.getHostName(), address.getPort());
        NacosUtil.address = address;
        serviceNames.add(serviceName);
    }

    /**
     *
     * @param serviceName 服务名称
     * @return 获得提供某个服务的所有提供者的列表
     * @throws NacosException
     */
    public static List<Instance> getAllInstance(String serviceName) throws NacosException {
        return namingService.getAllInstances(serviceName);
    }

    /**
     * 注销所有服务
     */
    public static void clearRegistry() {
        if(!serviceNames.isEmpty() && address != null) {
            String host = address.getHostName();
            int port = address.getPort();
            Iterator<String> iterator = serviceNames.iterator();
            while(iterator.hasNext()) {
                String serviceName = iterator.next();
                try {
                    namingService.deregisterInstance(serviceName, host, port);
                } catch (NacosException e) {
                    logger.error("注销服务 {} 失败", serviceName, e);
                }
            }
        }
    }
}

