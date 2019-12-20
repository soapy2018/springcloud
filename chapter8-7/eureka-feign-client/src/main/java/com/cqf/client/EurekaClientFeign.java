package com.cqf.client;

import com.cqf.client.config.FeignConfig;
import com.cqf.client.hystrix.HiHystrix;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by cqf on 2019/8/19
 */
@FeignClient(value = "eureka-client",configuration = FeignConfig.class,fallback = HiHystrix.class)
public interface EurekaClientFeign {
    @GetMapping(value = "/hi")
    String sayHiFromClientEureka(@RequestParam(value = "name") String name);
}
