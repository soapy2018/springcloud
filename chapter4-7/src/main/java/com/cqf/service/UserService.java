package com.cqf.service;

import com.cqf.dao.UserDao;
import com.cqf.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by CQF on 2019/8/6
 */
@Service
public class UserService {

    @Autowired
    private UserDao userRepository;

    public User findUserByName(String username){

        return userRepository.findByUsername(username);
    }

    public List<User> findAll(){
       return userRepository.findAll();
    }

    public User saveUser(User user){
      return  userRepository.save(user);
    }

    public User findUserById(long id){
        return userRepository.getOne(id);
    }

    public User updateUser(User user){
       return userRepository.saveAndFlush(user);
    }
    public void deleteUser(long id){
        userRepository.deleteById(id);
    }
}

