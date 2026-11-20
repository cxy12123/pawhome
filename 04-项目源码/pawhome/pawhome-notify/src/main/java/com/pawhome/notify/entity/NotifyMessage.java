package com.pawhome.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_notify_message")
public class NotifyMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long receiverId;
    private String receiverName;
    private String title;
    private String content;

    /** SYSTEM / ADOPTION / SUPPLY / DONATION / VOLUNTEER */
    private String type;

    private String bizType;
    private Long bizId;

    /** INNER / SMS / EMAIL */
    private String channel;

    private Integer readFlag;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}
