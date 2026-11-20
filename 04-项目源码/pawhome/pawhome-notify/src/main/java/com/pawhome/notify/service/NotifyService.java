package com.pawhome.notify.service;

import com.pawhome.api.dto.NotifyMessageDTO;
import com.pawhome.common.core.PageResult;
import com.pawhome.notify.entity.NotifyMessage;

public interface NotifyService {

    /** 发送消息（供其他服务调用） */
    Long send(NotifyMessageDTO dto);

    /** 我的消息 */
    PageResult<NotifyMessage> pageMine(long pageNum, long pageSize, Integer readFlag);

    /** 全部消息（管理端） */
    PageResult<NotifyMessage> pageAll(long pageNum, long pageSize, Long receiverId, String type);

    /** 未读数 */
    long unreadCount();

    /** 标记单条已读 */
    void markRead(Long id);

    /** 全部标记已读 */
    void markAllRead();
}
