package com.cqf.dto;

import com.cqf.entity.JWT;
import com.cqf.entity.User;


import java.util.List;

/**
 * Created by cqf on 2019/9/22
 */
public class UserLoginDTO {

    private JWT jwt;

    private User user;

    public JWT getJwt() {
        return jwt;
    }

    public void setJwt(JWT jwt) {
        this.jwt = jwt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
