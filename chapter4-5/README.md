## Chapter 4-5 Spring Boot整合JPA
====================================================================
JPA全称JAVA Persistence API，它是一个数据持久化的类和方法的集合。目前，在Java项目开发中提到JPA一般是指用Hibernate的实现，因为在Java的ORM框架中，只有Hibernate实现得最好。本案例使用JPA+MySQL数据库，MySQL需提前安装好。

### 一、新建一个 Spring Boot项目
在Spring Boot工程中加入JPA的依赖spring-boot-starter-data-jpa、MySQL数据库的连接器依赖mysql-connector-java。

### 二、配置数据源
在配置文件application.yml添加数据源配置信息：
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

### 三、创建实体对象
通过@Entity注解表明该类是一个实体类，它和数据库的表名相对应；@Id注解表明该变量对应数据库Id，@GeneratedValue注解配置Id字段为自增长；@Column表明该变量对应数据库表中的字段，nullable=false、unique=true表明该字段唯一非空约束。
```
@Entity
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false,  unique = true)
	private String username;

	@Column
	private String password;
  //省略了getter setter方法
}
```
### 四、创建DAO层
数据库库访问层DAO，编写一个UserDao接口，继承JpaRepository接口，就能对数据库进行读写操作。在UserDao类写一个findByUsername方法，无需额外编码，JPA自动根据关键字理解这个方法需求。代码如下：
```
public interface UserDao extends JpaRepository<User, Long>{

	User findByUsername(String username);
}
```

### 五、创建Service层
在UserService类中注入UserDao，并写一个根据用户名获取用户的方法，通过调用注入类的findByUsername方法实现。代码如下：
```
@Service
public class UserService {
    @Autowired
    private UserDao userRepository;
    public User findUserByName(String username){
        return userRepository.findByUsername(username);
    }
}
```

### 六、创建Controller层
在UserController类写一个GET类型的API接口，其中注解@PathVariable可以获取RESTful风格的Url路径上的参数。代码如下：
```
@RequestMapping("/user")
@RestController
public class UserController {

    @Autowired
    UserService userService;
    @GetMapping("/{username}")
    public User getUser(@PathVariable("username")String username){
       return userService.findUserByName(username);
    }
}
```



