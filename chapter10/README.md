## Chapter 10 配置中心Spring Cloud Config
====================================================================
本章主要讲述Spring Cloud的组件——分布式配置中心Spring Cloud Config，分为以下四个方面：
+ Config Server从本地读取配置文件。
+ Config Server从远程Git仓库读取配置文件。
+ 搭建高可用Config Server集群。
+ 使用Spring Cloud Bus刷新配置。

### Config Server从本地读取配置文件
Config Server可以从本地仓库读取配置文件，也可以从远程Git仓库读取。本地仓库是指将所有的配置文件统一写在Config Server工程目录下。Config Server暴露Http API接口，Config Client通过调用Config Server的Http API接口来读取配置文件。

1、config-server工程添加起步依赖```spring-cloud-config-server```，注意这里不需要```spring-cloud-starter-netflix-eureka-client```依赖。

2、在程序的启动类添加@EnableConfigServer注解开启Config Server功能，注意这里不需要@EnableEurekaClient注解。

3、config-server工程配置文件，通过spring.profiles.active=native来配置Config Server从本地读取配置，读取配置的路径为classpath下的shared目录。配置如下：
```
server:
  port: 8101
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
4、在工程的Resources目录下建一个shares文件夹，用于存放本地配置文件。在shared目录下，新建一个config-client-dev.yml文件，用作eureka-client工程的dev（开发环境）的配置文件。在config-client-dev.yml配置文件中，指定程序的端口号为8109，并定义一个值为foo version 1的变量foo。如下：
```
server:
  port: 8109

foo: foo version 1
```

Zuul作为路由网关组件，在微服务架构中有着非常重要的作用，主要体现在以下6个方面：
+ Zuul、Ribbon以及Eureka相结合，可以实现智能路由和负载均衡的功能，Zuul能够将请求流量按某种策略分发到集群部署的多个服务实例。
+ 网关将所有服务的API接口统一聚合，并统一对外暴露，屏蔽了内部各服务之间复杂的相互调用。同时，这样做也保护了内部微服务单元的API接口，防止其被外界直接调用，导致服务的敏感信息对外暴露。
+ 网关服务可以做用户身份认证和权限认证，防止非法请求操作API接口，对服务器起到保护作用。
+ 网关可以实现监控功能，实时日志输出，对请求进行记录。
+ 网关可以用来实现流量监控，在高流量的情况下，对服务进行降级。
+ API接口从内部服务剥离出来，方便做测试。

### Zuul的工作原理
Zuul是通过Servlet来实现的，Zuul通过自定义的ZuulServlet（类似于Spring MVC的DispatchServlet）来对请求进行控制。Zuul的核心是一些列过滤器，可以在Http请求的发起和响应返回期间执行一系列的过滤器。Zuul包括以下4种过滤器：
+ PRE过滤器：它是在请求路由到具体的服务之前执行的，这种类型的过滤器可以做安全验证，例如身份验证、参数验证等。
+ ROUTING过滤器：它用于将请求路由到具体的微服务实例。在默认情况下，它使用Http Client进行网络请求。
+ POST过滤器：它是请求已被路由到具体微服务后执行的。一般情况下，用作收集统计信息、指标，以及响应传输到客户端。
+ ERROR过滤器：它是在其他过滤器发生错误时执行的。

Zuul采取了动态读取、编译和运行这些过滤器。过滤器直接不能直接相互通信，而是通过RequestContext对象来共享数据，每个请求都会创建要给RequestContext对象。Zuul过滤器具有以下关键特性：
+ Type（类型）：Zuul过滤器的类型，这个类型决定了过滤器在请求的哪个阶段起作用，例如Pre、Post阶段等。
+ Execution Order（执行顺序）：规定了过滤器的执行顺序，Order的值越小，越先执行。
+ Criteria（标准）：过滤器执行所需的条件。
+ Action（行动）：如果符合执行条件，则执行Action（即逻辑代码）。

Zuul请求的生命周期：当一个客户端Request请求进入Zuul网关服务时，网关先进入“pre filter”，进行一系列验证、操作或者判断。然后交给“routing filter”进行路由转发，转发到具体的服务实例进行逻辑处理、返回数据。当具体的服务处理完后，最后由“post filter”进行处理，该类型的过滤器处理完后将Response信息返回给客户端。

ZuulServlet是Zuul的核心Servlet，它的作用是初始化ZuulFilter，并编排这些ZuulFilter的执行顺序，它有一个service()方法，定义了过滤器执行的逻辑，其代码如下：
```
  public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        try {
            this.init((HttpServletRequest)servletRequest, (HttpServletResponse)servletResponse);
            RequestContext context = RequestContext.getCurrentContext();
            context.setZuulEngineRan();

            try {
                this.preRoute();
            } catch (ZuulException var12) {
                this.error(var12);
                this.postRoute();
                return;
            }

            try {
                this.route();
            } catch (ZuulException var13) {
                this.error(var13);
                this.postRoute();
                return;
            }

            try {
                this.postRoute();
            } catch (ZuulException var11) {
                this.error(var11);
            }
        } catch (Throwable var14) {
            this.error(new ZuulException(var14, 500, "UNHANDLED_EXCEPTION_" + var14.getClass().getName()));
        } finally {
            RequestContext.getCurrentContext().unset();
        }
    }
```

### Zuul的常见使用方式
Zuul采用了类似于Spring MVC的DispatchServlet来实现，采用的是异步阻塞模型，所以性能比Nginx差。由于Zuul和其他Netflix组件可以相互配合、无缝集成，Zuul很容易就能实现负载均衡、智能路由和熔断器等功能。在大多数情况下，Zuul都是以集群的形式存在的。由于Zuul的横向扩展能力非常好，所以当负载过高时，可以通过添加实例来解决性瓶颈。

一种常见的使用方式是对不同的渠道使用不同的Zuul来进行路由，例如移动端共用一个Zuul网关实例，Web端用另一个Zuul网关实例，其他客户端用另外一个Zuul实例。

另外一种常见的集群是通过Nginx和Zuul互相结合来做负载均衡。暴露在最外面的是Nginx主从双热备进行Keepalive，Nginx经过某种路由策略，将请求路由转发到Zuul集群上，Zuul最终将请求分发到具体的服务上。

### 搭建Zuul服务
1、在eureka-zuul-client添加Zuul的起步依赖```spring-cloud-starter-netflix-zuul```，在启动类添加@EnableZuulProxy注解开启Zuul功能。

2、在配置文件配置Zuul路由。如下：
```
zuul:
  routes:
    hiapi:
      path: /hiapi/**
      serviceId: eureka-client
#      url: http://localhost:8762  #这样写不会做负载均衡
#      serviceId: hiapi-v1
    ribbonapi:
      path: /ribbonapi/**
      serviceId: eureka-ribbon-client
    feignapi:
      path: /feignapi/**
      serviceId: eureka-feign-client
```
其中，zuul.routes.hiapi.path为“ /hiapi/** ”，zuul.routes.hiapi.serviceId为“ eureka-client ”，这两个配置可以将以“ /hiapi ”开头的url路由到eureka-client服务，其中zuul.routes.hiapi的“ hiapi ”是自己定义的，需要指定它的path和serviceId，两者配合使用就可以将指定类型的请求url路由到指定的ServiceId。同理，满足以“ /ribbonapi ”开头的请求url都会被分发到eureka-ribbon-client服务，满足以“ /feignapi ”开头的请求url都会被分发到eureka-feign-client服务。如果某服务存在多个实例，Zuul结合Ribbon会做负载均衡，将请求路由到不同的服务实例。

3、依次启动eureka-server、eureka-client、eureka-ribbon-client、eureka-feign-client、eureka-zuul-client，其中eureka-client启动两个实例，端口分别为8902、8903，eureka-zuul-client端口为5000，在浏览器多次访问http://localhost:5000/hiapi/hi?name=cqf ，交替显示：
```
hi cqf,i am from port:8902
hi cqf,i am from port:8903
```
可见Zuul在路由转发做了负载均衡。同理多次访问http://localhost:5000/feignapi/hi?name=cqf 和 http://localhost:5000/ribbonapi/hi?name=cqf 可以看到一样的内容。

如果不需要用Ribbon做负载均衡，可以指定服务实例的url，例如：
```
zuul:
  routes:
    hiapi:
      path: /hiapi/**
      url: http://localhost:8902  #这样写不会做负载均衡
```
如果想指定url，并且想做负载均衡，那么需要自己维护负载均衡的服务注册列表，例如：
```
zuul:
  routes:
    hiapi:
      path: /hiapi/**
      serviceId: eureka-client
      serviceId: hiapi-v1
  
ribbon:
  eureka:
    enabled: false

hiapi-v1:
  ribbon:
    listOfServers: http://localhost:8762,http://localhost:8763
```
以上配置，将ribbon.eureka.enabled改为false（即Ribbon负载均衡客户端不向Eureka Client获取服务注册列表信息），同时自己维护一份注册列表信息，该注册列表对应的服务名为hiapi-v1（这个名字可以自动有），通过hiapi-v1.ribbon.listOfServers来配置多个负载均衡的url。

### 在Zuul上配置API接口的版本号
如果想给每一个服务的API接口加前缀，例如http://localhost:5000/v1/hiapi/hi?name=cqf ，即在所有的API接口上加一个v1作为版本号。这时需要用到zuul.prefix的配置，如下：
```
zuul:
  routes:
    hiapi:
      path: /hiapi/**
      serviceId: eureka-client
    ribbonapi:
      path: /ribbonapi/**
      serviceId: eureka-ribbon-client
    feignapi:
      path: /feignapi/**
      serviceId: eureka-feign-client
  prefix: /v1  #加个前缀
```

### 在Zuul上配置熔断器
在Zuul上实现熔断功能需要实现FallbackProvider接口。实现该接口需实现两个方法：一个是getRoute()方法，用于指定熔断功能应用于哪些路由的服务；另一个方法fallbackResponse()为进入熔断功能时执行的逻辑。实现一个针对eureka-client服务的熔断器，当eureka-client服务出现故障时，进入熔断器逻辑，向浏览器输入一句错误提示。代码如下：
```
@Component
class MyFallbackProvider  implements FallbackProvider {  //Zuul实现熔断功能需要实现此接口
    @Override
    public String getRoute() {  //指定熔断功能应用于哪些路由的服务
        return "eureka-client";
  	//    return "*"; //所有服务都加熔断功能
    }

    @Override
    public ClientHttpResponse fallbackResponse(String route, Throwable cause) { //进入熔断功能时执行的逻辑
        return new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() throws IOException {
                return HttpStatus.OK;
            }

            @Override
            public int getRawStatusCode() throws IOException {
                return 200;
            }

            @Override
            public String getStatusText() throws IOException {
                return "OK";
            }

            @Override
            public void close() {

            }

            @Override
            public InputStream getBody() throws IOException {
                return new ByteArrayInputStream("oooops!error, i'm the fallback.".getBytes());
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
            }
        };
    }
}
```
重新启动eureka-zuul-client，并且关闭eureka-client所有实例，在浏览器访问http://localhost:5000/hiapi/hi?name=cqf ，返回：
```
oooops!error, i'm the fallback.
```
如果需要所有的路由服务器都加熔断功能，只需要在getRoute()方法上返回“ * ”的匹配符，
```
    public String getRoute() {  //指定熔断功能应用于哪些路由的服务
        return "*"; //所有服务都加熔断功能
    }
```

### 在Zuul中使用过滤器
实现过滤器很简单，只需要继承ZuulFilter，并实现它的抽象方法，包括filterType()、filterOrder()、shouldFilter()以及run()。其中filterType()即过滤器类型，前文已介绍有4种。filterOrder()是过滤顺序，它是一个Int类型值，值越小越早执行该过滤器。shouldFilter()表示该过滤器是否过滤逻辑，如果为true，则执行run()方法，如果为false，则不执行run()方法。run()方法写具体过滤的逻辑。在本例中，检查请求的参数是否传了token这个参数，如果没有传，则请求不被路由到具体的服务实例，直接返回响应，状态码为401。代码如下：
```
@Component
public class MyFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(MyFilter.class);
    @Override
    public String filterType() { //过滤器类型，有四种，分别是“pre”，“post”，“routing”，“error”
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() { //过滤顺序，Int类型值，值越小，越早执行该过滤器
        return 0;
    }

    @Override
    public boolean shouldFilter() { //表示该过滤器是否过滤逻辑，如果为true，则执行run()方法，如果为false则不执行
        return true;
    }

    @Override
    public Object run() { //具体的过滤逻辑
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        log.info(String.format("%s >>> %s", request.getMethod(), request.getRequestURL().toString()));
        Object accessToken = request.getParameter("token");
      	if(accessToken == null) {
            log.warn("token is empty");
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(401);
            try {
                ctx.getResponse().getWriter().write("token is empty");
            }catch (Exception e){}

            return null;
        }
        log.info("ok");
        return null;
    }
}
```
重新启动服务，浏览器访问 http://localhost:5000/hiapi/hi?name=cqf ，显示：
```
token is empty
```
再次在浏览器访问 http://localhost:5000/hiapi/hi?name=cqf&token=1 ，显示正常。
