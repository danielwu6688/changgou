package com.itheima.canal.listener;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.itheima.canal.config.RabbitMQConfig;
import com.xpand.starter.canal.annotation.CanalEventListener;
import com.xpand.starter.canal.annotation.ListenPoint;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ZJ
 */
//声明这个类是canal的监听类
@CanalEventListener
public class SpuListener {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //声明这个方法监听哪一个库,哪一张表
    @ListenPoint(schema = "changgou_goods",table = "tb_spu")
    public void goodsUp(CanalEntry.EntryType entryType,CanalEntry.RowData rowData){
        //获取改变之前的数据,并且将这一部分数据转换添加到map
        HashMap<String, String> oldData = new HashMap<>();
        rowData.getBeforeColumnsList().forEach(column -> oldData.put(column.getName(), column.getValue()));
        //获取改变之后的数据,并且将这一部分数据转换添加到map
        HashMap<String, String> newData = new HashMap<>();
        rowData.getAfterColumnsList().forEach(column -> newData.put(column.getName(), column.getValue()));

        //获取最新上架的商品
        //改变之前上架状态为0
        //之后上架状态为1
        if ("0".equals(oldData.get("is_marketable"))&&"1".equals(newData.get("is_marketable"))) {
            //将新上架商品的id发送到rabbitmq
            rabbitTemplate.convertAndSend(RabbitMQConfig.GOODS_UP_EXCHANGE, "", newData.get("id"));
        }

        //获取最新下架的商品
        if ("1".equals(oldData.get("is_marketable")) && "0".equals(newData.get("is_marketable"))) {
            //将新下架的商品id发送到rabbitmq
            rabbitTemplate.convertAndSend(RabbitMQConfig.GOODS_DOWN_EXCHANGE,"",newData.get("id"));
        }

        //获取最新被审核通过的商品
        if ("0".equals(oldData.get("status")) && "1".equals(newData.get("status"))) {
            //将商品的spuId发送到MQ
            rabbitTemplate.convertAndSend(RabbitMQConfig.GOODS_UP_EXCHANGE,"",newData.get("id"));
        }
    }
}
