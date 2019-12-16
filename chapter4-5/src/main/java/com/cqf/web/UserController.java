package com.cqf.web;

import com.cqf.entity.User;
import com.cqf.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by cqf on 2019/8/6
 */
@RequestMapping("/user")
@RestController
public class UserController {

    @Autowired
    UserService userService;
    @GetMapping("/{username}")
    public User getUser(@PathVariable("username")String username){
       return userService.findUserByName(username);

    }



}
