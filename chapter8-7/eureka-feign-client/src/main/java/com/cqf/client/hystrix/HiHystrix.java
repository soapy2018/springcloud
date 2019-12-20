package com.cqf.client.hystrix;

import com.cqf.client.EurekaClientFeign;
import org.springframework.stereotype.Component;

/**
 * Created by cqf on 2019/8/19
 */
@Component
public class HiHystrix implements EurekaClientFeign {
    @Override
    public String sayHiFromClientEureka(String name) {
           return "hi,"+name+",sorry,error!";
    }
}
