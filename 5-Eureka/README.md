## Chapter5 服务注册和发现Eureka
====================================================================

Eureka来源于古希腊词汇，意为“发现了”。在软件领域，Eureka是Netflix在线影片公司开源的一个服务注册与发现的组件。Eureka分为Eureka Server和Eureka Client，Eureka Server为Eureka服务注册中心，Eureka Client为Eureka客户端。Eureka和其他组件，比如负载均衡组件Ribbon、熔断器组件Hystrix、熔断器监控组件Hystrix Dashboard、熔断器聚合监控Turbine，以及网关Zuul组件相互配合，能够轻松实现服务注册和发现、负载均衡、熔断和智能路由等功能，这些组件都是Netflix公司开源的，一起被称为Netflix OSS组件。Netflix OSS组件由Spring Cloud整合为Spring Cloud Netflix组件，它是Spring Cloud架构微服务的核心组件，也是基础组件。Eureka主要包含3种角色：
+ Register Service：服务注册中心，它是一个Eureka Server，提供服务注册和发现的功能。
+ Provider Service：服务提供者，它是一个Eureka Client，提供服务。
+ Consumer Service：服务消费者，它是一个Eureka Client，消费服务。
服务消费的基本过程：首先需要一个服务注册中心Eureka Server，服务提供者Eureka Client向服务注册中心注册，将自己的信息（比如服务吗和服务的IP地址等）通过REST API的形式提交给服务注册中心。同样，服务消费者Eureka Client也向服务注册中心注册，同时服务消费者获取一份服务注册列表的信息，该列表包含了所有向服务注册中心注册的服务信息。获取服务注册列表信息后，服务消费者就知道服务提供者的IP地址，可以通过HTTP远程调度来消费服务提供者的服务。

本案例有多个Spring Boot工程，为了方便管理，采用Maven多Module的结构，主Maven如下：
```
<modules>
    <module>eureka-client</module>
    <module>eureka-server</module>
</modules>
```
### 一、编写Eureka Server
1、创建工程eureka-server，在工程中添加依赖：
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```
2、在配置文件application.yml添加Eureka Server相关配置，默认情况下，Eureka Server会向自己注册，可以通过配置eureka.client.registerWithEureka和eureka.client.fetchRegistry为false，防止自己注册自己。示例如下：
```
server:
  port: 8521
eureka:
  instance:
    hostname: localhost
  client:
    registerWithEureka: false
    fetchRegistry: false
    serviceUrl:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
```
3、在工程的启动类加上@EnableEurekaServer注解，开启Eureka Server功能。代码如下：
```
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EurekaServerApplication.class, args);
	}
}
```

此时启动工程eureka-server，浏览器访问Eureka Server主页http://localhost:8521/ 发现没有任何注册的实例。

### 二、编写Eureka Client
1、创建工程eureka-client，在工程中添加依赖：
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```
2、在配置文件配置Eureka Client相关配置，需配置程序名、端口、服务注册地址。配置如下：
```
server:
  port: 8522
spring:
  application:
    name: eureka-client
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8521/eureka/
```
3、在启动类加上注解@EnableEurekaClient开启Eureka Client功能。代码如下：
```
@SpringBootApplication
@EnableEurekaClient
public class EurekaClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(EurekaClientApplication.class, args);
	}
}
```

启动Eureka-Client工程，控制台会打印出注册信息，同时再访问Eureka Server主页时会显示多了要给注册实例。



