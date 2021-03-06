# My-RPC-Framework

手写RPC简易版框架Demo，学习中...
一款基于 Nacos 实现的 RPC 轻量级框架



# 项目模块概览

roc-api	——	通用接口

rpc-common	——	实体对象、工具类等公用类

rpc-core	——	框架的核心实现

test-client	——	测试用消费侧

test-server	——	测试用提供侧



# 项目介绍



1. 实现了基于 Netty 传输网络传输方式，采用多对多线程模型
2. 使用Nacos作业注册中心，管理服务提供者信息
3. JDK动态代理增强方法
4. 自定义传输协议
5. 实现两种负载均衡算法：随机算法与轮转算法
6. 通过netty编码解码兼容多种序列化工具
7. 自定义注解@ServiceScan和@Service实现服务扫描自动注册
8. CompletableFuture实例异步获取netty请求的响应结果
9. 钩子函数ShutdownHook优雅下线
10. 使用netty心跳机制，检测保持连接
11. 客户端启动时使用countDownLatch实现连接失败重试机制





## 引入注册中心nacos

采用Docker的方式导入nacos镜像

单机版运行指令

```
1：docker pull docker.io/nacos/nacos-server

2：docker run --env MODE=standalone --name nacos -d -p 8848:8848 nacos/nacos-server
```

登陆地址：http://localhost:8848/nacos/index.html默认账号密码是nacos/nacos

如要把服务信息保存在MySQL数据库中，可自行搜索资料修改Docker实例里面的/home/nacos/conf/application.properties文件



## 动态代理

采用 **JDK 动态代理**方法完成客户端请求的封装、服务端对请求的动态解析

客户端

```java
public class RpcClientProxy implements InvocationHandler {
	...其他参数
	
	 @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {  // 生成代理类
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
    	...代理类实际处理过程
    }
}
```

## 传输协议

客户端和服务端均采用Netty进行网络传输，传输过程采用**自定义协议**

```
+---------------+---------------+-----------------+-------------+
|  Magic Number |  Package Type | Serializer Type | Data Length |
|    4 bytes    |    4 bytes    |     4 bytes     |   4 bytes   |
+---------------+---------------+-----------------+-------------+
|                          Data Bytes                           |
|                   Length: ${Data Length}                      |
+---------------------------------------------------------------+
```


| 字段            | 解释                         |
| :-------------- | :--------------------------- |
| Magic Number    | 魔数，表识一个 MRF 协议包    |
| Package Type    | 包类型，标明是请求活或响应   |
| Serializer Type | 序列化器类型，标明序列化方式 |
| Data Length     | 数据字节的长度               |
| Data Bytes      | 传输的对象                   |



## 线程模型

采用**主从多线程模型**，从一个主线程 NIO 线程池中选择一个线程作为 Acceptor 线程，绑定监听端口，接收客户端连接的连接，其他线程负责后续的接入认证等工作。

客户端

```java
private static final EventLoopGroup group;
private static final Bootstrap bootstrap;

static {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class);
    }
```

服务端

```java
//用于处理客户端新连接的主”线程池“
EventLoopGroup bossGroup = new NioEventLoopGroup();
//用于连接后处理IO事件的从”线程池“
EventLoopGroup workerGroup = new NioEventLoopGroup();
try {
    //初始化Netty服务端启动器，作为服务端入口
    ServerBootstrap serverBootstrap = new ServerBootstrap();
    //将主从“线程池”初始化到启动器中
    serverBootstrap.group(bossGroup, workerGroup)
        //设置服务端通道类型
        .channel(NioServerSocketChannel.class)
        .其他配置操作
```

## 编码译码-序列化

自定义`netty`的**通用编码和译码拦截器**，兼容多种主流的序列化`CommonSerializer`工具，例如Java自带序列、fastjson、kyro等

```java
/**
 * 通用的编码拦截器
 */
public class CommonEncoder extends MessageToByteEncoder {
	...其他操作
        
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        ...进行一系列自定义协议，编码数据包的操作，并msg写入out中
    }
}

/**
 * 通用的解码拦截器
 */
public class CommonDecoder extends ReplayingDecoder {
    ...其他操作
    
	@Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ...进行一系列自定义协议，解码数据包的操作，从in中获取数据，识别协议包，并把解析后的数据写入out中
    }
}
```

采用`Nacos`完成服务注册`ServiceRegistry`和服务发现`ServiceDiscovery`，管理服务提供者信息

负载均衡`LoadBalancer`实现了随机和轮询两种算法

## 服务扫描-自动注册

采用注解**@Service表示服务类**

```java
/**
 * 表示一个服务提供类，用于远程接口的实现类
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
    public String name() default "";
}
```

采用注解**@ServiceScan**扫描基础包路径下所有标识**@Service**的服务类，并**自动注册**

```java
/**
 * 服务扫描的基包
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceScan {
    public String value() default "";
}
```

## 钩子服务注销

JVM**钩子函数Hook注销服务**

```java
public void addClearAllHook() {
        logger.info("关闭后将自动注销所有服务");
        // Runtime 对象是 JVM 虚拟机的运行时环境，
        // 调用其 addShutdownHook 方法增加一个钩子函数，创建一个新线程调用 clearRegistry 方法完成注销工作。
        // 这个钩子函数会在 JVM 关闭之前被调用。
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            NacosUtil.clearRegistry();  // 在注册中心nacos工具类内，通过本地缓存向nacos清除服务
        }));
}
```



## 异步获取Netty请求的响应结果

采用异步获取Netty请求的响应结果，将每个请求对应的CompletableFuture实例都保存在一个Map中，其中key为请求ID

```java
public class UnprocessedRequests {  //未处理请求
	private static ConcurrentHashMap<String, CompletableFuture<RpcResponse>> unprocessedResponseFutures;
    ...其他内容
}
```

jdk代理增加发送信息接口，请求响应`rpcResponse = completableFuture.get();`

```java
@Override
public Object invoke(Object proxy, Method method, Object[] args) {
  logger.info("调用方法: {}#{}", method.getDeclaringClass().getName(), method.getName());
  // 生成客户端请求id，并组装一个请求RpcRequest实体类
  RpcRequest rpcRequest = new RpcRequest(UUID.randomUUID().toString(), method.getDeclaringClass().getName(),
                                         method.getName(), args, method.getParameterTypes(), false);
  RpcResponse rpcResponse = null;
  if (client instanceof NettyClient) {
    try {
      CompletableFuture<RpcResponse> completableFuture = (CompletableFuture<RpcResponse>) client.sendRequest(rpcRequest);
      rpcResponse = completableFuture.get();
    } catch (Exception e) {
      logger.error("方法调用请求发送失败", e);
      return null;
    }
  }
  // 验证请求和响应
  RpcMessageChecker.check(rpcRequest, rpcResponse);
  return rpcResponse.getData();
}
```





## 数据校验

利用请求号对服务端返回的响应数据进行校验，保证请求与响应一一对应

```java
//客户端jdk代理类中处理
public Object invoke(Object proxy, Method method, Object[] args) {
	...其他代码
	
	// 验证请求和响应
	RpcMessageChecker.check(rpcRequest, rpcResponse);
    return rpcResponse.getData();
}
```

客户端校验

```java
public static void check(RpcRequest rpcRequest, RpcResponse rpcResponse) {
        if (rpcResponse == null) {
            logger.error("调用服务失败,serviceName:{}", rpcRequest.getInterfaceName());
            throw new RpcException(RpcError.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(RpcError.RESPONSE_NOT_MATCH, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (rpcResponse.getStatusCode() == null || !rpcResponse.getStatusCode().equals(ResponseCode.SUCCESS.getCode())) {
            logger.error("调用服务失败,serviceName:{},RpcResponse:{}", rpcRequest.getInterfaceName(), rpcResponse);
            throw new RpcException(RpcError.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
}
```

netty编码解码都可以起到校验的作用

```java
@Override
protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int magic = in.readInt();
        if (magic != MAGIC_NUMBER) {
            logger.error("不识别的协议包: {}", magic);
            throw new RpcException(RpcError.UNKNOWN_PROTOCOL);
        }
        int packageCode = in.readInt();
        Class<?> packageClass;
        if (packageCode == PackageType.REQUEST_PACK.getCode()) {
            packageClass = RpcRequest.class;
        } else if (packageCode == PackageType.RESPONSE_PACK.getCode()) {
            packageClass = RpcResponse.class;
        } else {
            logger.error("不识别的数据包: {}", packageCode);
            throw new RpcException(RpcError.UNKNOWN_PACKAGE_TYPE);
        }
        int serializerCode = in.readInt();
        CommonSerializer serializer = CommonSerializer.getByCode(serializerCode);
        if (serializer == null) {
            logger.error("不识别的反序列化器: {}", serializerCode);
            throw new RpcException(RpcError.UNKNOWN_SERIALIZER);
        }
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        Object obj = serializer.deserialize(bytes, packageClass);
        out.add(obj);
}
```



## Netty心跳机制

采用 Netty 的**心跳机制** IdleStateEvent，保证连接

客户端

```java
public class NettyClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    
    ...其他函数
    
	/*
        设定IdleStateHandler心跳检测每五秒进行一次读检测，
        如果五秒内ChannelRead()方法未被调用则触发一次userEventTrigger()方法
        自定义处理类Handler继承ChannlInboundHandlerAdapter，
        实现其userEventTriggered()方法，在出现超时事件时会被触发，包括读空闲超时或者写空闲超时；
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {
                logger.info("发送心跳包 [{}]", ctx.channel().remoteAddress());
                Channel channel = ChannelProvider.get((InetSocketAddress) ctx.channel().remoteAddress(), CommonSerializer.getByCode(CommonSerializer.DEFAULT_SERIALIZER));
                RpcRequest rpcRequest = new RpcRequest();
                rpcRequest.setHeartBeat(true);
                channel.writeAndFlush(rpcRequest).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
```

服务端

```java
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
	
    ...其他函数方法
    /*
        设定IdleStateHandler心跳检测每五秒进行一次读检测，
        如果五秒内ChannelRead()方法未被调用则触发一次userEventTrigger()方法
    */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                logger.info("长时间未收到心跳包，断开连接...");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
```

## 连接失败重试机制

客户端使用`new CountDownLatch(1)`连接服务失败重试机制

```java
private static Channel connect(Bootstrap bootstrap, InetSocketAddress inetSocketAddress,
                        int retry, CountDownLatch countDownLatch) throws ExecutionException, InterruptedException {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                logger.info("客户端连接成功!");
                // 如果尚未完成，则将get()和相关方法返回的值设置为给定值
                completableFuture.complete(future.channel());
                //计数器减一
                countDownLatch.countDown();
            }
            if (retry == 0) {
                logger.error("客户端连接失败：重试次数已用完，放弃连接！");
                countDownLatch.countDown();
                throw new RpcException(RpcError.CLIENT_CONNECT_SERVER_FAILURE);
            }
            //第几次重连
            int order = (MAX_RETRY_COUNT - retry) + 1;
            //重连的时间间隔，相当于1乘以2的order次方
            int delay = 1 << order;
            logger.error("{}:连接失败，第{}次重连……", new Date(), order);
            //利用schedule()在给定的延迟时间后执行connect()重连
            bootstrap.config().group().schedule(() -> connect(bootstrap, inetSocketAddress,
                    retry - 1, countDownLatch), delay, TimeUnit.SECONDS);
        });
        return completableFuture.get();
}
```



服务端启动成功后，打印的部分日志

![image-20220723115136071](README.assets/image-20220723115136071.png)



客户端成功启动并发送请求后，打印的部分日志

![image-20220723115224929](README.assets/image-20220723115224929.png)



成功注册项目后，nacos控制台现实界面

![image-20220723022512028](README.assets/image-20220723022512028.png)
