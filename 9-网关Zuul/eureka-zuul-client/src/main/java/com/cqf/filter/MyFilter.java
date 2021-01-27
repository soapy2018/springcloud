package com.cqf.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.ZuulServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * Created by cqf on 2019/8/20
 */
@Component
public class MyFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(MyFilter.class);
    @Override
    public String filterType() { //过滤器类型，有四种，分别是“pre”，“post”，“routing”，“error”
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() { //过滤顺序，Int类型值，值越小，越早执行该过滤器
        return 0;
    }

    @Override
    public boolean shouldFilter() { //表示该过滤器是否过滤逻辑，如果为true，则执行run()方法，如果为false则不执行
        return true;
    }

    @Override
    public Object run() { //具体的过滤逻辑
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        log.info(String.format("%s >>> %s", request.getMethod(), request.getRequestURL().toString()));
        Object accessToken = request.getParameter("token");
        if(accessToken == null) {
            log.warn("token is empty");
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(401);
            try {
                ctx.getResponse().getWriter().write("token is empty");
            }catch (Exception e){}

            return null;
        }
        log.info("ok");
        return null;
    }
}