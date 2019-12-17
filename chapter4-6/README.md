## Chapter 4-6 Spring Boot整合Redis
====================================================================
Redis是一个开源的、先进的key-value存储系统，可用于构建高性能的存储系统，支持的数据结构有字符串、哈希、列表、集合、排序集合、位图、超文本等。Redis是一宗NoSQL，读写非常快，支持丰富的数据结构，所有的操作都是原子的。

### 一、Redis安装
官网上不提供windows版本的，但是微软在github上维护了一个版本。

### 二、Spring Boot中使用Redis
1、添加Redis的起步依赖spring-boot-starter-data-redis

2、在配置文件application.properties添加Redis的数据源配置，例如：
```
# REDIS (RedisProperties)
# Redis数据库索引（默认为0）
spring.redis.database=0
# Redis服务器地址
spring.redis.host=localhost
# Redis服务器连接端口
spring.redis.port=6379
# Redis服务器连接密码（默认为空）
spring.redis.password=
# 连接池最大连接数（使用负值表示没有限制）
spring.redis.jedis.pool.max-active=8
# 连接池最大阻塞等待时间（使用负值表示没有限制）
spring.redis.jedis.pool.max-wait=-1
# 连接池中的最大空闲连接
spring.redis.jedis.pool.max-idle=8
# 连接池中的最小空闲连接
spring.redis.jedis.pool.min-idle=0
# 连接超时时间（毫秒）
spring.redis.timeout=5000
```
3、创建Dao层

数据操作层的RedisDao类通过@Repository注解来注入Spring Ioc容器中，该类通过自动注入StringRedisTemplate的bean来对Redis数据库中的字符串类型的数据进行操作。代码如下：
```
@Repository
public class RedisDao {

    @Autowired
    private StringRedisTemplate template;

    public  void setKey(String key,String value){
        ValueOperations<String, String> ops = template.opsForValue();
        ops.set(key,value,1, TimeUnit.MINUTES);//1分钟过期
    }

    public String getValue(String key){
        ValueOperations<String, String> ops = template.opsForValue();
        return ops.get(key);
    }
}
```
### 运行
启动单元测试验证数据的写入和读取。   


