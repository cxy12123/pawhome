package com.pawhome.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体。
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    /** 存储格式：{bcrypt}$2a$...，前缀标明使用的编码器 */
    private String password;

    private String realName;
    private String phone;
    private String email;
    private String avatar;

    /** ADMIN / STAFF / VOLUNTEER / ADOPTER */
    private String role;

    private Long stationId;

    /** 1 启用 0 停用 */
    private Integer status;

    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** MyBatis-Plus 逻辑删除字段，查询时自动追加 deleted = 0 */
    @TableLogic
    private Integer deleted;
}
