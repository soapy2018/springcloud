## Chapter 13 微服务监控 Spring Security详解
====================================================================

### 什么是Spring Security
Spring Security是Spring Resource社区的一个安全组件，为JavaEE企业级开发提供了全面的安全防护。Spring Security采用“安全层”的概念，使每一层都尽可能安全，连续的安全层可以达到全面的保护。Spring Security可以在Controller层、Service层、DAO层等以加注解的方式来保护应用程序的安全。Spring Security提供了细粒度的权限控制，可以精细到每一个API接口、每一个业务的方法，或者每一个操作数据库的DAO层的方法。Spring Security提供的是应用程序层的安全解决方案，一个系统的安全还需要考虑传输层和系统层的安全，例如采用HTTPS协议、服务器部署防火墙等。

### 为什么选择Spring Security
使用Spring Security有很多原因，其中一个重要原因是它对环境的无依赖性、低代码耦合性。Spring Security提供了数十个安全模块，模块与模块之间的耦合性低，模块之间可以自由组合来实现特定需求的安全功能，具有较高的可定制性。跟JavaEE的另一个优秀安全框架Apache Shiro相比，更易于应用到Spring Boot工程，也易于集成到采用Spring Cloud构建的微服务系统中。

### Spring Security 集成
目前 Spring Security 5 支持与以下技术进行集成：
+ HTTP basic access authentication
+ LDAP system
+ OpenID identity providers
+ JAAS API
+ CAS Server
+ ESB Platform
+ ……
+ Your own authentication system

在进入 Spring Security 正题之前，我们先来了解一下它的整体架构：
![Aaron Swartz](https://raw.githubusercontent.com/soapy2018/MarkdownPhotos/master/Image16.png)

### 源码解析
一、核心组件

+ SecurityContextHolder，SecurityContext 和 Authentication

最基本的对象是 SecurityContextHolder，它是我们存储当前应用程序安全上下文的详细信息，其中包括当前使用应用程序的主体的详细信息。如当前操作的用户是谁，该用户是否已经被认证，他拥有哪些角色权限等。

默认情况下，SecurityContextHolder 使用 ThreadLocal 来存储这些详细信息，这意味着 Security Context 始终可用于同一执行线程中的方法，即使 Security Context 未作为这些方法的参数显式传递。

+ 获取当前用户的信息

因为身份信息与当前执行线程已绑定，所以可以使用以下代码块在应用程序中获取当前已验证用户的用户名：
```
Object principal = SecurityContextHolder.getContext()
  .getAuthentication().getPrincipal();

if (principal instanceof UserDetails) {
  String username = ((UserDetails)principal).getUsername();
} else {
  String username = principal.toString();
}
```
调用 getContext() 返回的对象是 SecurityContext 接口的一个实例，对应 SecurityContext 接口定义如下：
```
// org/springframework/security/core/context/SecurityContext.java
public interface SecurityContext extends Serializable {
	Authentication getAuthentication();
	void setAuthentication(Authentication authentication);
}
```
+ Authentication

在 SecurityContext 接口中定义了 getAuthentication 和 setAuthentication 两个抽象方法，当调用 getAuthentication 方法后会返回一个 Authentication 类型的对象，这里的 Authentication 也是一个接口，它的定义如下：
```
// org/springframework/security/core/Authentication.java
public interface Authentication extends Principal, Serializable {
  // 权限信息列表，默认是GrantedAuthority接口的一些实现类，通常是代表权限信息的一系列字符串。
	Collection<? extends GrantedAuthority> getAuthorities();
  // 密码信息，用户输入的密码字符串，在认证过后通常会被移除，用于保障安全。
	Object getCredentials();
	Object getDetails();
  // 最重要的身份信息，大部分情况下返回的是UserDetails接口的实现类，也是框架中的常用接口之一。
	Object getPrincipal();
	boolean isAuthenticated();
	void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException;
}
```
以上的 Authentication 接口是 spring-security-core jar 包中的接口，直接继承自 Principal 类，而 Principal 是位于 java.security 包中，由此可知 Authentication 是 spring security 中核心的接口。通过这个 Authentication 接口的实现类，我们可以得到用户拥有的权限信息列表，密码，用户细节信息，用户身份信息，认证信息等。

+ 小结

下面我们来简单总结一下 SecurityContextHolder，SecurityContext 和 Authentication 这个三个对象之间的关系，SecurityContextHolder 用来保存 SecurityContext （安全上下文对象），通过调用 SecurityContext 对象中的方法，如 getAuthentication 方法，我们可以方便地获取 Authentication 对象，利用该对象我们可以进一步获取已认证用户的详细信息。

二、身份验证

Spring Security 中的身份验证是什么？
让我们考虑一个每个人都熟悉的标准身份验证方案：
+ 系统会提示用户使用用户名和密码登录。
+ 系统验证用户名和密码是否正确。
+ 若验证通过则获取该用户的上下文信息（如权限列表）。
+ 为用户建立安全上下文。
+ 用户继续进行，可能执行某些操作，该操作可能受访问控制机制的保护，该访问控制机制根据当前安全上下文信息检查操作所需的权限。

前三项构成了身份验证进程，因此我们将在 Spring Security 中查看这些内容：
+ 获取用户名和密码并将其组合到 UsernamePasswordAuthenticationToken 的实例中（我们之前看到的Authentication 接口的实例）。
+ 令牌将传递给 AuthenticationManager 的实例以进行验证。
+ AuthenticationManager 在成功验证时返回完全填充的 Authentication 实例。
+ SecurityContext 对象是通过调用 SecurityContextHolder.getContext().setAuthentication(…) 创建的，传入返回的身份验证 Authentication 对象。

三、核心服务

+ AuthenticationManager，ProviderManager 和 AuthenticationProvider

AuthenticationManager（接口）是认证相关的核心接口，也是发起认证的出发点，因为在实际需求中，我们可能会允许用户使用用户名 + 密码登录，同时允许用户使用邮箱 + 密码，手机号码 + 密码登录，甚至，可能允许用户使用指纹登录，所以要求认证系统要支持多种认证方式。

Spring Security 中 AuthenticationManager 接口的默认实现是 ProviderManager，但它本身并不直接处理身份验证请求，它会委托给已配置的 AuthenticationProvider 列表，每个列表依次被查询以查看它是否可以执行身份验证。每个 Provider 验证程序将抛出异常或返回一个完全填充的 Authentication 对象。

也就是说，Spring Security 中核心的认证入口始终只有一个：AuthenticationManager，不同的认证方式：用户名 + 密码（UsernamePasswordAuthenticationToken），邮箱 + 密码，手机号码 + 密码登录则对应了三个 AuthenticationProvider。

在 ProviderManager 进行认证的过程中，会遍历 providers 列表，判断是否支持当前 authentication 对象的认证方式，若支持该认证方式时，就会调用所匹配 provider（AuthenticationProvider）对象的 authenticate 方法进行认证操作。若认证失败则返回 null，下一个 AuthenticationProvider 会继续尝试认证，如果所有认证器都无法认证成功，则 ProviderManager 会抛出一个 ProviderNotFoundException 异常。

+ UserDetails 与 UserDetailsService

虽然 Authentication 与 UserDetails 很类似，但它们之间是有区别的。Authentication 的 getCredentials() 与 UserDetails 中的 getPassword() 需要被区分对待，前者是用户提交的密码凭证，后者是用户正确的密码，认证器其实就是对这两者进行比对。

此外 Authentication 中的 getAuthorities() 实际是由 UserDetails 的 getAuthorities() 传递而形成的。还记得 Authentication 接口中的 getUserDetails() 方法吗？其中的 UserDetails 用户详细信息就是经过了 provider （AuthenticationProvider） 认证之后被填充的。

在 UserDetailsService 接口中，只有一个 loadUserByUsername 方法，用于通过 username 来加载匹配的用户。当找不到 username 对应用户时，会抛出 UsernameNotFoundException 异常。UserDetailsService 和 AuthenticationProvider 两者的职责常常被人们搞混，记住一点即可，UserDetailsService 只负责从特定的地方（通常是数据库）加载用户信息，仅此而已。
