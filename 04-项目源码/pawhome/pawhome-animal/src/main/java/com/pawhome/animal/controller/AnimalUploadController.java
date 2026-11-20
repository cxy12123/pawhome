package com.pawhome.animal.controller;

import com.pawhome.animal.config.UploadProperties;
import com.pawhome.animal.config.UploadWebConfig;
import com.pawhome.common.core.BizException;
import com.pawhome.common.core.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * 动物封面图上传。
 * <p>
 * 为什么放在 animal 服务而不是网关：网关是 WebFlux 网关，定位是路由与鉴权，
 * 不适合处理 multipart 落盘；图片属于"动物档案"的附属资源，跟着业务服务走更自然。
 */
@Slf4j
@Tag(name = "动物档案-图片上传", description = "上传封面图，返回可直接访问的地址")
@RestController
@RequestMapping("/animal")
@RequiredArgsConstructor
public class AnimalUploadController {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif");

    private final UploadWebConfig uploadWebConfig;
    private final UploadProperties uploadProperties;

    @Operation(summary = "上传动物封面图",
            description = "支持 jpg / png / gif，单张不超过 5MB；返回 /api/animal/uploads/xxx 形式的地址")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<String> upload(@RequestPart("file") MultipartFile file) {
        BizException.throwIf(file == null || file.isEmpty(), "请选择要上传的图片");

        long maxMb = uploadProperties.getMaxSize() / 1024 / 1024;
        BizException.throwIf(file.getSize() > uploadProperties.getMaxSize(),
                "图片不能超过 " + maxMb + "MB");

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "";
        BizException.throwIf(!ALLOWED_EXT.contains(ext), "只支持 jpg / png / gif 格式的图片");

        // 真读一遍再落盘：防止把改了后缀的其它文件传上来
        try (InputStream in = file.getInputStream()) {
            BizException.throwIf(ImageIO.read(in) == null, "文件内容不是有效图片");
        } catch (IOException e) {
            throw new BizException("读取图片失败，请重新选择");
        }

        // 文件名由服务端生成（UUID），既避免重名覆盖，也杜绝了路径穿越
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = uploadWebConfig.getRoot().resolve(filename);
        try {
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("保存图片失败：{}", target, e);
            throw new BizException("保存图片失败，请重试");
        }

        log.info("图片已上传：{}（{} KB）", target, file.getSize() / 1024);
        return R.ok("上传成功", "/api/animal/uploads/" + filename);
    }
}
