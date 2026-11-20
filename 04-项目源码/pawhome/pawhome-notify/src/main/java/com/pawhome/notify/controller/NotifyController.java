package com.pawhome.notify.controller;

import com.pawhome.api.dto.NotifyMessageDTO;
import com.pawhome.common.core.PageResult;
import com.pawhome.common.core.R;
import com.pawhome.notify.entity.NotifyMessage;
import com.pawhome.notify.service.NotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "消息通知", description = "站内信、未读数、已读")
@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    @Operation(summary = "我的消息")
    @GetMapping("/messages")
    public R<PageResult<NotifyMessage>> myMessages(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Integer readFlag) {
        return R.ok(notifyService.pageMine(pageNum, pageSize, readFlag));
    }

    @Operation(summary = "未读消息数")
    @GetMapping("/unread-count")
    public R<Map<String, Object>> unreadCount() {
        return R.ok(Map.of("count", notifyService.unreadCount()));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/messages/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        notifyService.markRead(id);
        return R.ok("已读", null);
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/messages/read-all")
    public R<Void> markAllRead() {
        notifyService.markAllRead();
        return R.ok("全部已读", null);
    }

    @Operation(summary = "全部消息（管理端）")
    @GetMapping("/all")
    public R<PageResult<NotifyMessage>> all(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long receiverId,
            @RequestParam(required = false) String type) {
        return R.ok(notifyService.pageAll(pageNum, pageSize, receiverId, type));
    }
}
