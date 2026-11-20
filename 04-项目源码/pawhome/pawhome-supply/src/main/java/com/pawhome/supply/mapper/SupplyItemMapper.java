package com.pawhome.supply.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pawhome.supply.entity.SupplyItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SupplyItemMapper extends BaseMapper<SupplyItem> {

    /**
     * CAS 方式扣减库存。
     * <p>
     * 关键在 WHERE 里的 stock >= #{qty}：
     * 数据库行锁保证这条 UPDATE 原子执行，库存不足时影响行数为 0，
     * 调用方据此判断是否扣减成功。这是不用分布式锁也能防超卖的方案。
     * <p>
     * 本项目同时使用 Redisson 锁 + 这条 CAS SQL，属于"双保险"：
     * 锁负责把并发串行化、避免大量无效重试，CAS 负责最终的数据正确性。
     */
    @Update("UPDATE t_supply_item SET stock = stock - #{qty}, update_time = NOW() " +
            "WHERE id = #{id} AND stock >= #{qty} AND deleted = 0")
    int deductStock(@Param("id") Long id, @Param("qty") Integer qty);

    /** 原子增加库存 */
    @Update("UPDATE t_supply_item SET stock = stock + #{qty}, update_time = NOW() " +
            "WHERE id = #{id} AND deleted = 0")
    int addStock(@Param("id") Long id, @Param("qty") Integer qty);
}
