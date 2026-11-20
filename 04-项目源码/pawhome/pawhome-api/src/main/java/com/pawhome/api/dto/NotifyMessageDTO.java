package com.pawhome.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 发送通知消息的请求对象。
 * 各服务需要通知用户时都走 notify 服务，而不是各自写一套发送逻辑，
 * 将来要接短信/邮件/企业微信，只改 notify 一个服务即可。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyMessageDTO implements Serializable {

    /** 接收人用户ID */
    private Long receiverId;
    /** 接收人姓名 */
    private String receiverName;
    /** 标题 */
    private String title;
    /** 内容 */
    private String content;
    /** 类型：SYSTEM/ADOPTION/SUPPLY/DONATION/VOLUNTEER */
    private String type;
    /** 业务类型，前端据此跳转 */
    private String bizType;
    /** 业务单据ID */
    private Long bizId;
    /** 通道：INNER/SMS/EMAIL */
    private String channel;
}
