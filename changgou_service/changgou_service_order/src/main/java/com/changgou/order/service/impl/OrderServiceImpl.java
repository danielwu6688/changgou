package com.changgou.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.entity.Result;
import com.changgou.entity.StatusCode;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.order.config.RabbitMQConfig;
import com.changgou.order.dao.OrderItemMapper;
import com.changgou.order.dao.OrderMapper;
import com.changgou.order.dao.TaskMapper;
import com.changgou.order.pojo.OrderItem;
import com.changgou.order.pojo.Task;
import com.changgou.order.service.CartService;
import com.changgou.order.service.OrderService;
import com.changgou.order.pojo.Order;
import com.changgou.util.IdWorker;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 查询全部列表
     * @return
     */
    @Override
    public List<Order> findAll() {
        return orderMapper.selectAll();
    }

    /**
     * 根据ID查询
     * @param id
     * @return
     */
    @Override
    public Order findById(String id){
        return  orderMapper.selectByPrimaryKey(id);
    }


    @Autowired
    private CartService cartService;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SkuFeign skuFeign;

    @Autowired
    private TaskMapper taskMapper;

    /**
     * 增加
     * @param order
     */
    @Override
    public void add(Order order){
        //1.获取购车的相关数据(redis)
        Map cartMap = cartService.list(order.getUsername());
        List<OrderItem> orderItemList = (List<OrderItem>) cartMap.get("orderItemList");

        //2.统计计算:总金额,总商品数量
        order.setTotalNum((Integer) cartMap.get("totalNum"));
        order.setTotalMoney((Integer) cartMap.get("totalMoney"));
        //3.填充订单数据,并保存到tb_order
        order.setPayMoney((Integer) cartMap.get("totalMoney"));
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        order.setBuyerRate("0");
        order.setSourceType("1");
        order.setOrderStatus("0");
        order.setPayStatus("0");
        order.setConsignStatus("0");
        String orderId = idWorker.nextId() + "";
        order.setId(orderId);

        orderMapper.insert(order);
        //4.tb_order_item,保存订单项中的每一个商品信息
        for (OrderItem orderItem : orderItemList) {
            orderItem.setId(idWorker.nextId() + "");
            orderItem.setIsReturn("0");
            orderItem.setOrderId(orderId);
            orderItemMapper.insertSelective(orderItem);
        }

        //扣减库存并增加销量
        skuFeign.decrCount(order.getUsername());

        //添加任务数据
        System.out.println("向订单数据库中的任务表去添加任务数据");
        Task task = new Task();
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        task.setMqExchange(RabbitMQConfig.EX_BUYING_ADDPOINTUSER);
        task.setMqRoutingkey(RabbitMQConfig.CG_BUYING_ADDPOINT_KEY);

        Map map = new HashMap();
        map.put("username", order.getUsername());
        map.put("orderId", orderId);
        map.put("point", order.getPayMoney());
        task.setRequestBody(JSON.toJSONString(map));

        taskMapper.insertSelective(task);

        //5.删除购物车数据(redis)
        redisTemplate.delete("cart_" + order.getUsername());

    }


    /**
     * 修改
     * @param order
     */
    @Override
    public void update(Order order){
        orderMapper.updateByPrimaryKey(order);
    }

    /**
     * 删除
     * @param id
     */
    @Override
    public void delete(String id){
        orderMapper.deleteByPrimaryKey(id);
    }


    /**
     * 条件查询
     * @param searchMap
     * @return
     */
    @Override
    public List<Order> findList(Map<String, Object> searchMap){
        Example example = createExample(searchMap);
        return orderMapper.selectByExample(example);
    }

    /**
     * 分页查询
     * @param page
     * @param size
     * @return
     */
    @Override
    public Page<Order> findPage(int page, int size){
        PageHelper.startPage(page,size);
        return (Page<Order>)orderMapper.selectAll();
    }

    /**
     * 条件+分页查询
     * @param searchMap 查询条件
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public Page<Order> findPage(Map<String,Object> searchMap, int page, int size){
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        return (Page<Order>)orderMapper.selectByExample(example);
    }

    /**
     * 合并订单
     * @param mainOrderId   主订单ID
     * @param fromOrderId   从订单ID
     */
    @Transactional
    @Override
    public Result merge(String mainOrderId, String fromOrderId) {
        //===============================判断两个订单能不能合并==============================
        //1.判断两个订单用户名是否相同
        Order mainOrder = orderMapper.selectByPrimaryKey(mainOrderId);
        Order fromOrder = orderMapper.selectByPrimaryKey(fromOrderId);
        if (mainOrder.getUsername() == fromOrder.getUsername()) {
            return new Result(false,StatusCode.ERROR,"不是同一个用户的订单啊啊啊啊!!!!!");
        }
        //2.判断订单是否已发货
        if (mainOrder.getConsignStatus().equals("1") || fromOrder.getConsignStatus().equals("1")) {
            return new Result(false,StatusCode.ERROR,"有订单已经发货啊啊啊啊啊啊!!!!!");
        }
        //3.判断订单是否完成
        if (!(mainOrder.getOrderStatus().equals("0") && fromOrder.getOrderStatus().equals("0"))) {
            return new Result(false,StatusCode.ERROR,"订单不是未完成订单啊啊啊!!!!!");
        }
        //4.判断订单是否已支付
        if (mainOrder.getPayStatus().equals("1") && fromOrder.getOrderStatus().equals("1")) {
            return new Result(false,StatusCode.ERROR,"订单未支付啊啊啊啊啊啊啊!!!!!");
        }

        //===============================合并订单项=======================================
        //1.根据id查询两个订单数据
        Example mainExample = new Example(OrderItem.class);
        Example.Criteria mainCriteria = mainExample.createCriteria();
        mainCriteria.andEqualTo("orderId", mainOrderId);
        List<OrderItem> mainOrderItems = orderItemMapper.selectByExample(mainExample);
        Example fromExample = new Example(OrderItem.class);
        Example.Criteria fromCriteria = fromExample.createCriteria();
        fromCriteria.andEqualTo("orderId", fromOrderId);
        List<OrderItem> fromOrderItems = orderItemMapper.selectByExample(fromExample);
        for (int i = 0; i < mainOrderItems.size(); i++) {
            OrderItem mainOrderItem = mainOrderItems.get(i);
            for (int j = 0; j < fromOrderItems.size(); j++) {
                OrderItem fromOrderItem = fromOrderItems.get(j);
                System.out.println(mainOrderItem.getSkuId());
                System.out.println(fromOrderItem.getSkuId());
                //1.1判断两个内是否有相同商品
                if (mainOrderItem.getSkuId().equals(fromOrderItem.getSkuId())) {
                    //1.2如果两个商品id相同
                    //主订单项现有数量=原主订单项该商品数量+从订单项该商品数量
                    mainOrderItem.setNum(mainOrderItem.getNum() + fromOrderItem.getNum());
                    //主订单项现总金额=原主订单项总金额+从订单项总金额
                    mainOrderItem.setMoney(mainOrderItem.getMoney() + fromOrderItem.getMoney());
                    //主订单项现实付金额=原主订单项实付金额+从订单项实付金额
                    mainOrderItem.setPayMoney(mainOrderItem.getPayMoney() + fromOrderItem.getPayMoney());
                    //主订单现项重量=原主订单项重量+从订单项重量
                    mainOrderItem.setWeight(mainOrderItem.getWeight() + fromOrderItem.getWeight());
                    //运费算不出来
                    //1.3更新主订单项
                    orderItemMapper.updateByPrimaryKey(mainOrderItem);
                    //删除从表订单项
                    orderItemMapper.delete(fromOrderItem);
                    //删除List集合中的值
                    fromOrderItems.remove(j);
                }
            }
        }
        //添加从订单项集合中剩下的不同订单项
        for (int i = 0; i < fromOrderItems.size(); i++) {
            OrderItem fromOrderItem = fromOrderItems.get(i);
            orderItemMapper.delete(fromOrderItem);
            fromOrderItem.setOrderId(mainOrderId);
            orderItemMapper.insertSelective(fromOrderItem);
        }

        //===============================更新订单==============================
        //2.数据库中删除从订单(魔法删除)
        fromOrder.setIsDelete("1");
        orderMapper.updateByPrimaryKeySelective(fromOrder);
        //3.更新主订单的订单信息
        //数量合计
        int totalNum = 0;
        //金额合计
        int totalMoney = 0;
        //实付金额
        int payMoney = 0;
        Example newMainExample = new Example(OrderItem.class);
        Example.Criteria newMainCriteria = newMainExample.createCriteria();
        newMainCriteria.andEqualTo("orderId", mainOrderId);
        List<OrderItem> newMainOrderItems = orderItemMapper.selectByExample(newMainExample);
        System.out.println("newMainOrderItems = " + newMainOrderItems.size());
        for (OrderItem newMainOrderItem : newMainOrderItems) {
            totalNum = totalNum + newMainOrderItem.getNum();
            System.out.println("totalNum = " + totalNum);
            totalMoney = totalMoney + newMainOrderItem.getMoney();
            System.out.println("totalMoney = " + totalMoney);
            payMoney = payMoney + newMainOrderItem.getPayMoney();
            System.out.println("payMoney = " + payMoney);
        }
        //优惠金额
        int preMoney = totalMoney - payMoney;
        mainOrder.setTotalNum(totalNum);
        mainOrder.setTotalMoney(totalMoney);
        mainOrder.setPayMoney(payMoney);
        mainOrder.setPreMoney(preMoney);
        //订单更新时间
        mainOrder.setUpdateTime(new Date());
        //更新主订单
        orderMapper.updateByPrimaryKeySelective(mainOrder);
        //返回订单合并成功
        return new Result(true,StatusCode.OK,"订单合并成功");
    }

    /**
     * 拆分订单
     * @param orderItemIds  订单项ID
     * @param orderItemNums 订单项数量
     * @return
     */
    @Override
    public Result split(String[] orderItemIds, ArrayList<String[]> orderItemNums) {
        for (int i = 0; i < orderItemIds.length; i++) {
            //需要拆分的订单项ID
            String orderItemId = orderItemIds[i];
            String[]orderItemNum = orderItemNums.get(i);

        }
        return null;
    }


    /**
     * 构建查询对象
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 订单id
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andEqualTo("id",searchMap.get("id"));
           	}
            // 支付类型，1、在线支付、0 货到付款
            if(searchMap.get("payType")!=null && !"".equals(searchMap.get("payType"))){
                criteria.andEqualTo("payType",searchMap.get("payType"));
           	}
            // 物流名称
            if(searchMap.get("shippingName")!=null && !"".equals(searchMap.get("shippingName"))){
                criteria.andLike("shippingName","%"+searchMap.get("shippingName")+"%");
           	}
            // 物流单号
            if(searchMap.get("shippingCode")!=null && !"".equals(searchMap.get("shippingCode"))){
                criteria.andLike("shippingCode","%"+searchMap.get("shippingCode")+"%");
           	}
            // 用户名称
            if(searchMap.get("username")!=null && !"".equals(searchMap.get("username"))){
                criteria.andLike("username","%"+searchMap.get("username")+"%");
           	}
            // 买家留言
            if(searchMap.get("buyerMessage")!=null && !"".equals(searchMap.get("buyerMessage"))){
                criteria.andLike("buyerMessage","%"+searchMap.get("buyerMessage")+"%");
           	}
            // 是否评价
            if(searchMap.get("buyerRate")!=null && !"".equals(searchMap.get("buyerRate"))){
                criteria.andLike("buyerRate","%"+searchMap.get("buyerRate")+"%");
           	}
            // 收货人
            if(searchMap.get("receiverContact")!=null && !"".equals(searchMap.get("receiverContact"))){
                criteria.andLike("receiverContact","%"+searchMap.get("receiverContact")+"%");
           	}
            // 收货人手机
            if(searchMap.get("receiverMobile")!=null && !"".equals(searchMap.get("receiverMobile"))){
                criteria.andLike("receiverMobile","%"+searchMap.get("receiverMobile")+"%");
           	}
            // 收货人地址
            if(searchMap.get("receiverAddress")!=null && !"".equals(searchMap.get("receiverAddress"))){
                criteria.andLike("receiverAddress","%"+searchMap.get("receiverAddress")+"%");
           	}
            // 订单来源：1:web，2：app，3：微信公众号，4：微信小程序  5 H5手机页面
            if(searchMap.get("sourceType")!=null && !"".equals(searchMap.get("sourceType"))){
                criteria.andEqualTo("sourceType",searchMap.get("sourceType"));
           	}
            // 交易流水号
            if(searchMap.get("transactionId")!=null && !"".equals(searchMap.get("transactionId"))){
                criteria.andLike("transactionId","%"+searchMap.get("transactionId")+"%");
           	}
            // 订单状态
            if(searchMap.get("orderStatus")!=null && !"".equals(searchMap.get("orderStatus"))){
                criteria.andEqualTo("orderStatus",searchMap.get("orderStatus"));
           	}
            // 支付状态
            if(searchMap.get("payStatus")!=null && !"".equals(searchMap.get("payStatus"))){
                criteria.andEqualTo("payStatus",searchMap.get("payStatus"));
           	}
            // 发货状态
            if(searchMap.get("consignStatus")!=null && !"".equals(searchMap.get("consignStatus"))){
                criteria.andEqualTo("consignStatus",searchMap.get("consignStatus"));
           	}
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andEqualTo("isDelete",searchMap.get("isDelete"));
           	}

            // 数量合计
            if(searchMap.get("totalNum")!=null ){
                criteria.andEqualTo("totalNum",searchMap.get("totalNum"));
            }
            // 金额合计
            if(searchMap.get("totalMoney")!=null ){
                criteria.andEqualTo("totalMoney",searchMap.get("totalMoney"));
            }
            // 优惠金额
            if(searchMap.get("preMoney")!=null ){
                criteria.andEqualTo("preMoney",searchMap.get("preMoney"));
            }
            // 邮费
            if(searchMap.get("postFee")!=null ){
                criteria.andEqualTo("postFee",searchMap.get("postFee"));
            }
            // 实付金额
            if(searchMap.get("payMoney")!=null ){
                criteria.andEqualTo("payMoney",searchMap.get("payMoney"));
            }

        }
        return example;
    }

}
