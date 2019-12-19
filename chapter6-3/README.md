## Chapter 6-3 使用RestTemplate和Ribbon来消费服务
====================================================================

常见的负载均衡有两种方式：一是独立进程单元，通过负载均衡策略，将请求转发到不同的执行单元，例如Nginx；另一种是将负载均衡逻辑以代码的形式封装到服务消费者的客户端上，服务消费者客户端维护了一份服务提供者的信息列表，有了信息列表，通过负载均衡策略将请求分摊给多个服务提供者，从而达到负载均衡的目的。

Ribbo是Netfilx公司开源的一个负载均衡的组件，它属于上述的第二种方式，是将负载均衡逻辑封装在客户端中，并运行在客户端的进程里。在Spring Cloud构建的微服务系统中，Ribbon作为服务消费者的负载均衡器，有两种使用方式：一种是和RestTemplate相结合；另一种是和Feign相结合。Feign已经默认集成了Ribbon。

本案例包含的工程：
```
<modules>
  <module>eureka-client</module>
  <module>eureka-server</module>
  <module>eureka-ribbon-client</module>
  <module>ribbon-client</module>
</modules>
```

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
（4）写一个RibbonController类，加上@RestController开启RestController功能，写一个“/hi”GET类型接口，调用RibbonService类的hi()方法。代码如下：
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

