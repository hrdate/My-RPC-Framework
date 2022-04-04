# My-RPC-Framework

简易RPC框架Demo，学习中...
一款基于 Nacos 实现的 RPC 轻量级框架

采用 JDK 动态代理方法完成客户端请求的封装、服务端对请求的动态解析

客户端和服务端均采用Netty进行网络传输，传输过程采用自定义协议，会采用 Netty 的心跳机制 IdleStateEvent，保证连接

兼容多种主流的序列化工具，例如Java自带序列、fastjson、kyro等

采用Nacos完成服务注册，管理服务提供者信息，并实现了随机和轮询两种负载均衡算法

采用自动注解Service自动注册服务，JVM钩子函数注销服务

采用ConcurrentHashMap异步获取Netty请求的响应结果，将每个请求对应的CompletableFuture实例都保存在一个Map中，其中key为请求ID


# 项目模块概览
roc-api	——	通用接口

rpc-common	——	实体对象、工具类等公用类

rpc-core	——	框架的核心实现

test-client	——	测试用消费侧

test-server	——	测试用提供侧


# 传输协议

```
+---------------+---------------+-----------------+-------------+
|  Magic Number |  Package Type | Serializer Type | Data Length |
|    4 bytes    |    4 bytes    |     4 bytes     |   4 bytes   |
+---------------+---------------+-----------------+-------------+
|                          Data Bytes                           |
|                   Length: ${Data Length}                      |
+---------------------------------------------------------------+
```


| 字段            | 解释                                                         |
| :-------------- | :----------------------------------------------------------- |
| Magic Number    | 魔数，表识一个 MRF 协议包                                    |
| Package Type    | 包类型，标明是请求活或响应                                    |
| Serializer Type | 序列化器类型，标明序列化方式                                 |
| Data Length     | 数据字节的长度                                               |
| Data Bytes      | 传输的对象                                                  |

