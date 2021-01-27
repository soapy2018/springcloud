package com.cqf.dao;


import com.cqf.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by cqf on 2019/10/9
 */

public interface UserDao extends JpaRepository<User, Long> {

	User findByUsername(String username);
}
