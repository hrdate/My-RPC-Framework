package core.netty.server;

import core.codec.CommonDecoder;
import core.codec.CommonEncoder;
import core.hook.ShutdownHook;
import core.provider.ServiceProviderImpl;
import core.registry.NacosServiceRegistry;
import core.serializer.CommonSerializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


public class NettyServer extends AbstractRpcServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
    private final CommonSerializer serializer;

    public NettyServer(String host, int port) {
        this(host, port, DEFAULT_SERIALIZER);
    }

    public NettyServer(String host, int port, Integer serializer) {
        this.host = host;
        this.port = port;
        serviceRegistry = new NacosServiceRegistry();
        serviceProvider = new ServiceProviderImpl();
        this.serializer = CommonSerializer.getByCode(serializer);
        //自动注册服务
        scanServices();
    }

    @Override
    public void start() {
        // 添加钩子函数，关闭后将自动注销所有服务
        ShutdownHook.getShutdownHook().addClearAllHook();
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
                    //日志打印方式
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 设置服务端接受连接的最大队列长度，如果队列已满，客户端连接将被拒绝。
                    .option(ChannelOption.SO_BACKLOG, 256)
                    // TCP的心跳机制，TCP会主动探测空闲连接的有效性
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    // 启用Nagle算法
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //初始化管道
                            ChannelPipeline pipeline = ch.pipeline();
                            // 心跳包，服务端
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS))
                                    .addLast(new CommonEncoder(serializer))
                                    .addLast(new CommonDecoder())
                                    // 心跳包响应事件
                                    .addLast(new NettyServerHandler());
                        }
                    });
            //绑定端口，启动Netty，sync()代表阻塞主Server线程，以执行Netty线程，如果不阻塞Netty就直接被下面shutdown了
            ChannelFuture future = serverBootstrap.bind(host, port).sync();
            //等确定通道关闭了，关闭future回到主Server线程
            future.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            logger.error("启动服务器时有错误发生: ", e);
        } finally {
            //优雅关闭Netty服务端且清理掉内存
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}

