package com.changgou.goods.dao;

import com.changgou.goods.pojo.Spec;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

public interface SpecMapper extends Mapper<Spec> {
    /**
     * 根据商品名称查询规格列表
     * @param categoryName
     * @return
     */
    @Select("SELECT NAME,options FROM tb_spec WHERE template_id IN (SELECT template_id FROM tb_category WHERE name = #{categoryName})")
    List<Map> findSpecListByCategoryName(@Param("categoryName") String categoryName);
}
