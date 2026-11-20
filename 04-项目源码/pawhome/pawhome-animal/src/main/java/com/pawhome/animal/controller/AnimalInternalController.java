package com.pawhome.animal.controller;

import cn.hutool.core.bean.BeanUtil;
import com.pawhome.animal.dto.AnimalVO;
import com.pawhome.animal.service.AnimalService;
import com.pawhome.api.dto.AnimalDTO;
import com.pawhome.common.core.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 内部接口，仅供其他微服务通过 Feign 调用。
 * <p>
 * 路径统一以 /internal 开头，网关不会把外部请求路由到这里，
 * 相当于一个"内网专区"。真实生产环境还会再加一层
 * 网络隔离（服务网格 / 安全组），防止内网端口被直接访问。
 */
@Tag(name = "动物服务-内部接口", description = "仅供服务间调用，不对外暴露")
@RestController
@RequestMapping("/internal/animals")
@RequiredArgsConstructor
public class AnimalInternalController {

    private final AnimalService animalService;

    @Operation(summary = "查询动物基础信息")
    @GetMapping("/{id}")
    public R<AnimalDTO> getById(@PathVariable Long id) {
        AnimalVO vo = animalService.detail(id, false);
        AnimalDTO dto = new AnimalDTO();
        BeanUtil.copyProperties(vo, dto);
        return R.ok(dto);
    }

    @Operation(summary = "更新领养状态")
    @PutMapping("/{id}/adopt-status")
    public R<Boolean> updateAdoptStatus(@PathVariable Long id, @RequestParam String status) {
        return R.ok(animalService.updateAdoptStatus(id, status));
    }

    @Operation(summary = "浏览量+1")
    @PutMapping("/{id}/view")
    public R<Void> increaseView(@PathVariable Long id) {
        animalService.increaseView(id);
        return R.ok(null);
    }
}
