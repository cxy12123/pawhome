package com.pawhome.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 * <p>
 * 三个插件各有用处：
 * 1. 分页插件：不装的话 page 参数无效，会一次查全表；
 * 2. 防全表更新删除插件：拦截没有 where 条件的 update/delete，
 *    避免手误把整张表改成同一个值；
 * 3. 乐观锁插件（本项目按需开启，库存扣减场景用分布式锁更直观）。
 */
@Configuration
@MapperScan("com.pawhome.**.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大 200 条，防止前端传 pageSize=100000 拖垮数据库
        pagination.setMaxLimit(200L);
        // 超过最大页数时返回空集合而不是回到首页
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);

        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }
}
