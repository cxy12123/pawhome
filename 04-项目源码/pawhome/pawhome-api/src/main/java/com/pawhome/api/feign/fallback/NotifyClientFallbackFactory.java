package com.pawhome.api.feign.fallback;

import com.pawhome.api.dto.NotifyMessageDTO;
import com.pawhome.api.feign.NotifyClient;
import com.pawhome.common.core.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 通知服务降级工厂。
 * <p>
 * 通知失败绝不能阻塞主流程：审核该通过还是要通过，
 * 大不了用户晚一点收到消息。所以这里返回 ok，
 * 只在日志里留痕，方便事后补发。
 */
@Slf4j
@Component
public class NotifyClientFallbackFactory implements FallbackFactory<NotifyClient> {

    @Override
    public NotifyClient create(Throwable cause) {
        log.error("调用通知服务失败，消息可能未送达：{}", cause.getMessage());
        return message -> {
            log.warn("通知降级，丢弃消息：receiverId={}, title={}", message.getReceiverId(), message.getTitle());
            return R.ok(0L);
        };
    }
}
