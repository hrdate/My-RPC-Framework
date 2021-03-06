package core.provider;

import common.enumeration.RpcError;
import common.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.parser.Entity;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceProviderImpl implements ServiceProvider{
    private static final Logger logger = LoggerFactory.getLogger(ServiceProviderImpl.class);

    /**
     * 服务注册表serviceMap，注意需要加上static 修饰，防止被value为null失效
     */
    private static final Map<String, Object> serviceMap = new ConcurrentHashMap<>(16);
    private static final Set<String> registeredService = ConcurrentHashMap.newKeySet();

    /**
     * 添加服务
     * @param service 服务类
     * @param serviceName 服务名称
     * @param <T> 泛型
     */
    @Override
    public synchronized <T> void addServiceProvider(T service,String serviceName) {
//        String serviceName = service.getClass().getCanonicalName();
        if(registeredService.contains(serviceName)) {
            return;
        }
        logger.info("本地容器Map注册服务");
        Class<?>[] interfaceArray = service.getClass().getInterfaces();
        if(interfaceArray.length == 0) {
            throw new RpcException(RpcError.SERVICE_NOT_IMPLEMENT_ANY_INTERFACE);
        }
        serviceMap.put(serviceName, service);
        for(Class<?> i : interfaceArray) {
            serviceMap.put(i.getCanonicalName(), service);
        }
        registeredService.add(serviceName);
        logger.info("向接口:{} 注册服务:{} 服务实例:{}", interfaceArray, serviceName, service);
    }

    /**
     * 根据服务名称获取服务
     * @param serviceName 服务名称
     * @return 服务
     */
    @Override
    public synchronized Object getServiceProvider(String serviceName) {
        System.out.println("getServiceProvider serviceMap: 当前本地缓存的服务实例");
        for(Map.Entry<String, Object> entry : serviceMap.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getKey());
        }
        logger.info("getServiceProvider 根据服务名称获取服务:{}", serviceName);
        Object service = serviceMap.get(serviceName);
        if(service == null) {
            throw new RpcException(RpcError.SERVICE_NOT_FOUND);
        }
        return service;
    }
}
