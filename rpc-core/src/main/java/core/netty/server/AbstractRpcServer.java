package core.netty.server;

import core.annotation.Service;
import core.annotation.ServiceScan;
import common.enumeration.RpcError;
import common.exception.RpcException;
import core.registry.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import core.provider.ServiceProvider;
import common.util.ReflectUtil;

import java.net.InetSocketAddress;
import java.util.Set;

/**
 * @author huangrendi
 */
public abstract class AbstractRpcServer implements RpcServer {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    protected String host;
    protected int port;

    protected ServiceRegistry serviceRegistry;
    protected ServiceProvider serviceProvider;

    public void scanServices() {
        String mainClassName = ReflectUtil.getStackTrace();
        logger.info("服务发现注册扫描 scanServices mainClassName:{}", mainClassName);
        Class<?> startClass;
        try {
            startClass = Class.forName(mainClassName);
            if(!startClass.isAnnotationPresent(ServiceScan.class)) {
                logger.error("scanServices: 启动类缺少 @ServiceScan 注解");
                throw new RpcException(RpcError.SERVICE_SCAN_PACKAGE_NOT_FOUND);
            }
        } catch (ClassNotFoundException e) {
            logger.error("scanServices: 出现未知错误");
            throw new RpcException(RpcError.UNKNOWN_ERROR);
        }
        String basePackage = startClass.getAnnotation(ServiceScan.class).value();
        if("".equals(basePackage)) {
            // 注意一定要有包名，即蓝色Java目录下，还需要有一层包名后，再创建别的目录和类
            // 需要确保包名不为空，才可以扫描到@ServiceScan 和 @Service
            basePackage = mainClassName.substring(0, mainClassName.lastIndexOf("."));
        }
        Set<Class<?>> classSet = ReflectUtil.getClasses(basePackage);
        for(Class<?> clazz : classSet) {
            if(clazz.isAnnotationPresent(Service.class)) {
                String serviceName = clazz.getAnnotation(Service.class).name();
                Object obj;
                try {
                    obj = clazz.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    logger.error("scanServices: 创建 " + clazz + " 时有错误发生");
                    continue;
                }
                if("".equals(serviceName)) {
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> oneInterface: interfaces){
                        publishService(obj, oneInterface.getCanonicalName());
                        logger.info("scanServices getCanonicalName: 通过扫描@Service成功向注册中心自动注册服务:{}", oneInterface.getCanonicalName());
                    }
                } else {
                    publishService(obj, serviceName);
                    logger.info("scanServices serviceName: 通过扫描@Service向注册中心自动注册服务:{}", serviceName);
                }
            }
        }
    }

    @Override
    public <T> void publishService(T service, String serviceName) {
        serviceRegistry.register(serviceName, new InetSocketAddress(host, port));
        serviceProvider.addServiceProvider(service, serviceName);

    }

}

