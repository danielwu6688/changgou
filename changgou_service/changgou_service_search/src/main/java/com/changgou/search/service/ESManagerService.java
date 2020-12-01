package com.changgou.search.service;

public interface ESManagerService {

    //创建索引库结构
    void createIndexAndMapping();

    //导入全部数据进入es
    void importAll();

    //根据spuId查询skuList,再导入索引库
    void importDataToESBySpuId(String spuId);

    //根据spuId删除es索引库中相关的sku数据
    void delDataBySpuId(String spuId);

}
