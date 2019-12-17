## Chapter 4-7 Spring Boot整合Swagger2，搭建RESTful API在线文档
====================================================================

Swagger，中文“拽”的意思，它是一个功能强大的在线API文档的框架，当前使用的版本是2.X，所以称为Swagger2。

### 一、引入依赖
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


