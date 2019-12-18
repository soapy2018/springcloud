## Chapter 6-3 使用RestTemplate和Ribbon来消费服务
====================================================================

常见的负载均衡有两种方式：一是独立进程单元，通过负载均衡策略，将请求转发到不同的执行单元，例如Nginx；另一种是将负载均衡逻辑以代码的形式封装到服务消费者的客户端上，服务消费者客户端维护了一份服务提供者的信息列表，有了信息列表，通过负载均衡策略将请求分摊给多个服务提供者，从而达到负载均衡的目的。

Ribbo是Netfilx公司开源的一个负载均衡的组件，它属于上述的第二种方式，是将负载均衡逻辑封装在客户端中，并运行在客户端的进程里。在Spring Cloud构建的微服务系统中，Ribbon作为服务消费者的负载均衡器，有两种使用方式：一种是和RestTemplate相结合；另一种是和Feign相结合。Feign已经默认集成了Ribbon。
在实际项目中，可能有几十个或几百个的微服务实例，这时Eureka Server可能成为瓶颈，所以需要对Eureka Server进行高可用集群部署。本案例在5-2基础上进行改造。

1、更改eureka-server的配置文件，采用多profile的格式。如下：
```
---
spring:
   profiles: peer1
   application:
      name: eureka-server1
server:
   port: 8551
eureka:
   instance:
      hostname: peer1
   client:
      serviceUrl:
         defaultZone: http://peer2:8552/eureka/

---
spring:
   profiles: peer2
   application:
      name: eureka-server2
server:
   port: 8552
eureka:
   instance:
      hostname: peer2
   client:
      serviceUrl:
         defaultZone: http://peer1:8551/eureka/
```
上述配置定义了两个profile文件，分别为peer1和peer2，它们的hostname分别为peer1和peer2（在实际开发中可能是具体的服务器IP地址），它们的端口分别为8551和8552。因为是本地搭建的Eureka Server集群，所有需要添加本地host配置：
```
127.0.0.1 peer1
127.0.0.1 peer2
```
2、并行启动两个eureka-server实例，通过```--spring.profiles.active```分别指定配置文件peer1和peer2。

3、启动eureka-client，eureka-client向peer1注册，其配置如下：
```
server:
  port: 8553
spring:
  application:
    name: eureka-client
eureka:
  client:
    serviceUrl:
      defaultZone: http://peer1:8551/eureka/
```
此时访问peer2主页，发现有eureka-client的注册信息，说明peer1的注册列表信息同步给了peer2。

