package com.pawhome.auth.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pawhome.auth.entity.User;
import com.pawhome.auth.mapper.UserMapper;
import com.pawhome.auth.security.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 初始密码升级器。
 * <p>
 * SQL 脚本里的初始密码以 {noop}123456 形式写入，方便文档审阅时确认密码。
 * 服务首次启动时，这个 Runner 会把所有 {noop} 开头的记录加密成 BCrypt，
 * 之后数据库里就再没有明文密码了。
 * <p>
 * 这个设计的价值在于：
 * - 部署即安全，不依赖运维手工执行加密脚本；
 * - 幂等，重启多次不会重复加密（加密后前缀变成 {bcrypt}，不会被再次扫描到）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordUpgradeRunner implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .likeRight(User::getPassword, "{noop}"));

            if (users.isEmpty()) {
                return;
            }

            for (User user : users) {
                String raw = user.getPassword().substring("{noop}".length());
                User update = new User();
                update.setId(user.getId());
                update.setPassword(passwordEncoder.encode(raw));
                userMapper.updateById(update);
                log.info("初始账号 {} 的密码已升级为 BCrypt 存储", user.getUsername());
            }
            log.info("本次启动共升级 {} 个账号的密码", users.size());
        } catch (Exception e) {
            // 升级失败不影响服务启动，但要在日志里明确暴露出来
            log.error("初始密码升级失败，请检查数据库连接：{}", e.getMessage());
        }
    }
}
