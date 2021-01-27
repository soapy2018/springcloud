package com.cqf.client.hystrix;


import com.cqf.client.AuthServiceClient;
import com.cqf.entity.JWT;
import org.springframework.stereotype.Component;

/**
 * Created by cqf on 2019/9/22
 */
@Component
public class AuthServiceHystrix implements AuthServiceClient {
    @Override
    public JWT getToken(String authorization, String type, String username, String password) {
        return null;
    }
}
