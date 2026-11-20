package com.pawhome.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pawhome.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。
 * <p>
 * 继承 BaseMapper 后，单表 CRUD 全部由 MyBatis-Plus 生成，
 * 只有复杂统计才需要自己写 SQL。本项目绝大多数表都是这个模式。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
