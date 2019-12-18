## Chapter 5-2 服务注册和发现Eureka
====================================================================

Eureka来源于古希腊词汇，意为“发现了”。在软件领域，Eureka是Netflix在线影片公司开源的一个服务注册与发现的组件。Eureka分为Eureka Server和Eureka Client，Eureka Server为Eureka服务注册中心，Eureka Client为Eureka客户端。Eureka和其他组件，比如负载均衡组件Ribbon、熔断器组件Hystrix、熔断器监控组件Hystrix Dashboard、熔断器聚合监控Turbine，以及网关Zuul组件相互配合，能够轻松实现服务注册和发现、负载均衡、熔断和智能路由等功能，这些组件都是Netflix公司开源的，一起被称为Netflix OSS组件。Netflix OSS组件由Spring Cloud整合为Spring Cloud Netflix组件，它是Spring Cloud架构微服务的核心组件，也是基础组件。Eureka主要包含3种角色：
+ Register Service：服务注册中心，它是一个Eureka Server，提供服务注册和发现的功能。
+ Provider Service：服务提供者，它是要给Eureka Client，提供服务。
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

  
 
在工程中添加依赖：
```
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger2</artifactId>
    <version>2.6.1</version>
</dependency>
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger-ui</artifactId>
    <version>2.6.1</version>
</dependency>
```

### 二、配置Swagger2
写一个配置类Swagger2，添加注解@Configuration表明是一个配置类，同时加注解@EnableSwagger2开启Swagger2的功能。在配置类Swagger2中需要注入一个Docket的Bean，该Bean用apinfo方法初始化文档的描述信息，同时指定包扫描的路径。代码如下：
```
@Configuration
@EnableSwagger2
public class Swagger2 {

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.cqf.web"))
                .paths(PathSelectors.any())
                .build();
    }
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("springboot利用swagger构建api文档")
                .description("简单优雅的restfun风格，https://blog.csdn.net/soapy2010")
                .termsOfServiceUrl("https://blog.csdn.net/soapy2010")
                .version("1.0")
                .build();
    }
}
```
### 三、数据操作层
本例复用4-6 JPA+MySQL

### 四、Web层
在Web层通过GET、POST、DELETE、PUT这四种HTTP方法，构建一组以资源为中心的RESTful风格的API接口。代码如下：
```
RequestMapping("/user")
@RestController
public class UserController {

    @Autowired
    UserService userService;

    @ApiOperation(value="用户列表", notes="用户列表")
    @RequestMapping(value={""}, method= RequestMethod.GET)
    public List<User> getUsers() {
        List<User> users = userService.findAll();
        return users;
    }

    @ApiOperation(value="创建用户", notes="创建用户")
    @RequestMapping(value="", method=RequestMethod.POST)
    public User postUser(@RequestBody User user) {
      return   userService.saveUser(user);

    }
    @ApiOperation(value="获用户细信息", notes="根据url的id来获取详细信息")

    @RequestMapping(value="/{id}", method=RequestMethod.GET)
    public User getUser(@PathVariable Long id) {
        return userService.findUserById(id);
    }

    @ApiOperation(value="更新信息", notes="根据url的id来指定更新用户信息")
    @RequestMapping(value="/{id}", method= RequestMethod.PUT)
    public User putUser(@PathVariable Long id, @RequestBody User user) {
        User user1 = new User();
        user1.setUsername(user.getUsername());
        user1.setPassword(user.getPassword());
        user1.setId(id);
       return userService.updateUser(user1);

    }
    @ApiOperation(value="删除用户", notes="根据url的id来指定删除用户")
    @RequestMapping(value="/{id}", method=RequestMethod.DELETE)
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "success";
    }

    @ApiIgnore//使用该注解忽略这个API
    @RequestMapping(value = "/hi", method = RequestMethod.GET)
    public String  jsonTest() {
        return " hi you!";
    }
}
```

### 运行
启动程序，在浏览器上访问http://localhost:8047/swagger-ui.html    


