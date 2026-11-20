package com.pawhome.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花 ID 精度修复。
 *
 * <p>MyBatis-Plus 默认用 ASSIGN_ID 生成 19 位 Long 主键，例如
 * 2121608906703630337。这个值超过了 JavaScript Number 的安全整数范围
 * （2^53-1，约 9.007e15），浏览器 JSON.parse 之后末几位会被抹平，
 * 前端再把这个 id 传回后端时就不是原来那条记录了。
 *
 * <p>典型症状：领养申请列表能显示，但点「审核」提示「申请单不存在」；
 * 库存记录、捐赠单等新增数据也会出现同样的"查不到"现象。
 *
 * <p>这里把所有 Long 包装类型统一序列化成字符串，前端原样带着走即可，
 * 后端反序列化时仍会按 Long 接收。基本类型 long（如分页 total）
 * 不受影响，前端分页组件拿到的依然是数字。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance);
    }
}
