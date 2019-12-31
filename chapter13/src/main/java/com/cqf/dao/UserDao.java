package com.cqf.dao;
import com.cqf.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserDao extends JpaRepository<User, Long>{

	User findByUsername(String username);
}
