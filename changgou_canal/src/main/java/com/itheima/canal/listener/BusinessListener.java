package com.itheima.canal.listener;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.itheima.canal.config.RabbitMQConfig;
import com.xpand.starter.canal.annotation.CanalEventListener;
import com.xpand.starter.canal.annotation.ListenPoint;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author ZJ
 */
@CanalEventListener //声明当前的类是canal的监听类
public class BusinessListener {

    //注入rabbitMQ的模板类
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     *
     * @param eventType     当前操作数据库的类型
     * @param rowData       当前操作数据库的数据
     */
    @ListenPoint(schema = "changgou_business", table = {"tb_ad"})   //设置监听changgou_business库,tb_ad表
    public void adUpdate(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        System.err.println("广告数据发生变化");

        //修改前数据
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            if (column.getName().equals("position")) {
                System.out.println("发送消息到mq  ad_update_queue:" + column.getValue());
                rabbitTemplate.convertAndSend("", "ad_update_queue", column.getValue());  //发送消息到mq
                break;
            }
        }

        //修改后数据
        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            if (column.getName().equals("position")) {
                System.out.println("发送消息到mq  ad_update_queue:" + column.getValue());
                rabbitTemplate.convertAndSend("", "ad_update_queue", column.getValue());  //发送消息到mq
                break;
            }
        }
    }
}
