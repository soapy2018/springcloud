package com.cqf.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Created by cqf on 2019/8/12
 */
@Service
public class RibbonService {

    @Autowired
    RestTemplate restTemplate;

    public String hi(String name) {
        return restTemplate.getForObject("http://eureka-client/hi?name="+name,String.class);
    }
}
