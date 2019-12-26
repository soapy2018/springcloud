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

### 案例实战
本案例包含注册中心eureka-server、服务提供者user-server、网关服务gateway-server。

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


