## Chapter 8-7 熔断器Hystrix + 监控Hystrix Dashboard
====================================================================

### 什么是Hystrix
在分布式系统中，服务和服务之间的依赖错综复杂，一种不可避免的情况就是某些服务会出现故障，导致依赖于它们的其他服务出现远程调度的线程阻塞。Hystirx是Netfilx公司开源的一个项目，它提供了熔断器功能，能够阻止分布式系统中出现联动故障。Hystrix是通过隔离服务的访问点阻止联动故障的，并提供了故障的解决方案，从而提高了整个分布式系统的弹性。

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



常见的负载均衡有两种方式：一是独立进程单元，通过负载均衡策略，将请求转发到不同的执行单元，例如Nginx；另一种是将负载均衡逻辑以代码的形式封装到服务消费者的客户端上，服务消费者客户端维护了一份服务提供者的信息列表，有了信息列表，通过负载均衡策略将请求分摊给多个服务提供者，从而达到负载均衡的目的。

Ribbon是Netfilx公司开源的一个负载均衡的组件，它属于上述的第二种方式，是将负载均衡逻辑封装在客户端中，并运行在客户端的进程里。在Spring Cloud构建的微服务系统中，Ribbon作为服务消费者的负载均衡器，有两种使用方式：一种是和RestTemplate相结合；另一种是和Feign相结合。Feign已经默认集成了Ribbon。

本案例包含的工程：
```
<modules>
  <module>eureka-client</module>
  <module>eureka-server</module>
  <module>eureka-ribbon-client</module>
  <module>ribbon-client</module>
  <module>eureka-feign-client</module>
</modules>
```
### 使用RestTemplate消费服务
RestTemplate是Spring Resources中一个访问第三方RESTful API接口的网络请求框架。RestTemplate是用来消费REST服务的，所以RestTemplate的主要方法都与REST的HTTP协议的一些方法紧密相连，例如POST、PUT、DELETE、GET等，所以用RestTemplate很容易构建RESTful API。它支持XML、JSON数据格式，默认实现了序列化，可以自动将JSON字符串转换为实体。代码例如eureka-ribbon-client工程的RestTestController类：
```
@RestController
public class RestTestController {

    @GetMapping("/testRest")
    public String testRest(){
        RestTemplate restTemplate=new RestTemplate();
        return restTemplate.getForObject("https://www.baidu.com/",String.class);
    }
}

```
上述代码getForObject()方法可以获取网页Html代码，并在"/testRest"接口返回该网页的Html字符串。

### 使用RestTemplate和Ribbon来消费服务
1、服务注册中心

eureka-server工程作为服务注册中心，端口为8631；

2、服务提供者

eureka-client工程作为服务提供者，向服务注册中心注册，提供的服务如下：
```
@RestController
public class HiController {
    
    @Value("${server.port}")
    String port;
    @GetMapping("/hi")
    public String home(@RequestParam String name) {
        return "hi "+name+",i am from port:" +port;
    }
}
```

3、服务消费者

（1）在工程eureka-ribbon-client添加Ribbon的起步依赖```spring-cloud-starter-netflix-ribbon```，并向服务注册中心注册。

（2）写一个Ribbon配置类：通过向Ioc容器注入一个RestTemplate的Bean，并在这个Bean上加上@LoadBalanced注解，此时RestTemplate就结合Ribbon开启了负载均衡功能。代码如下：
```
@Configuration
public class RibbonConfig {

    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```
（3）写一个RibbonService类，注入一个RestTemplate的Bean，在hi()方法中用RestTemplate的Bean调用eureka-client的API接口，URL不需要使用硬编码（例如IP），只需要写服务名eureka-client即可。代码如下：
```
@Service
public class RibbonService {

    @Autowired
    RestTemplate restTemplate;

    public String hi(String name) {
        return restTemplate.getForObject("http://eureka-client/hi?name="+name,String.class);
    }
}
```
（4）写一个RibbonController类，加上@RestController开启RestController功能，写一个"/hi" GET类型接口，调用RibbonService类的hi()方法。代码如下：
```
@RestController
public class RibbonController {

    @Autowired
    RibbonService ribbonService;
    @GetMapping("/hi")
    public String hi(@RequestParam(required = false,defaultValue = "cqf") String name){
        return ribbonService.hi(name);
    }
}
```

4、启动eureka-server工程，端口为8631，启动两个eureka-client实例，端口分别为8634、8635，启动eureka-ribbon-client工程，端口为8632，浏览器多次访问http://localhost:8632/hi?name=cqf ，会轮流显示：
```
hi cqf,i am from port:8634
hi cqf,i am from port:8635
```

### 直接使用LoadBalancerClient来实现负载均衡
其它一样，在服务消费工程eureka-ribbon-client，增加一个接口"/testRibbon"，在testRibbon()方法中通过LoadBalancerClient的choose()方法获取服务实例的信息。代码如下：
```
@RestController
public class RibbonController {
    @Autowired
    private LoadBalancerClient loadBalancer;

    @GetMapping("/testRibbon")
    public String  testRibbon() {
        ServiceInstance instance = loadBalancer.choose("eureka-client");
      //  URI uri = URI.create(String.format("http://%s:%s", instance.getHost(), instance.getPort()));
        return instance.getHost()+":"+instance.getPort();
    }
}
```
多次访问http://localhost:8632/testRibbon ，浏览器轮流显示：
```
LAPTOP-66FA2IS3:8634
LAPTOP-66FA2IS3:8635
```
负载均衡器LoadBalancerClient是从eureka-client获取服务注册列表信息的，并将服务注册信息缓存，在调用Choose()方法时，根据负载均衡策略选择一个服务实例。LoadBalancerClient也可以不从eureka-client获取注册列表信息，这时需要自己维护一份注册列表信息。查看工程ribbon-client的配置：
```
stores:
  ribbon:
    listOfServers: example.com,google.com
ribbon:
  eureka:
   enabled: false
server:
  port: 8633
```
配置了一个stores服务，通过listOfServers配置两个不同URL地址的服务实例，并通过配置ribbon.eureka.enabled=false来禁止调用Eureka Client获取注册列表。

启动工程，端口为8633，多次访问http://localhost:8633/testRibbon ，浏览器轮流显示：
```
example.com:80
google.com:80
```

###声明式调用Feign
Feign受Retrofit、JAXRS-2.0和WebSocket的影响，采用了声明式API接口风格，将Java Http客户端绑定到它的内部。eureka-feign-client工程是Feign调用的一个例子。

1、首先添加Feign的起步依赖```spring-cloud-starter-openfeign```，并向注册中心注册。在程序的启动类加上@EnableFeignClients开启Feign Client功能。代码如下：
```
@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
public class EurekaFeignClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(EurekaFeignClientApplication.class, args);
	}
}
```
2、实现一个简单的Feign Client：新建一个EurekaClientFeign接口，在接口上加@FeignClient注解来声明一个Feign Client，其中value为远程调用其他服务的服务名，FeignConfig.class为Feign Client的配置类。在EurekaClientFeign内部有一个sayHiFromClientEureka()方法，该方法通过Feign调用eureka-client服务的"/hi" API接口。代码如下：
```
@FeignClient(value = "eureka-client",configuration = FeignConfig.class)
public interface EurekaClientFeign {
    @GetMapping(value = "/hi")
    String sayHiFromClientEureka(@RequestParam(value = "name") String name);
}
```
3、编写Feign Client的配置类：新建配置类FeignConfig，加上@Configuration注解，并注入一个名为feignRetryer的Retryer的Bean。注入该Bean后，Feign在远程调用失败后会进行重试。代码如下：
```
@Configuration
public class FeignConfig {

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, SECONDS.toMillis(1), 5);
    }
}
```
4、在Server层的HiService类注入一个EurekaClientFeign的Bean，通过该Bean调用sayHiFromClientEureka()方法。代码如下：
```
@Service
public class HiService {

    @Autowired
    EurekaClientFeign eurekaClientFeign;
    public String sayHi(String name){
        return  eurekaClientFeign.sayHiFromClientEureka(name);
    }
}
```
5、在Controller层的HiController类写一个API接口"/hi"，该接口调用自动注入的HiService的sayHi()方法，HiService通过EurekaClientFeign远程调用eureka-client服务的API接口"/hi"。代码如下：
```
@RestController
public class HiController {
    @Autowired
    HiService hiService;
    @GetMapping("/hi")
    public String sayHi(@RequestParam( defaultValue = "cqf",required = false)String name){
        return hiService.sayHi(name);
    }
}
```
6、启动工程eureka-feign-client，端口为8636，多次访问http://localhost:8636/hi?name=cqf ，浏览器轮流显示：
```
hi cqf,i am from port:8634
hi cqf,i am from port:8635
```

### 源码剖析
Ribbon的负载均衡主要是通过LoadBalancerClient来实现的，而LoadBalancerClient具体交给了ILoadBalancer来处理，ILoadBalancer通过配置IRule、IPing等，向Eureka Client获取注册列表的信息，默认每10秒向Eureka Client发送一次Ping检查是否需要更新服务的注册列表信息。在得到服务的注册信息后，ILoadBalancer根据IRule的策略进行负载均衡。而RestTemplate加上@LoadBalance注解后在远程调度时能够负载均衡，主要是维护了一个被@LoadBalance注解的RestTemplate列表，并给该列表中的RestTemplate对象添加了拦截器。在拦截器方法中，将远程调度方法交给了Ribbon的负载均衡器LoadBalancerClient去处理，从而达到了负载均衡的目的。

Feign的实现过程：

（1）首先通过@EnableFeignClient注解开启FeignClient功能。只有这个功能开启，才会在程序启动时开启对@FeignClient注解的包扫描。

（2）根据Feign的规则实现接口，并在接口上面加上@FeignClient注解。

（3）程序启动后，会进行包扫描，扫描所有的@FeignClient注解的类，并将这些信息注入IoC容器中。

（4）当接口的方法被调用时，通过JDK的代理生成具体的RequestTemplate模板对象。

（5）根据RequestTemplate生成Http请求的Request对象。

（6）Request对象交给Client去处理，其中Client的网络请求框架可以是HttpURLConnection、HttpClient和OKHttp。

（7）最好Client被封装到LoadBalancerClient类，这个类结合Ribbon做到了负载均衡。

