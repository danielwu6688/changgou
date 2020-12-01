package com.changgou.order.service;

import java.util.Map;

public interface CartService {

    /**
     * 添加购物车
     * @param skuId 商品skuId
     * @param num   商品数量
     * @param username  用户名
     */
    void addCart(String skuId, Integer num, String username);

    /**
     * 查询购物车
     * @param username 用户名
     * @return 购物车信息
     */
    Map list(String username);
}
