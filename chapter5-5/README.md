## Chapter 5-5 构建高可用的Eureka Server集群
====================================================================

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

