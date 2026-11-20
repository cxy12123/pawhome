package com.pawhome.api.feign;

import com.pawhome.api.dto.NotifyMessageDTO;
import com.pawhome.api.feign.fallback.NotifyClientFallbackFactory;
import com.pawhome.common.core.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 通知服务 Feign 客户端。
 * <p>
 * 通知属于"锦上添花"的非核心链路：即使 notify 服务挂了，
 * 领养审核也必须成功。因此这里配置了 fallback，
 * 调用失败只记录日志，不向上抛异常。
 */
@FeignClient(
        name = "pawhome-notify",
        contextId = "notifyClient",
        path = "/internal/notify",
        fallbackFactory = NotifyClientFallbackFactory.class
)
public interface NotifyClient {

    /** 发送一条站内信 */
    @PostMapping("/send")
    R<Long> send(@RequestBody NotifyMessageDTO message);
}
