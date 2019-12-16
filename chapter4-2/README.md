## Chapter 4 Examples
====================================================================

### 一、 Spring Boot配置文件详解

1、自定义属性

可以在配置文件application.yml自定义一组属性，例如：
```
my:
 name: cqf
 age: 12
```
如果要读取配置文件application.yml的属性值，只需在变量上加@Value("${属性名}")注解，就可以将属性值赋给变量，例如类MiyaController：
```
@RestController
public class MiyaController {

    @Value("${my.name}")
    private String name;
    @Value("${my.age}")
    private int age;

    @RequestMapping(value = "/miya")
    public String miya(){
        return name+":"+age;
    }
}
```
2、将配置文件的属性赋给实体类

当有很多配置属性时，如果逐个地读取属性会非常麻烦，通常的做法会把这些属性名作为变量名来创建一个JavaBean的变量，并将属性值赋给JavaBean变量的值。例如用配置文件创建实体类ConfigBean：
```
@ConfigurationProperties(prefix = "my")
@Component
public class ConfigBean {
    private String name;
    private int age;
    private int number;
    private String uuid;
    private int max;
    private String value;
    private String greeting;
    //省略了getter setter ...
}
```
注解@ConfigurationProperties表明是一个配置属性类，prefix属性表明会取配置文件application.yml的my属性组的内容构造类，同时注解@Component表明会被自动扫描注入IoC容器中。在类LucyController使用配置属性类ConfigBean：
```
@RestController
@EnableConfigurationProperties({ConfigBean.class})
public class LucyController {
    @Autowired
    ConfigBean configBean;
    @RequestMapping(value = "/lucy")
    public String lucy(){
        return configBean.getGreeting()+" >>>>"+configBean.getName()+" >>>>"+ configBean.getUuid()+" >>>>"+configBean.getMax();
    }
}
```
注解@EnableConfigurationProperties使得被@ConfigurationProperties注解的beans自动被Environment属性配置。

3、自定义配置文件



二、接口工程

dubbo-api工程里有一个接口IHello

三、服务端

dubbo-server工程添加Dubbo和接口工程的依赖，实现IHello接口，并用注解@Service暴露服务，在application.properties文件中配置Dubbo。

四、消费方

dubbo-client工程添加Dubbo和接口工程的依赖，编写远程调用Dubbo服务，@Reference注解可以用于生成远程服务代理，在application.properties文件中配置跟服务端一样的服务注册中心。

五、网关

模块之间互相调用时，为了降低由网络波动带来的不确定性因素并提升系统安全性，生产环境中所有模块一般都运行在内网环境中，并单独提供一个工程作为网关服务，开放固定端口代理所有模块提供的服务，并通过拦截器验证所有外部请求以达到权限管理的目的。外部应用可能是App、网站或桌面客户端，为了达到通用性，网关服务一般为web服务，通过HTTP协议提供RESTful风格的API接口。

dubbo-gateway工程添加Dubbo和接口工程的依赖，并添加spring-boot-starter-web依赖（支持Web应用开发，包含Tomcat和Spring-MVC），并在application.properties文件中配置Dubbo，编写用于调用Dubbo服务的控制器，编写验证逻辑RequestInterceptor，配置拦截器WebAppConfiguration。
