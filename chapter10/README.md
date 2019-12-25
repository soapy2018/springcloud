## Chapter 10 配置中心Spring Cloud Config
====================================================================

本章主要讲述Spring Cloud的组件——分布式配置中心Spring Cloud Config，分为以下四个方面：
+ Config Server从本地读取配置文件。
+ Config Server从远程Git仓库读取配置文件。
+ 构建高可用Config Server集群。
+ 使用Spring Cloud Bus刷新配置。

### Config Server从本地读取配置文件
Config Server可以从本地仓库读取配置文件，也可以从远程Git仓库读取。本地仓库是指将所有的配置文件统一写在Config Server工程目录下。Config Server暴露Http API接口，Config Client通过调用Config Server的Http API接口来读取配置文件。

1、config-server工程添加起步依赖```spring-cloud-config-server```，注意这里不需要```spring-cloud-starter-netflix-eureka-client```依赖。

2、在程序的启动类添加@EnableConfigServer注解开启Config Server功能，注意这里不需要@EnableEurekaClient注解。

3、config-server工程配置文件，通过spring.profiles.active=native来配置Config Server从本地读取配置，读取配置的路径为classpath下的shared目录。配置如下：
```
server:
  port: 8102
# native 从本地读取配置文件
spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/shared
  profiles:
    active: native
```
4、在config-server工程的Resources目录下建一个shares文件夹，用于存放本地配置文件。在shared目录下，新建一个config-client-dev.yml文件，用作eureka-client工程的dev（开发环境）的配置文件。在config-client-dev.yml配置文件中，指定程序的端口号为8109，并定义一个值为foo version 1的变量foo。如下：
```
server:
  port: 8109
  
foo: foo version 1
```
5、在config-client工程引入依赖```spring-cloud-starter-config```和```spring-boot-starter-web```，并添加注解@RestController开启web功能。

6、在配置文件bootstrap.yml中做程序的配置（注意bootstrap相对于application具有优先的执行顺序）。bootstrap.yml指定了程序名为config-client，向Url地址为 http://localhost:8101 的Config Server读取配置文件，如果没有读取成功，则执行快速失败（fail-fast）。变量spring.applicatition.name和变量spring.profiles.active，两者以“ - ”相连，构成了向Config Server读取的配置文件名，所以config-client在配置中心读取的配置文件名为config-client-dev.yml文件。配置如下：
```
#从Config Server获取配置文件
spring:
  application:
    name: config-client
  profiles:
    active: dev
  cloud:
    config:
     uri: http://localhost:8102
     fail-fast: true
```

7、启动config-server、config-client工程，config-server端口为8101，发现config-client的端口为8109，访问http://localhost:8109/foo ，显示：
```
foo version 1
```

### Config Server从远程Git仓库读取配置文件
1、修改Config Server的配置文件如下：
```
# remote git 从远程git仓库读取配置文件
spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        git:
          uri: https://github.com/soapy2018/SpringCloudConfig
          searchPaths: respo
          username:
          password:
      label: master
```
其中，uri为远程Git仓库地址，searchPaths为搜索远程仓库的文件夹地址，username和password为Git仓库的登录名和密码。如果是私人仓库需要登录名和密码，如果是公开仓库则可以不填。label为Git仓库的分支名，本例从master读取。

2、将配置文件config-client-dev.yml上传到我自己新建的仓库 https://github.com/soapy2018/SpringCloudConfig 的respo文件夹下。配置文件内容如下：
```
server:
  port: 8108

foo: foo version 108
```
3、重新启动config-server、config-client工程，发现config-client的端口为8108，访问http://localhost:8109/foo ，显示：
```
foo version 108
```

### 构建高可用Config Server集群
当服务实例很多时，所有的服务实例需要同时从配置中心Config Server读取配置文件，这时可以考虑将配置中心Config Server做成一个微服务，并且将其集群化，从而达到高可用。本案例Config Server和Config Client向Eureka Server注册，且将Config Server多实例集群部署。

1、增加一个服务注册中心eureka-server。

2、改造config-server：作为Eureka Client，添加依赖```spring-cloud-starter-netflix-eureka-client```；在启动类添加注解@EnableEurekaClient；在配置文件添加服务注册地址，如下：
```
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8102/eureka/
```
3、改造config-client：同样作为Eureka Client添加依赖和注解。在配置文件指定服务注册中心地址，并向serviceId为config-server的服务读取配置文件，如下：
```
#Config Server作为eureka client集群部署时，从Config Server获取配置文件
spring:
  application:
    name: config-client
  profiles:
    active: dev
  cloud:
    config:
      fail-fast: true
      discovery:
        enabled: true
        serviceId: config-server
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8101/eureka/
```

4、启动eureka-server，启动两个config-server实例，端口分别为8102、8103，为了测试，我们可以让一个实例用本地配置，一个实例用Git仓库配置，然后多次启动eureka-client，从控制台可以发现它会轮流从 http://localhost:8102 和 http://localhost:8103 的config-server读取配置文件，并做了负载均衡。

### 使用Spring Cloud Bus刷新配置
Spring Cloud Bus是用轻量的消息代理将分布式的节点连接起来，可以用于广播配置文件的更改或者服务的监控管理。一个关键的思想是，消息总线可以为微服务做监控，也可以实现应用程序之间相互通信。Spring Cloud Bus可选的消息代理组件包括RabbitMQ、AMQP和Kafka等。本案例使用RabbitMQ作为Spring Cloud的消息组件去刷新更改微服务的配置文件。

为什么需要用Spring Cloud Bus去刷新配置呢？

如果有几十个微服务，而每一个服务又是多实例，当更改配置时，需要重新启动多个微服务实例，会非常麻烦。Spring Cloud Bus的一个功能就是让这个过程变得简单，当远程Git仓库的配置更改后，只需要向某个微服务实例发送一个Post请求，通过消息组件通知其他微服务实例重新拉取配置文件。

改造config-client工程：1、添加用RabbitMQ实现的Spring Cloud Bus的起步依赖```spring-cloud-starter-bus-amqp```和```spring-boot-starter-actuator```，并在程序启动类添加注解@RefreshScope，只有加上了该注解，才会在不重启服务的情况下更新配置。2、配置文件添加RabbitMQ相关配置，其中host为RabbitMQ服务器的IP地址，port为RabbitMQ服务器的端口，username和password为RabbitMQ服务器的用户名和密码，同时需要暴露端点bus-refresh，配置如下：
```
#消息组件RabbitMQ配置
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    publisher-confirms: true
    virtual-host: /

management:
  endpoints:
    web:
      exposure:
        include: bus-refresh
```
Git配置文件如下：
```
foo: foo version 108
```
依次启动eureka-server，启动两个config-server实例（两个实例都配置成从远程Git仓库获取），端口分别为8102、8103，启动两个config-client实例，端口分别为8104、8105（端口在config-client指定，不从Git仓库获取）。启动完成后，在浏览器上访问 http://localhost:8104/foo 或 http://localhost:8105/foo ，浏览器显示：
```
foo version 108
```
更改Git仓库配置文件成：
```
foo: foo version 888
```
通过Postman或者其他工具发送一个post请求http://localhost:8104/actuator/bus-refresh （或者服务的另一个8105端口的实例），请求发送成功，再访问http://localhost:8104/foo 或 http://localhost:8105/foo ，浏览器显示：
```
foo version 888
```
可见通过Postman或者其他工具发送一个post请求刷新配置，由于使用了Spring Cloud Bus，其他服务实例（案例中是8105端口的服务实例）会接收到刷新配置的消息，从而刷新配置。另外“ /actuator/bus/refresh ”API接口也可以指定服务，即使用“ destination ”参数，例如“ /actuator/bus/refresh?destination=config-client:** ”，即刷新服务名为config-client的所有服务实例。

