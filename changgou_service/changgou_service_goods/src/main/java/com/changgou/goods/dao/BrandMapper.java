package com.changgou.goods.dao;

import com.changgou.goods.pojo.Brand;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

public interface BrandMapper extends Mapper<Brand> {
    @Select("SELECT \n" +
            "\tNAME,image \n" +
            "FROM\n" +
            "\ttb_brand \n" +
            "WHERE\n" +
            "\tid IN ( SELECT brand_id FROM tb_category_brand WHERE category_id IN ( SELECT id FROM tb_category WHERE NAME = #{categoryName} ) )")
    List<Map> findBrandListBycategoryName(@Param("categoryName") String categoryName);
}
