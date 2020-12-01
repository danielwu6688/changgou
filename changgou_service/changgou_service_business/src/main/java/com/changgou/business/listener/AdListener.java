package com.changgou.business.listener;

import com.alibaba.fastjson.util.JavaBeanInfo;
import okhttp3.*;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

//这个类交给spring管理
@Component
//监听rabbitMQ里面的队列消息
@RabbitListener(queues = "ad_update_queue")
public class AdListener {

    @RabbitHandler
    public void receiveMessage(String message) {
        System.out.println("接收到的消息为:" + message);

        //发起远程调用
        OkHttpClient okHttpClient = new OkHttpClient();
        //发起远程访问
        //请求路径
        String url = "http://192.168.200.128/ad_update?position=" + message;
        //创建Request请求
        Request request = new Request.Builder().url(url).build();
        //创建连接
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            //请求失败
            @Override
            public void onFailure(Call call, IOException e) {
                //打印错误信息
                e.printStackTrace();
            }

            //请求成功
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //输出响应信息
                System.out.println("请求成功:" + response.message());
            }
        });
    }
}
