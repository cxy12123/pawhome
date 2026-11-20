package com.pawhome.api.feign;

import com.pawhome.api.dto.AnimalDTO;
import com.pawhome.api.feign.fallback.AnimalClientFallbackFactory;
import com.pawhome.common.core.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 动物服务 Feign 客户端。
 * <p>
 * name 写的是 Nacos 中的服务名，不是 IP 和端口。
 * 这样 animal 服务扩容到 3 个实例时，这里一行代码都不用改，
 * LoadBalancer 会自动轮询到不同实例。
 * <p>
 * 所有 /internal/** 接口是服务间专用，网关不对外暴露，
 * 避免外部绕过业务校验直接改数据。
 */
@FeignClient(
        name = "pawhome-animal",
        contextId = "animalClient",
        path = "/internal/animals",
        fallbackFactory = AnimalClientFallbackFactory.class
)
public interface AnimalClient {

    /** 查询动物详情 */
    @GetMapping("/{id}")
    R<AnimalDTO> getById(@PathVariable("id") Long id);

    /** 更新动物领养状态 */
    @PutMapping("/{id}/adopt-status")
    R<Boolean> updateAdoptStatus(@PathVariable("id") Long id, @RequestParam("status") String status);

    /** 累加浏览量（非核心链路，失败可忽略） */
    @PutMapping("/{id}/view")
    R<Void> increaseView(@PathVariable("id") Long id);
}
