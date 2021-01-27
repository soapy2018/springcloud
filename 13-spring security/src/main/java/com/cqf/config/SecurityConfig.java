
package com.cqf.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;


@EnableWebSecurity
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	// @formatter:off
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
				.authorizeRequests()
					.antMatchers("/css/**", "/index").permitAll()
					.antMatchers("/user/**").hasRole("USER")
				    .antMatchers("/blogs/**").hasRole("USER")
					.and()
				.formLogin().loginPage("/login").failureUrl("/login-error")
				.and()
				.exceptionHandling().accessDeniedPage("/401");
		http.logout().logoutSuccessUrl("/");
	}


	//数据库中校验
	@Autowired
	UserDetailsService userDetailsService; // 注入UserService

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		//内存中校验
//		auth.userDetailsService(userDetailsService());

		//数据库中校验
		auth.userDetailsService(userDetailsService).passwordEncoder(NoOpPasswordEncoder.getInstance());
	}



	//内存中存放用户信息
//	@Bean
//	public UserDetailsService userDetailsService() {
//		InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager(); // 在内存中存放用户信息
//		manager.createUser(User.withUsername("cqf").password("{noop}123456").roles("USER").build());
//		manager.createUser(User.withUsername("admin").password("{noop}123456").roles("USER","ADMIN").build());
//		return manager;
//	}
}
