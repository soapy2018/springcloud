package com.cqf.service;

import com.cqf.client.EurekaClientFeign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by cqf on 2019/8/20
 */
@Service
public class HiService {

    @Autowired
    EurekaClientFeign eurekaClientFeign;
    public String sayHi(String name){
        return  eurekaClientFeign.sayHiFromClientEureka(name);
    }
}
