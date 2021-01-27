package com.cqf.client.hystrix;

import com.cqf.client.UserServiceClient;
import com.cqf.dto.RespDTO;
import com.cqf.entity.User;
import org.springframework.stereotype.Component;


/**
 * Created by cqf on 2019/10/9
 */
@Component
public class UserServiceHystrix implements UserServiceClient {

    @Override
    public RespDTO<User> getUser(String token, String username) {
        System.out.println(token);
        System.out.println(username);
        return null;
    }
}
