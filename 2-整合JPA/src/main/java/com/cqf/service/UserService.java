package com.cqf.service;
import com.cqf.dao.UserDao;
import com.cqf.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by CQF on 2018/8/6
 */
@Service
public class UserService {
    @Autowired
    private UserDao userRepository;

    public User findUserByName(String username){

        return userRepository.findByUsername(username);
    }
}

