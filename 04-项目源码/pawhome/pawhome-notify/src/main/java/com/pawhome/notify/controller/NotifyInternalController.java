package com.pawhome.notify.controller;

import com.pawhome.api.dto.NotifyMessageDTO;
import com.pawhome.common.core.R;
import com.pawhome.notify.service.NotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 内部接口，供其他微服务通过 Feign 发消息。
 */
@Tag(name = "通知服务-内部接口", description = "仅供服务间调用")
@RestController
@RequestMapping("/internal/notify")
@RequiredArgsConstructor
public class NotifyInternalController {

    private final NotifyService notifyService;

    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public R<Long> send(@RequestBody NotifyMessageDTO dto) {
        return R.ok(notifyService.send(dto));
    }
}
