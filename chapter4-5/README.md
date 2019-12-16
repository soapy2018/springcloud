## Chapter 4-5 Spring Boot整合JPA
====================================================================
JPA全称JAVA Persistence API，它是一个数据持久化的类和方法的集合。目前，在Java项目开发中提到JPA一般是指用Hibernate的实现，因为在Java的ORM框架中，只有Hibernate实现得最好。本案例使用JPA+MySQL数据库，MySQL需提前安装好。

### 一、新建一个 Spring Boot项目
在Spring Boot工程中加入JPA的依赖spring-boot-starter-data-jpa、MySQL数据库的连接器依赖mysql-connector-java。

### 二、配置数据源
```
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver #MySQL驱动类
    url: jdbc:mysql://localhost:3306/SpringCloud?useUnicode=true&characterEncoding=utf8&characterSetResults=utf8&serverTimezone=GMT
    username: root
    password: 123456

  jpa:
    hibernate:
      ddl-auto: update # 第一次建表create  后面用update
    show-sql: true
```
hibernate.hbm2ddl.auto 参数的作用主要用于：自动创建、更新、验证数据库表结构，有五个值，默认是 none 。

create：每次加载 Hibernate 时都会删除上一次生成的表，然后根据 model 类再重新来生成新表，哪怕两次没有任何改变也要这样执行，这就是导致数据库表数据丢失的一个重要原因。

create-drop：每次加载 Hibernate 时根据 model 类生成表，但是 sessionFactory 一关闭，表就自动删除。

update：最常用的属性，第一次加载 Hibernate 时根据 model 类会自动建立起表的结构（前提是先建立好数据库），以后加载 Hibernate 时根据 model 类自动更新表结构，即使表结构改变了，但表中的行仍然存在，不会删除以前的行。要注意的是当部署到服务器后，表结构是不会被马上建立起来的，是要等应用第一次运行起来后才会。

validate ：每次加载 Hibernate 时，验证创建数据库表结构，只会和数据库中的表进行比较，不会创建新表，但是会插入新值。


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

上面介绍了如何把配置属性写到application.yml配置文件中，并把配置属性读取到一个配置类中。有时属性太多，把所有的配置属性都写到application.yml配置文件中不太合适，这时需要自定义配置文件。例如例子中自定义了一个test.properties配置文件，其配置信息如下：
```
com.cqf.name=cqf
com.cqf.age=12
```
如何将这个配置文件的属性赋值给一个JavaBean呢？需要在类名增加注解@PropertySource指明配置文件的路径。例如：
```
@Configuration
@PropertySource(value = "classpath:test.properties")
@ConfigurationProperties(prefix = "com.cqf")
public class User {
    private String name;
    private int age;
    //省略getter setter
}
```
4、多个环境的配置文件
实际开发过程中，可能有多个不同环境的配置文件，例如：开发环境、测试环境、生产环境等。Spring Boot支持程序启动时在配置文件application.yml中指定环境的配置文件，配置文件的格式为application-{profile}.propertites，其中{profile}对应环境标志，例如在application.yum加上配置：
```
spring:
  profiles:
    active: dev
```
则使用application-dev.propertites作为配置文件。另外，我们也可以通过java -jar这种方式启动程序并指定配置文件，例如：
```
java -jar springbootdemo.jar -- spring.profiles.active=dev
```

### 二、运行状态监控Actuator
Actuator的起步依赖为spring-boot-starter-actuator，需要注意的是 Spring Boot 2.0 相对于上个版本， Actuator 发生很多变化，所有 endpoints 默认情况下都已移至/actuator，就是多了根路径 actuator。

您可以按如下方式公开所有端点：management.endpoints.web.exposure.include="*"

您可以通过以下方式显式启用/shutdown端点：management.endpoint.shutdown.enabled=true

要公开所有（已启用）网络端点除env端点之外：
```
management.endpoints.web.exposure.include="*"
management.endpoints.web.exposure.exclude=env
```
例子：
```
server:
  port: 8082
management:
    server:
      port: 9001
    endpoints:
      web:
        exposure:
         include: "*"
    endpoint:
      shutdown:
        enabled: true
```
management.server.port指定Actuator对外暴露Restful API接口的端口，如果不指定默认为应用程序的启动端口。

1、查看运行程序的健康状态

GET请求：Actuator/health

2、查看运行程序的beans

GET请求： Actuator/beans

3、使用Actuator关闭运行程序

POST请求：Actuator/shutdown

4、使用shell连接Actuator

除了REST API这种方式监控运行程序，还可通过shell连接Actuator，需添加起步依赖spring-boot-starter-remote-shell，端口是2000。但是该功能在Spring Boot 2.X版本后已经废弃。
