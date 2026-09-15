package com.aiarticle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@SpringBootApplication
public class AiArticleMakeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiArticleMakeApplication.class, args);
    }

}