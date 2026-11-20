package com.pawhome.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pawhome.api.dto.NotifyMessageDTO;
import com.pawhome.common.context.UserContext;
import com.pawhome.common.core.BizException;
import com.pawhome.common.core.PageResult;
import com.pawhome.notify.entity.NotifyMessage;
import com.pawhome.notify.mapper.NotifyMessageMapper;
import com.pawhome.notify.service.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final NotifyMessageMapper messageMapper;

    @Override
    public Long send(NotifyMessageDTO dto) {
        BizException.throwIf(dto.getReceiverId() == null, "接收人不能为空");
        BizException.throwIf(StrUtil.isBlank(dto.getTitle()), "消息标题不能为空");

        NotifyMessage message = new NotifyMessage();
        message.setReceiverId(dto.getReceiverId());
        message.setReceiverName(dto.getReceiverName());
        message.setTitle(dto.getTitle());
        message.setContent(dto.getContent());
        message.setType(StrUtil.isBlank(dto.getType()) ? "SYSTEM" : dto.getType());
        message.setBizType(dto.getBizType());
        message.setBizId(dto.getBizId());
        message.setChannel(StrUtil.isBlank(dto.getChannel()) ? "INNER" : dto.getChannel());
        message.setReadFlag(0);
        messageMapper.insert(message);

        // 短信/邮件通道在这里扩展，本实验用站内信模拟
        if (!"INNER".equals(message.getChannel())) {
            log.info("【模拟{}】发送给 {}：{}", message.getChannel(), message.getReceiverName(), message.getTitle());
        }
        return message.getId();
    }

    @Override
    public PageResult<NotifyMessage> pageMine(long pageNum, long pageSize, Integer readFlag) {
        Long userId = UserContext.requireUserId();
        Page<NotifyMessage> page = messageMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<NotifyMessage>()
                        .eq(NotifyMessage::getReceiverId, userId)
                        .eq(readFlag != null, NotifyMessage::getReadFlag, readFlag)
                        .orderByDesc(NotifyMessage::getCreateTime));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public PageResult<NotifyMessage> pageAll(long pageNum, long pageSize, Long receiverId, String type) {
        Page<NotifyMessage> page = messageMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<NotifyMessage>()
                        .eq(receiverId != null, NotifyMessage::getReceiverId, receiverId)
                        .eq(StrUtil.isNotBlank(type), NotifyMessage::getType, type)
                        .orderByDesc(NotifyMessage::getCreateTime));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public long unreadCount() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return 0;
        }
        Long count = messageMapper.selectCount(new LambdaQueryWrapper<NotifyMessage>()
                .eq(NotifyMessage::getReceiverId, userId)
                .eq(NotifyMessage::getReadFlag, 0));
        return count == null ? 0 : count;
    }

    @Override
    public void markRead(Long id) {
        Long userId = UserContext.requireUserId();
        NotifyMessage message = messageMapper.selectById(id);
        BizException.throwIfNull(message, "消息不存在");
        if (!userId.equals(message.getReceiverId())) {
            throw new BizException("只能操作自己的消息");
        }
        if (message.getReadFlag() != null && message.getReadFlag() == 1) {
            return;
        }
        NotifyMessage update = new NotifyMessage();
        update.setId(id);
        update.setReadFlag(1);
        update.setReadTime(LocalDateTime.now());
        messageMapper.updateById(update);
    }

    @Override
    public void markAllRead() {
        Long userId = UserContext.requireUserId();
        messageMapper.update(null, new LambdaUpdateWrapper<NotifyMessage>()
                .eq(NotifyMessage::getReceiverId, userId)
                .eq(NotifyMessage::getReadFlag, 0)
                .set(NotifyMessage::getReadFlag, 1)
                .set(NotifyMessage::getReadTime, LocalDateTime.now()));
    }
}
