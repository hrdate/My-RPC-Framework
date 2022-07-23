package core.registry;

import com.alibaba.nacos.api.exception.NacosException;
import common.enumeration.RpcError;
import common.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import common.util.NacosUtil;

import java.net.InetSocketAddress;

/**
* 注册中心nacos注册服务
**/
public class NacosServiceRegistry implements ServiceRegistry{

    private static final Logger logger = LoggerFactory.getLogger(NacosServiceRegistry.class);

    @Override
    public void register(String serviceName, InetSocketAddress inetSocketAddress) {
        try {
            NacosUtil.registerService(serviceName, inetSocketAddress);
        } catch (NacosException e) {
            logger.error("注册服务时有错误发生:", e);
            throw new RpcException(RpcError.REGISTER_SERVICE_FAILED);
        }
    }





}
