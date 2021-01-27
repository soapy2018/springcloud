package com.cqf.service;

import com.cqf.entity.Blog;

import java.util.List;

/**
 * Created by cqf on 2019/9/22
 */
public interface IBlogService {
    List<Blog> getBlogs();
    void deleteBlog(long id);
}
