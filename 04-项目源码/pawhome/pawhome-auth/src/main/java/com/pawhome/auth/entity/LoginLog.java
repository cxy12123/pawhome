package com.pawhome.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体。
 * <p>
 * 记录每一次登录尝试（含失败），用于风控和审计。
 * 连续多次失败可以触发账号锁定，这在实验报告中作为安全加固点讨论。
 */
@Data
@TableName("t_login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;
    private String loginIp;
    private String userAgent;

    /** 1 成功 0 失败 */
    private Integer success;

    private String failReason;
    private LocalDateTime loginTime;
}
