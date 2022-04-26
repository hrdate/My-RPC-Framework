package provider;

import enumeration.RpcError;
import exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceProviderImpl implements ServiceProvider{
    private static final Logger logger = LoggerFactory.getLogger(ServiceProviderImpl.class);

    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();
    private final Set<String> registeredService = ConcurrentHashMap.newKeySet();

    /**
     * 添加服务
     * @param service 服务类
     * @param serviceName 服务名称
     * @param <T> 泛型
     */
    @Override
    public synchronized <T> void addServiceProvider(T service,String serviceName) {
//        String serviceName = service.getClass().getCanonicalName();
        if(registeredService.contains(serviceName)) return;
        registeredService.add(serviceName);
        Class<?>[] interfaceArray = service.getClass().getInterfaces();
        if(interfaceArray.length == 0) {
            throw new RpcException(RpcError.SERVICE_NOT_IMPLEMENT_ANY_INTERFACE);
        }
        for(Class<?> i : interfaceArray) {
            serviceMap.put(i.getCanonicalName(), service);
        }
        logger.info("向接口: {} 注册服务: {}", interfaceArray, serviceName);
    }

    /**
     * 根据服务名称获取服务
     * @param serviceName 服务名称
     * @return 服务
     */
    @Override
    public synchronized Object getServiceProvider(String serviceName) {
        Object service = serviceMap.get(serviceName);
        if(service == null) {
            throw new RpcException(RpcError.SERVICE_NOT_FOUND);
        }
        return service;
    }
}
