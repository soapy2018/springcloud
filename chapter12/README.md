# Chapter 12 微服务监控 Spring Boot Admin
====================================================================

## Spring Boot Admin简介
Spring Boot Admin是一个开源社区项目，用于管理和监控SpringBoot应用程序。 应用程序作为Spring Boot Admin Client向为Spring Boot Admin Server注册（通过HTTP）或使用SpringCloud注册中心（例如Eureka，Consul）发现。 UI是的AngularJs应用程序，展示Spring Boot Admin Client的Actuator端点上的一些监控。常见的功能或者监控如下：
+ 显示健康状况
+ 显示详细信息，例如 
  - JVM和内存指标
  - micrometer.io指标
  - 数据源指标
  - 缓存指标
+ 显示构建信息编号
+ 关注并下载日志文件
+ 查看jvm系统和环境属性
+ 查看Spring Boot配置属性
+ 支持Spring Cloud的postable / env-和/ refresh-endpoint
+ 轻松的日志级管理
+ 与JMX-beans交互
+ 查看线程转储
+ 查看http跟踪
+ 查看auditevents
+ 查看http-endpoints
+ 查看计划任务
+ 查看和删除活动会话（使用spring-session）
+ 查看Flyway / Liquibase数据库迁移
+ 下载heapdump
+ 状态变更通知（通过电子邮件，Slack，Hipchat，…）
+ 状态更改的事件日志（非持久性）

## 快速开始
### 创建Spring Boot Admin Server
1、在工程admin-server引入admin-server的起来依赖和web的起步依赖，代码如下：
```
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>spring-boot-admin-starter-server</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
2、然后在工程的启动类AdminServerApplication加上@EnableAdminServer注解，开启AdminServer的功能。
3、在工程的配置文件application.yml中配置程序名和程序的端口，代码如下：
```
spring:
  application:
    name: admin-server
server:
  port: 8888
```
这样Admin Server就创建好了。

### 创建Spring Boot Admin Client
1、在eureka-client-one、eureka-client-two工程的pom文件引入admin-client的起步依赖和web的起步依赖，代码如下：
```
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>spring-boot-admin-starter-client</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
2、在工程的配置文件中配置应用名和端口信息，以及向admin-server注册的地址为 http://localhost:8888， 最后暴露自己的actuator的所有端口信息，具体配置如下：
```
server:
  port: 8122
spring:
  application:
    name: admin-client
  ###没有集成注册中心时，需要配置admin server地址
  admin:
    client:
      url: http://localhost:8888
  ###没有集成注册中心时，需要配置admin server地址

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: ALWAYS
```

依次启动两个工程，在浏览器上输入localhost:8888 ，浏览器显示的界面如下：
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image13.png)

点击wallboard，可以查看admin-client具体的信息，比如内存状态信息等等：
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image14.png)

## Spring boot Admin结合SC注册中心使用
### 搭建注册中心
工程eureka-server作为注册中心，跟前面例子类似。它的端口为8121。

### 结合SC搭建admin-server
1、修改admin-server工程，pom文件增加引入eureka-client的起步依赖。

2、修改配置，向注册中心注册，注册地址为http://localhost:8121 ，最后将actuator的所有端口暴露出来，配置如下：
```
server:
  port: 8888

spring:
  application:
    name: admin-service

eureka:
  client:
    registryFetchIntervalSeconds: 5
    serviceUrl:
      defaultZone: ${EUREKA_SERVICE_URL:http://localhost:8121}/eureka/
  instance:
    leaseRenewalIntervalInSeconds: 10
    health-check-url-path: /actuator/health
```

3、在工程的启动类AdminServerApplication加上@EnableAdminServer注解，开启admin server的功能，加上@EnableDiscoveryClient注解（跟注解@EnableEurekaClient貌似效果一样）开启eurke client的功能。代码如下：
```
@SpringBootApplication
@EnableAdminServer
@EnableDiscoveryClient
public class AdminServerApplication {

    public static void main(String[] args) {
        SpringApplication.run( AdminServerApplication.class, args );
    }

}
```
### 结合SC搭建admin-client
1、修改admin-client的pom文件增加eureka-client的起步依赖，并引用actuator的起步依赖如下：
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>

<!--在集成SC后必须添加actuator依赖-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```
2、修改配置文件，去掉admin-server的注册地址，转而向注册中心注册。
```
server:
  port: 8122

spring:
  application:
    name: admin-client

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: ALWAYS

eureka:
  client:
    registryFetchIntervalSeconds: 5
    serviceUrl:
      defaultZone: ${EUREKA_SERVICE_URL:http://localhost:8121}/eureka/
  instance:
    leaseRenewalIntervalInSeconds: 10
    health-check-url-path: /actuator/health
    metadata-map:
      startup: ${random.int}    #needed to trigger info and endpoint update after restart
```
在启动类加上@EnableEurekaClient注解，开启Eureka Client的功能。

依次启动eureka-server、admin-server、dmin-client，访问http://localhost:8888/ ，效果一样，只是多了一个服务。

重点：admin会自己拉取Eureka Server上注册的服务信息，主动去发现注册client。这也是唯一区别之前手动注册的地方，就是client端不需要admin-client的依赖，也不需要配置 admin地址了，一切全部由 admin-server自己实现。这样的设计对环境变化很友好，不用改了admin-server后去改所有app 的配置了。








