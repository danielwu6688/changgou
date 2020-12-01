package com.changgou.goods.feign;

import com.changgou.entity.Result;
import com.changgou.goods.pojo.Sku;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@FeignClient(name = "goods")
public interface SkuFeign {

    /**
     * 根据spuId搜索skuList
     * @param spuId 商品id
     * @return  商品属性列表
     */
    @GetMapping("/sku/spu/{spuId}")
    List<Sku> findSkuListBySpuId(@PathVariable("spuId") String spuId);

    /**
     * 根据skuId搜索sku
     * @param id
     * @return
     */
    @GetMapping("/sku/{id}")
    public Result<Sku> findById(@PathVariable String id);

    /**
     * 根据用户名扣减库存
     * @param username
     * @return
     */
    @PostMapping("/decr/count")
    public Result decrCount(@RequestParam("username") String username);
}
