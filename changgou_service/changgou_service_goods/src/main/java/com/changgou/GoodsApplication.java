package com.changgou;

import com.changgou.util.IdWorker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@EnableEurekaClient
@MapperScan(basePackages = {"com.changgou.goods.dao"})
public class GoodsApplication {
    public static void main(String[] args) {
        SpringApplication.run( GoodsApplication.class);
//        ClassLoader classLoader = GoodsApplication.class.getClassLoader();
//        System.out.println("classLoader = " + classLoader);
//        ClassLoader classLoaderParent = classLoader.getParent();
//        System.out.println("classLoaderParent = " + classLoaderParent);
//        ClassLoader classLoaderParentParent = classLoaderParent.getParent();
//        System.out.println("classLoaderParentParent = " + classLoaderParentParent);
    }

    @Value("${workId}")
    private int workId;

    @Value("${datacenterId}")
    private int datacenterId;

    @Bean
    public IdWorker idWorker(){
        return new IdWorker(workId, datacenterId);
    }


}
