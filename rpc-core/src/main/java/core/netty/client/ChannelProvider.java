package core.netty.client;

import common.enumeration.RpcError;
import common.exception.RpcException;
import core.codec.CommonDecoder;
import core.codec.CommonEncoder;
import core.serializer.CommonSerializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 用于获取 Channel 对象
 */
public class ChannelProvider {

    private static final Logger logger = LoggerFactory.getLogger(ChannelProvider.class);
    private static  EventLoopGroup eventLoopGroup;

    // 调用initializeBootstrap()方法初始化，例如：开启TCP 底层心跳机制
    private static Bootstrap bootstrap = initializeBootstrap();

    private static final Integer MAX_RETRY_COUNT = 5;

    // 存储 Channel 在 ConcurrentHashMap内存中
    private static Map<String, Channel> channels = new ConcurrentHashMap<>();

    private static Bootstrap initializeBootstrap() {
        eventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                //连接的超时时间，超过这个时间还是建立不上的话则代表连接失败
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                //是否开启 TCP 底层心跳机制
                .option(ChannelOption.SO_KEEPALIVE, true)
                //TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                .option(ChannelOption.TCP_NODELAY, true);
        return bootstrap;
    }

    public static Channel get(InetSocketAddress inetSocketAddress, CommonSerializer serializer) throws InterruptedException {
        String key = inetSocketAddress.toString() + serializer.getCode();
        if (channels.containsKey(key)) {
            Channel channel = channels.get(key);
            if(channels != null && channel.isActive()) {
                return channel;
            } else {
                channels.remove(key);
            }
        }
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                /*自定义序列化编解码器*/
                // RpcResponse -> ByteBuf
                ch.pipeline().addLast(new CommonEncoder(serializer))
                        // 心跳包，客户端触发userEventTriggered方法
                        .addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS))
                        .addLast(new CommonDecoder())
                        // 自定义客户端处理器
                        .addLast(new NettyClientHandler());
            }
        });
        Channel channel = null;
        //设置计数器值为1
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            // 连接从服务列表中获取的真实服务地址
            channel = connect(bootstrap, inetSocketAddress, countDownLatch);
            countDownLatch.await();
        } catch (ExecutionException e) {
            logger.error("连接客户端时有错误发生", e);
            return null;
        }
        channels.put(key, channel);
        return channel;
    }

    private static Channel connect(Bootstrap bootstrap, InetSocketAddress inetSocketAddress, CountDownLatch countDownLatch) throws ExecutionException, InterruptedException {
        return connect(bootstrap, inetSocketAddress, MAX_RETRY_COUNT, countDownLatch);
    }

    /**
     * Netty客户端创建通道连接,实现连接失败重试机制
     * @param bootstrap
     * @param inetSocketAddress
     * @param retry
     * @param countDownLatch
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
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



}
