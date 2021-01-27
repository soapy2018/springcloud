package com.cqf.dto;

import com.cqf.entity.Blog;
import com.cqf.entity.User;

/**
 * Created by cqf on 2019/10/9
 */
public class BlogDetailDTO {
    private Blog blog;
    private User user;

    public Blog getBlog() {
        return blog;
    }

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
