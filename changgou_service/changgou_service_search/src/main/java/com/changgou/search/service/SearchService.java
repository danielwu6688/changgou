package com.changgou.search.service;

import java.util.Map;

public interface SearchService {

    /**
     * 按照查询条件进行数据查询
     * @param searchMap 查询参数
     * @return 查询出来的结果集
     */
    public Map search(Map<String, String> searchMap);
}
