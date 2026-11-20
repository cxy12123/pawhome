package com.pawhome.animal.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 把上传目录挂成静态资源。
 * <p>
 * 浏览器访问 /api/animal/uploads/xxx.jpg 时，网关去掉 /api 前缀，
 * 服务端按 /animal/uploads/xxx.jpg 匹配这里的映射，直接读磁盘文件。
 * 这样图片既不占 jar 体积，也不用每次上传都重新打包。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class UploadWebConfig implements WebMvcConfigurer {

    private final UploadProperties properties;

    private Path root;

    @PostConstruct
    public void init() {
        root = Paths.get(properties.getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            log.info("上传目录已就绪：{}", root);
        } catch (IOException e) {
            log.error("创建上传目录失败：{}", root, e);
        }
    }

    public Path getRoot() {
        return root;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/animal/uploads/**")
                .addResourceLocations(root.toUri().toString());
    }
}
