# My-RPC-Framework

简易RPC框架Demo，学习中...
采用动态代理方法完成客户端请求的封装、服务端对请求的动态解析
客户端和服务端均采用Netty进行网络传输，传输过程采用自定义协议
兼容多种主流的序列化工具，例如java自带序列、fastjson、kyro等
采用nacos完成服务注册，并实现了随机和轮询两种负载均衡算法。
