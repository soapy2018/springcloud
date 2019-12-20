package com.cqf.web;

import com.cqf.service.HiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by cqf on 2019/8/20
 */
@RestController
public class HiController {
    @Autowired
    HiService hiService;
    @GetMapping("/hi")
    public String sayHi(@RequestParam( defaultValue = "cqf",required = false)String name){
        return hiService.sayHi(name);
    }
}
