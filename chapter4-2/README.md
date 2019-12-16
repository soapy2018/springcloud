Chapter 4 Examples
==================
一、注册中心

本例使用Zookeeper作为注册中心，需事先安装好Zookeeper。

二、接口工程

dubbo-api工程里有一个接口IHello

三、服务端

dubbo-server工程添加Dubbo和接口工程的依赖，实现IHello接口，并用注解@Service暴露服务，在application.properties文件中配置Dubbo。

四、消费方

dubbo-client工程添加Dubbo和接口工程的依赖，编写远程调用Dubbo服务，@Reference注解可以用于生成远程服务代理，在application.properties文件中配置跟服务端一样的服务注册中心。

五、网关

模块之间互相调用时，为了降低由网络波动带来的不确定性因素并提升系统安全性，生产环境中所有模块一般都运行在内网环境中，并单独提供一个工程作为网关服务，开放固定端口代理所有模块提供的服务，并通过拦截器验证所有外部请求以达到权限管理的目的。外部应用可能是App、网站或桌面客户端，为了达到通用性，网关服务一般为web服务，通过HTTP协议提供RESTful风格的API接口。

dubbo-gateway工程添加Dubbo和接口工程的依赖，并添加spring-boot-starter-web依赖（支持Web应用开发，包含Tomcat和Spring-MVC），并在application.properties文件中配置Dubbo，编写用于调用Dubbo服务的控制器，编写验证逻辑RequestInterceptor，配置拦截器WebAppConfiguration。
