## Chapter 9 路由网关Spring Cloud Zuul
====================================================================

### 为什么需要Zuul
Zuul作为路由网关组件，在微服务架构中有着非常重要的作用，主要体现在以下6个方面：
+ Zuul、Ribbon以及Eureka相结合，可以实现智能路由和负载均衡的功能，Zuul能够将请求流量按某种策略分发到集群部署的多个服务实例。
+ 网关将所有服务的API接口统一聚合，并统一对外暴露，屏蔽了内部各服务之间复杂的相互调用。同时，这样做也保护了内部微服务单元的API接口，防止其被外界直接调用，导致服务的敏感信息对外暴露。
+ 网关服务可以做用户身份认证和权限认证，防止非法请求操作API接口，对服务器起到保护作用。
+ 网关可以实现监控功能，实时日志输出，对请求进行记录。
+ 网关可以用来实现流量监控，在高流量的情况下，对服务进行降级。
+ API接口从内部服务剥离出来，方便做测试。

### Hystrix的设计原则
总的来说，Hystrix的设计原则如下：
+ 防止单个服务的故障耗尽整个服务的Servlet容器（例如Tomcat）的线程资源。
+ 快速失败机制，如果某个服务出现了故障，则调用该服务的请求快速失败，而不是线程等待。
+ 提供回退（fallback）方案，在请求发生故障时，提供设定好的回退方案。
+ 使用熔断机制，防止故障扩散到其他服务。
+ 提供熔断器的监控组件Hystrix Dashboard，可以实时监控熔断器的状态。

### Hystrix的工作机制
首先，当服务的某个API接口的失败次数在一定时间内小于设定的阈值时，熔断器处于关闭状态，该API接口正常提供服务。当该API接口处理请求的失败次数大于设定的阈值时，Hystrix判定该API接口出现了故障，打开熔断器，这时请求该API接口会执行快速失败的逻辑（即fallback回退的逻辑），不执行业务逻辑，请求的线程不会处于阻塞状态。处于打开状态的熔断器，一段时间后会处于半打开状态，并将一定数量的请求执行正常逻辑，剩余的请求会执行快速失败。若执行正常逻辑的请求失败了，则熔断器继续打开，若成功了，则熔断器关闭，这样熔断器就有了自我修复的能力。本案例包括工程结构为：
```
<modules>
	<module>eureka-client</module>
	<module>eureka-server</module>
	<module>eureka-feign-client</module>
	<module>eureka-ribbon-client</module>
	<module>eureka-monitor-client</module>
</modules>
```

### 在RestTemplate和Ribbon上使用熔断器
1、工程eureka-ribbon-client在例子6-3基础上添加Hystrix的起步依赖```spring-cloud-starter-netflix-hystrix```，并在启动类上添加@EnableHystrix注解开启Hystrix熔断器功能。

2、修改RibbonService类，在hi()方法上添加@HystrixCommand注解启用熔断器功能，其中fallbackMethod为处理回退（fallback）逻辑的方法。在熔断器打开的状态下，会执行fallback逻辑。fallback逻辑最好是返回一些静态的字符串，不需要处理复杂的逻辑，也不需要远程调度其他服务，这样方便执行快速失败，释放线程资源。如果一定要在fallback逻辑中远程调用其他服务，最好在远程调度其他服务时也加上熔断器，本案例的fallback逻辑为执行hiError()方法直接返回一个字符串。代码如下：
```
Service
public class RibbonService {

    @Autowired
    RestTemplate restTemplate;

    @HystrixCommand(fallbackMethod = "hiError")
    public String hi(String name) {
        return restTemplate.getForObject("http://eureka-client/hi?name="+name,String.class);
    }

    public String hiError(String name) {
        return "hi,"+name+",sorry,error!";
    }
}
```
3、依次启动eureka-server、eureka-client、和eure-ribbon-client后，端口分别为8881、8882、8883，在浏览器访问http://localhost:8883/hi ，显示：
```
hi cqf,i am from port:8882
```
关闭eureka-client，即它处于不可用状态，此时eure-ribbon-client无法调用eureka-client的"/hi"接口，再次访问http://localhost:8883/hi ，显示：
```
hi,cqf,sorry,error!
```
由此可见，调用eureka-client的"/hi"接口会进入RibbonService类的hi()方法，由于eureka-client没有响应，判定eureka-client不可用，开启了熔断器，进入fallbackMethod的逻辑，也即执行hiError()方法。

### 在Feign上使用熔断器
1、由于Feign的起步依赖已经引入了Hystrix的依赖，所以在Feign使用Hystrix不需要引入Hystrix的起步依赖，只需要在配置文件中配置开启Hystrix功能。如下：
```
feign:
  hystrix:
    enabled: true
```
2、在6-3基础上修改工程eureka-feign-client，在@FeignClient注解的fallback配置上加上快速失败的处理类。该处理类是作为Feign熔断器的逻辑处理类，必须实现被@FeignClient修饰的接口。本例的HiHystrix实现了接口EurekaClientFeign，最后需要以Spring Bean的形式注入IoC容器中。代码如下：
```
@FeignClient(value = "eureka-client",configuration = FeignConfig.class,fallback = HiHystrix.class)
public interface EurekaClientFeign {
    @GetMapping(value = "/hi")
    String sayHiFromClientEureka(@RequestParam(value = "name") String name);
}
```
HiHystrix作为熔断器的处理类，需要实现EurekaClientFeign接口，并需要在接口方法sayHiFromClientEureka()里写处理熔断的具体逻辑，同时需要在HiHystrix类上加@Component注解，注入IoC容器中。代码如下：
```
@Component
public class HiHystrix implements EurekaClientFeign {
    @Override
    public String sayHiFromClientEureka(String name) {
           return "hi,"+name+",sorry,error!";
    }
}
```
3、依次启动eureka-server、eureka-client、和eureka-feign-client后，端口分别为8881、8882、8884，在浏览器访问http://localhost:8884/hi ，显示：
```
hi cqf,i am from port:8882
```
关闭eureka-client，即它处于不可用状态，此时eureka-feign-client无法调用eureka-client的"/hi"接口，再次访问http://localhost:8884/hi ，显示：
```
hi,cqf,sorry,error!
```

### 使用Hystrix Dashboard监控熔断器的状态
应用整合Hystrix，应包含```spring-boot-starter-actuator```依赖，就会存在一个/actuator/hystrix.stream 端点，用来监控Hystrix Command。当被@HystrixCommand 注解的方法被调用时，就会产生监控信息，并暴露到该端点中。当然，该端点默认是不会暴露的， springboot2.x使用了endpoint，需使用如下配置将其暴露。
```
management:
  endpoints:
    web:
      exposure:
        include: 'hystrix.stream'
```
这样就可以了，但是actuator/health就无法访问了，所以还可以选择全部放开。
```
management:
  endpoints:
    web:
      exposure:
        include: '*'
```
至此，我们已可通过/actuator/hystrix.stream 端点观察Hystrix运行情况，但文字形式的监控数据很不直观。现实项目中一般都需要一个可视化的界面，这样才能迅速了解系统的运行情况。Hystrix提供了一个轮子——Hystrix Dashboard，它的作用只有一个，那就是将文字形式的监控数据转换成图表展示。

1、使用Hystrix Dashboard需要添加依赖：
```
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
</dependency>

<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
</dependency>
```
需要说明的是，Feign也需要，因为Feign自带的Hystrix依赖不是起步依赖。

2、在程序启动类添加注解@EnableHystrix、@EnableHystrixDashboard(测试发现Feign也需要@EnableHystrix注解)，开启熔断器监控功能。
启动工程eureka-server、eureka-client、和eure-ribbon-client、eureka-feign-client后，端口分别为8881、8882、8883、8884，访问浏览器http://localhost:8883/hystrix 和 http://localhost:8884/hystrix ，都能看到页面：
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image2.png)
将上文的/actuator/hystrix.stream 端点的地址贴到图中，并指定Title，然后点击Monitor Stream 按钮，即可看到类似如下的图表：
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image3.png)

### 使用Turbine聚合监控
在使用Hystrix Dashboard组件监控服务的熔断器状况时，每个服务都有一个Hystrix Dashboard主页，当服务数量很多时，监控非常不便。为了同时监控多个服务的熔断器状况，Netflix开源了另一个组件Turbine。Turbine用于聚合多个Hystrix Dashboard，将多个Hystrix Dashboard组件的数据放在一个页面展示，进行集中监控。

新建工程eureka-monitor-client，添加依赖```spring-cloud-starter-netflix-turbine```，启动程序添加注解@EnableTurbine，配置如下：
```
spring:
  application:
    name: service-turbine
server:
  port: 8885
turbine:
  aggregator:
    clusterConfig: default
  appConfig: eureka-ribbon-client,eureka-feign-client
  clusterNameExpression: new String("default")
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8881/eureka/
```
这样，Tubine即可聚合eureka-ribbon-client,eureka-feign-client两个服务的/actuator/hystrix.stream 信息，并暴露在http://localhost:8885/turbine.stream ，将该地址贴到http://localhost:8883/hystrix 或 http://localhost:8884/hystrix 的Hystrix Dashboard上，即可看到类似如下的图表：
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image4.png)

注: 如果界面一直提示loading，那么是因为没有进行请求访问，只需在浏览器上输入请求，然后刷新该界面就可以进行查看了。




