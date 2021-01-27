package com.cqf.client.hystrix;


import com.cqf.client.AuthServiceClient;
import com.cqf.entity.JWT;
import org.springframework.stereotype.Component;

/**
 * Created by cqf on 2019/10/9
 */
@Component
public class AuthServiceHystrix implements AuthServiceClient {
    @Override
    public JWT getToken(String authorization, String type, String username, String password) {
        System.out.println("--------opps getToken hystrix---------");
        return null;
    }
}
