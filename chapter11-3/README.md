## Chapter 11 服务链路追踪 Spring Cloud Sleuth
====================================================================

随着业务发展，系统拆分导致系统调用链路愈发复杂一个前端请求可能最终需要调用很多次后端服务才能完成，当整个请求变慢或不可用时，我们是无法得知该请求是由某个或某些后端服务引起的，这时就需要解决如何快读定位服务故障点，以对症下药。于是就有了分布式系统调用跟踪的诞生。

现今业界分布式服务跟踪的理论基础主要来自于 Google 的一篇论文《Dapper, a Large-Scale Distributed Systems Tracing Infrastructure》，使用最为广泛的开源实现是 Twitter 的 Zipkin，为了实现平台无关、厂商无关的分布式服务跟踪，CNCF 发布了布式服务跟踪标准 Open Tracing。国内，淘宝的“Eagleeye”（鹰眼）、京东的“Hydra”、大众点评的“CAT”、新浪的“Watchman”、唯品会的“Microscope”、窝窝网的“Tracing”都是这样的系统。

### Spring Cloud Sleuth
一般的，一个分布式服务跟踪系统，主要有三部分：数据收集、数据存储和数据展示。根据系统大小不同，每一部分的结构又有一定变化。譬如，对于大规模分布式系统，数据存储可分为实时数据和全量数据两部分，实时数据用于故障排查（troubleshooting），全量数据用于系统优化；数据收集除了支持平台无关和开发语言无关系统的数据收集，还包括异步数据收集（需要跟踪队列中的消息，保证调用的连贯性），以及确保更小的侵入性；数据展示又涉及到数据挖掘和分析。虽然每一部分都可能变得很复杂，但基本原理都类似。

服务追踪的追踪单元是从客户发起请求（request）抵达被追踪系统的边界开始，到被追踪系统向客户返回响应（response）为止的过程，称为一个“trace”。每个 trace 中会调用若干个服务，为了记录调用了哪些服务，以及每次调用的消耗时间等信息，在每次调用服务时，埋入一个调用记录，称为一个“span”。这样，若干个有序的 span 就组成了一个 trace。在系统向外界提供服务的过程中，会不断地有请求和响应发生，也就会不断生成 trace，把这些带有span 的 trace 记录下来，就可以描绘出一幅系统的服务拓扑图。附带上 span 中的响应时间，以及请求成功与否等信息，就可以在发生问题的时候，找到异常的服务；根据历史数据，还可以从系统整体层面分析出哪里性能差，定位性能优化的目标。

Spring Cloud Sleuth为服务之间调用提供链路追踪。通过Sleuth可以很清楚的了解到一个服务请求经过了哪些服务，每个服务处理花费了多长。从而让我们可以很方便的理清各微服务间的调用关系。此外Sleuth可以帮助我们：
+ 耗时分析: 通过Sleuth可以很方便的了解到每个采样请求的耗时，从而分析出哪些服务调用比较耗时;
+ 可视化错误: 对于程序未捕捉的异常，可以通过集成Zipkin服务界面上看到;
+ 链路优化: 对于调用比较频繁的服务，可以针对这些服务实施一些优化措施。
+ spring cloud sleuth可以结合zipkin，将信息发送到zipkin，利用zipkin的存储来存储信息，利用zipkin ui来展示数据。

这是Spring Cloud Sleuth的概念图：
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image5.png)

Spring Cloud Sleuth采用了Google的开源项目Dapper的专业术语。

+ Span：基本工作单元，发送一个远程调度任务就会产生一个Span，Span是用一个64位ID唯一标识的，Trace是用另一个64位ID唯一标识的。Span还包含了其他的信息，例如摘要、时间戳事件、Span的ID以及进程ID。
+ Trace：由一系列Span组成的，呈树状结构。请求一个微服务系统的API接口，这个API接口需要调用多个微服务单元，调用每个微服务单元都会产生一个新的Span，所有由这个请求产生的Span组成了这个Trace。
+ Annotation：用于记录一个事件，一些核心注解用于定义一个请求的开始和结束，这些注解如下：
  - cs-Client Sent：客户端发送一个请求，这个注解描述了Span的开始。
  - sr-Server Received：服务端获得请求并准备开始处理它，如果将其sr减去cs时间戳，便可得到网络传输的时间。
  - ss-Server Sent：服务端发送响应，该注解表明请求处理的完成（当请求返回客户端），用ss的时间戳减去sr时间戳，便可以得到服务器请求的时间。
  - cr-Client Received：客户端接收响应，此时Span结束，如果cr的时间戳减去cs时间戳，便可以得到整个请求所消耗的时间。

Spring Cloud Sleuth 也为我们提供了一套完整的链路解决方案，Spring Cloud Sleuth 可以结合 Zipkin，将信息发送到 Zipkin，利用 Zipkin 的存储来存储链路信息，利用 Zipkin UI 来展示数据。

### Zipkin
Zipkin是一种分布式链路追踪系统。 它有助于收集解决微服务架构中的延迟问题所需的时序数据。 它管理这些数据的收集和查找。 Zipkin的设计基于Google Dapper论文。

跟踪器存在于应用程序中，记录请求调用的时间和元数据。跟踪器使用库，它们的使用对用户是无感知的。例如，Web服务器会在收到请求时和发送响应时会记录相应的时间和一些元数据。一次完整链路请求所收集的数据被称为Span。

我们可以使用它来收集各个服务器上请求链路的跟踪数据，并通过它提供的 REST API 接口来辅助我们查询跟踪数据以实现对分布式系统的监控程序，从而及时地发现系统中出现的延迟升高问题并找出系统性能瓶颈的根源。除了面向开发的 API 接口之外，它也提供了方便的 UI 组件来帮助我们直观的搜索跟踪信息和分析请求链路明细，比如：可以查询某段时间内各用户请求的处理时间等。 Zipkin 提供了可插拔数据存储方式：In-Memory、MySql、Cassandra 以及 Elasticsearch。接下来的测试为方便直接采用 In-Memory 方式进行存储，生产推荐 Elasticsearch。
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image6.png)

上图展示了 Zipkin 的基础架构，它主要由 4 个核心组件构成：
+ Collector：收集器组件，它主要用于处理从外部系统发送过来的跟踪信息，将这些信息转换为 Zipkin 内部处理的 Span 格式，以支持后续的存储、分析、展示等功能。
+ Storage：存储组件，它主要对处理收集器接收到的跟踪信息，默认会将这些信息存储在内存中，我们也可以修改此存储策略，通过使用其他存储组件将跟踪信息存储到数据库中。
+ RESTful API：API 组件，它主要用来提供外部访问接口。比如给客户端展示跟踪信息，或是外接系统访问以实现监控等。
+ Web UI：UI 组件，基于 API 组件实现的上层应用。通过 UI 组件用户可以方便而有直观地查询和分析跟踪信息。

#### zipkin-server
在Spring Cloud D版本，zipkin-server通过引入依赖的方式构建工程，自从E版本之后，这一方式改变了，采用官方的jar形式启动，所以需要通过下载官方的jar来启动，也通过以下命令一键启动：
```
curl -sSL https://zipkin.io/quickstart.sh | bash -s
java -jar zipkin.jar
```
上面的第一行命令会从zipkin官网下载官方的jar包。 如果是window系统，建议使用gitbash执行上面的命令。

如果用 Docker 的话，使用以下命令：
```
docker run -d -p 9411:9411 openzipkin/zipkin
```
通过java -jar zipkin.jar的方式启动之后，在浏览器上访问lcoalhost:9411即可。

#### 使用Http传输链路数据
本案例包含注册中心eureka-server、服务提供者user-server、网关服务gateway-server。

1、服务提供者user-service添加Eureka、Web、sleuth（可以不要，Zipkin里已包含sleuth）、Zipkin依赖如下：
```
	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-sleuth</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-zipkin</artifactId>
		</dependency>
	</dependencies>
```
在配置文件中配置如下：
```
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8111/eureka/
server:
  port: 8112
spring:
  application:
    name: user-service
  sleuth:
    sampler:
      probability: 1.0
  zipkin:
    base-url: http://localhost:9411
```
其中spring.sleuth.sampler.probability可以设置为小数，最大值为1.0，当设置为1.0时就是链路数据100%收集到zipkin-server，当设置为0.1时，即10%概率收集链路数据；spring.zipkin.base-url设置zipkin-server的地址。

服务提供者提供一个“ /user/hi ”的API接口，对外提供服务，并在程序启动类开启Eureka Client功能。

2、网关服务gateway-service添加Eureka、Zuul、Web、sleuth（可以不要，Zipkin里已包含sleuth）、Zipkin依赖如下：
```
<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-zuul</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-sleuth</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-zipkin</artifactId>
		</dependency>
```
在配置文件中配置如下：
```
server:
  port: 5000
  
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8111/eureka/

spring:
  application:
    name: gateway-service
  sleuth:
    sampler:
      probability: 1.0
  zipkin:
    base-url: http://localhost:9411

zuul:
  routes:
    api-a:
      path: /user-api/**
      serviceId: user-service
```
并在程序启动类开启Eureka Client和Zuul代理功能。

3、依次启动zipkin-server（通过命令java -jar zipkin.jar）、user-service、gateway-service，浏览器访问http://localhost:5000/user-api/user/hi ，返回：
```
I'm cqf
```
访问http://localhost:9411/ ，即访问Zipkin的展示界面，如图：
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image7.png)

点击“Find Tracks”按钮，显示请求的调用情况，如请求的调用时间、消耗时间以及链路情况。如图：
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image8.png)

点击“Dependences”按钮，可以查看服务的依赖关系，本例中gateway-service将请求转发到了user-service。如图：
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image9.png)

### 使用rabbitmq进行链路数据收集
在上面的案例中使用的http请求的方式将链路数据发送给zipkin-server，其实还可以使用rabbitmq的方式进行服务的消费。使用rabbitmq需要安装rabbitmq程序，下载地址http://www.rabbitmq.com/ 。zipkin连接RabbitMQ的属性所对应的环境变量如下：

属性     | 环境变量 |  描述  |
-------- | ----- | ------ |
zipkin.collector.rabbitmq.addresses  | RABBIT_ADDRESSES  | 用逗号分隔的 RabbitMQ 地址列表，例如localhost:5672,localhost:5673  |
zipkin.collector.rabbitmq.password  | RABBIT_PASSWORD  | 连接到 RabbitMQ 时使用的密码，默认为 guest  |
zipkin.collector.rabbitmq.username  | RABBIT_USER  | 连接到 RabbitMQ 时使用的用户名，默认为guest  |
zipkin.collector.rabbitmq.virtual-host  | RABBIT_VIRTUAL_HOST  | 使用的 RabbitMQ virtual host，默认为 /  |
zipkin.collector.rabbitmq.use-ssl  | RABBIT_USE_SSL  | 设置为true则用 SSL 的方式与 RabbitMQ 建立链接  |
zipkin.collector.rabbitmq.concurrency  | RABBIT_CONCURRENCY  | 并发消费者数量，默认为1  |
zipkin.collector.rabbitmq.connection-timeout  | RABBIT_CONNECTION_TIMEOUT  | 建立连接时的超时时间，默认为 60000毫秒，即 1 分钟  |
zipkin.collector.rabbitmq.queue  | RABBIT_QUEUE  | 从中获取 span 信息的队列，默认为 zipkin  |

1、Zipkin集成RabbitMQ：根据官方给的方式，我们可以使用 java -jar zipkin.jar 的方式启动 Zipkin Server，在使用这个命令的时候，我们是可以设置一些参数的，这里我们可以通过设置环境变量让 Zipkin 从 RabbitMQ 中获取到跟踪信息，比如，通过以下命令启动：
```
RABBIT_ADDRESSES=localhost java -jar zipkin.jar
```
上面的命令等同于一下的命令：
```
java -jar zipkin.jar --zipkin.collector.rabbitmq.addresses=localhost
```
此时访问RabbitMQ的管理界面http://localhost:15672/ ，在Queues可以看到已经创建了一个zipkin的队列，说明ZipServer 集成RabbitMQ成功了。界面如下：
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image10.png)
当然，访问http://localhost:9411/ ，也能看到Zipkin的管理界面。

2、在工程user-service和gateway-service添加rabbitmq的起步依赖```spring-cloud-stream-binder-rabbit```，并将配置文件的如下配置注释：
```
#    zipkin:
#      base-url: http://localhost:9411
```
3、这样就配置好了，重启user-service和gateway-service，访问http://localhost:9411/ 就能看到链路相关信息了。

### 将链路数据存储在Mysql数据库中
上面的例子是将链路数据存在内存中，只要zipkin-server重启之后，之前的链路数据全部查找不到了，zipkin是支持将链路数据存储在mysql、cassandra、elasticsearch中的。 现在讲解如何将链路数据存储在Mysql数据库中。 首先需要初始化zikin存储在Mysql的数据的scheme，可以在这里查看https://github.com/openzipkin/zipkin/blob/master/zipkin-storage/mysql-v1/src/main/resources/mysql.sql， 具体如下：
```
CREATE TABLE IF NOT EXISTS zipkin_spans (
  `trace_id_high` BIGINT NOT NULL DEFAULT 0 COMMENT 'If non zero, this means the trace uses 128 bit traceIds instead of 64 bit',
  `trace_id` BIGINT NOT NULL,
  `id` BIGINT NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `remote_service_name` VARCHAR(255),
  `parent_id` BIGINT,
  `debug` BIT(1),
  `start_ts` BIGINT COMMENT 'Span.timestamp(): epoch micros used for endTs query and to implement TTL',
  `duration` BIGINT COMMENT 'Span.duration(): micros used for minDuration and maxDuration query',
  PRIMARY KEY (`trace_id_high`, `trace_id`, `id`)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED CHARACTER SET=utf8 COLLATE utf8_general_ci;

ALTER TABLE zipkin_spans ADD INDEX(`trace_id_high`, `trace_id`) COMMENT 'for getTracesByIds';
ALTER TABLE zipkin_spans ADD INDEX(`name`) COMMENT 'for getTraces and getSpanNames';
ALTER TABLE zipkin_spans ADD INDEX(`remote_service_name`) COMMENT 'for getTraces and getRemoteServiceNames';
ALTER TABLE zipkin_spans ADD INDEX(`start_ts`) COMMENT 'for getTraces ordering and range';

CREATE TABLE IF NOT EXISTS zipkin_annotations (
  `trace_id_high` BIGINT NOT NULL DEFAULT 0 COMMENT 'If non zero, this means the trace uses 128 bit traceIds instead of 64 bit',
  `trace_id` BIGINT NOT NULL COMMENT 'coincides with zipkin_spans.trace_id',
  `span_id` BIGINT NOT NULL COMMENT 'coincides with zipkin_spans.id',
  `a_key` VARCHAR(255) NOT NULL COMMENT 'BinaryAnnotation.key or Annotation.value if type == -1',
  `a_value` BLOB COMMENT 'BinaryAnnotation.value(), which must be smaller than 64KB',
  `a_type` INT NOT NULL COMMENT 'BinaryAnnotation.type() or -1 if Annotation',
  `a_timestamp` BIGINT COMMENT 'Used to implement TTL; Annotation.timestamp or zipkin_spans.timestamp',
  `endpoint_ipv4` INT COMMENT 'Null when Binary/Annotation.endpoint is null',
  `endpoint_ipv6` BINARY(16) COMMENT 'Null when Binary/Annotation.endpoint is null, or no IPv6 address',
  `endpoint_port` SMALLINT COMMENT 'Null when Binary/Annotation.endpoint is null',
  `endpoint_service_name` VARCHAR(255) COMMENT 'Null when Binary/Annotation.endpoint is null'
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED CHARACTER SET=utf8 COLLATE utf8_general_ci;

ALTER TABLE zipkin_annotations ADD UNIQUE KEY(`trace_id_high`, `trace_id`, `span_id`, `a_key`, `a_timestamp`) COMMENT 'Ignore insert on duplicate';
ALTER TABLE zipkin_annotations ADD INDEX(`trace_id_high`, `trace_id`, `span_id`) COMMENT 'for joining with zipkin_spans';
ALTER TABLE zipkin_annotations ADD INDEX(`trace_id_high`, `trace_id`) COMMENT 'for getTraces/ByIds';
ALTER TABLE zipkin_annotations ADD INDEX(`endpoint_service_name`) COMMENT 'for getTraces and getServiceNames';
ALTER TABLE zipkin_annotations ADD INDEX(`a_type`) COMMENT 'for getTraces and autocomplete values';
ALTER TABLE zipkin_annotations ADD INDEX(`a_key`) COMMENT 'for getTraces and autocomplete values';
ALTER TABLE zipkin_annotations ADD INDEX(`trace_id`, `span_id`, `a_key`) COMMENT 'for dependencies job';

CREATE TABLE IF NOT EXISTS zipkin_dependencies (
  `day` DATE NOT NULL,
  `parent` VARCHAR(255) NOT NULL,
  `child` VARCHAR(255) NOT NULL,
  `call_count` BIGINT,
  `error_count` BIGINT,
  PRIMARY KEY (`day`, `parent`, `child`)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED CHARACTER SET=utf8 COLLATE utf8_general_ci;

```
在数据库中初始化上面的脚本之后，需要做的就是zipkin-server如何连接数据库。zipkin如何连数据库同连接rabbitmq一样。zipkin连接MySQL的属性所对应的环境变量如下：

属性     | 环境变量 |  描述  |
-------- | ----- | ------ |
zipkin.storage.type  | STORAGE_TYPE  | 默认的为mem，即为内存，其他可支持的为cassandra、cassandra3、elasticsearch、mysql  |
zipkin.storage.mysql.host  | MYSQL_HOST  | 数据库的host，默认localhost  |
zipkin.storage.mysql.port  | MYSQL_TCP_PORT  | 数据库的端口，默认3306  |
zipkin.storage.mysql.username  | MYSQL_USER  | 连接数据库的用户名，默认为空  |
zipkin.storage.mysql.password  | MYSQL_PASS  | 连接数据库的密码，默认为空  |
zipkin.storage.mysql.db  | MYSQL_DB  | zipkin使用的数据库名，默认是zipkin  |
zipkin.storage.mysql.max-active  | MYSQL_MAX_CONNECTIONS  | 最大连接数，默认是10  |

zipkin-server使用MySQL存储，用如下命令启动zipkin：
```
STORAGE_TYPE=mysql MYSQL_HOST=localhost MYSQL_TCP_PORT=3306 MYSQL_USER=root MYSQL_PASS=123456 MYSQL_DB=zipkin java -jar zipkin.jar
```
等同于以下的命令
```
java -jar zipkin.jar --zipkin.storage.type=mysql --zipkin.storage.mysql.host=localhost --zipkin.storage.mysql.port=3306 --zipkin.storage.mysql.username=root --zipkin.storage.mysql.password=123456
```
若使用RabbitMQ传输链路数据，则以上命令再加上RABBIT_ADDRESSES=localhost，完整命令如下：
```
java -jar zipkin.jar --zipkin.collector.rabbitmq.addresses=localhost --zipkin.storage.type=mysql --zipkin.storage.mysql.host=localhost --zipkin.storage.mysql.port=3306 --zipkin.storage.mysql.username=root --zipkin.storage.mysql.password=123456
```
这样链路信息就会存储在MySQL中了。

### 将链路数据存在在Elasticsearch中
在高并发的情况下，使用MySQL存储链路数据显然是不合理的，这时可以选择Elasticsearch。zipkin-server支持将链路数据存储在ElasticSearch中。读者需要自行安装ElasticSearch和Kibana，下载地址为 https://www.elastic.co/downloads/elasticsearch 和 https://www.elastic.co/downloads/kibana。 安装完成后启动，其中ElasticSearch的默认端口号为9200，Kibana的默认端口号为5601。
zipkin连接Elasticsearch的属性所对应的环境变量如下：

属性     | 环境变量 |  描述  |
-------- | ----- | ------ |
zipkin.storage.elasticsearch.hosts  | ES_HOSTS  | ES_HOSTS，默认为空  |
zipkin.storage.elasticsearch.pipeline  | ES_PIPELINE  | ES_PIPELINE，默认为空  |
zipkin.storage.elasticsearch.max-requests  | ES_MAX_REQUESTS  | ES_MAX_REQUESTS，默认为64  |
zipkin.storage.elasticsearch.timeout  | ES_TIMEOUT  | ES_TIMEOUT，默认为10s  |
zipkin.storage.elasticsearch.index  | ES_INDEX  | ES_INDEX，默认是zipkin  |
zipkin.storage.elasticsearch.date-separator  | ES_DATE_SEPARATOR  | ES_DATE_SEPARATOR，默认为“-”  |
zipkin.storage.elasticsearch.index-shards  | ES_INDEX_SHARDS  | ES_INDEX_SHARDS，默认是5  |
zipkin.storage.elasticsearch.index-replicas  | ES_INDEX_REPLICAS  | ES_INDEX_REPLICAS，默认是1  |
zipkin.storage.elasticsearch.username | ES_USERNAME  | ES的用户名，默认为空  |
zipkin.storage.elasticsearch.password  | ES_PASSWORD  | ES的密码，默认是为空 |

zipkin-server使用Elasticsearch存储，用如下命令启动zipkin：
```
java -jar zipkin.jar --STORAGE_TYPE=elasticsearch --ES_HOSTS=http://localhost:9200 --ES_INDEX=zipkin
```
等同于以下的命令
```
java -jar zipkin.jar --zipkin.storage.type=elasticsearch --zipkin.storage.elasticsearch.hosts=http://localhost:9200 --zipkin.storage.elasticsearch.index=zipkin
```
若使用RabbitMQ传输链路数据，则以上命令再加上RABBIT_ADDRESSES=localhost，完整命令如下：
```
java -jar zipkin.jar --zipkin.collector.rabbitmq.addresses=localhost --zipkin.storage.type=elasticsearch --zipkin.storage.elasticsearch.hosts=http://localhost:9200 --zipkin.storage.elasticsearch.index=zipkin
```
启动Zipkin、Elasticsearch，这样链路信息就会存储在Elasticsearch中了。

### 在zipkin上展示链路数据
链路数据存储在ElasticSearch中，ElasticSearch可以和Kibana结合，将链路数据展示在Kibana上。安装完成Kibana后启动，Kibana默认会向本地端口为9200的ElasticSearch读取数据（若不是本地，打开config/kibana.yml文件，通过elasticsearch.hosts设置ElasticSearch地址）。Kibana默认的端口为5601，访问Kibana的主页http://localhost:5601， 其界面如下图所示：
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image11.png)
在上图的界面中，单击“Management”按钮，然后添加一个index。我们将ElasticSearch中写入链路数据的index配置为“zipkin”，那么在界面填写为“zipkin-*”。创建完成index后，单击“Discover”，就可以在界面上展示链路数据了，展示界面如下图所示：
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image12.png)




