package com.pawhome.animal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图片上传相关配置。
 * <p>
 * 目录用相对路径，默认落在模块目录下的 uploads（运行时工作目录是 target，
 * 所以 ../uploads 就是 pawhome-animal/uploads）。
 * 放在 target 外面是故意的：mvn clean 只删 target，上传的图片不会跟着没。
 */
@Data
@Component
@ConfigurationProperties(prefix = "pawhome.upload")
public class UploadProperties {

    /** 上传根目录，可用环境变量 UPLOAD_DIR 覆盖 */
    private String dir = "../uploads";

    /** 单张图片大小上限（字节），默认 5MB */
    private long maxSize = 5 * 1024 * 1024L;
}
