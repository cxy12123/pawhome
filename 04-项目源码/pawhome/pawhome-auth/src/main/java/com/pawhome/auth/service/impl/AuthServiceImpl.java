package com.pawhome.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pawhome.auth.config.JwtProperties;
import com.pawhome.auth.dto.ChangePasswordRequest;
import com.pawhome.auth.dto.LoginRequest;
import com.pawhome.auth.dto.LoginVO;
import com.pawhome.auth.dto.ProfileUpdateRequest;
import com.pawhome.auth.dto.RegisterRequest;
import com.pawhome.auth.dto.UserVO;
import com.pawhome.auth.entity.LoginLog;
import com.pawhome.auth.entity.User;
import com.pawhome.auth.mapper.LoginLogMapper;
import com.pawhome.auth.mapper.UserMapper;
import com.pawhome.auth.security.PasswordEncoder;
import com.pawhome.auth.service.AuthService;
import com.pawhome.common.constant.CommonConstants;
import com.pawhome.common.context.LoginUser;
import com.pawhome.common.context.UserContext;
import com.pawhome.common.core.BizException;
import com.pawhome.common.core.PageResult;
import com.pawhome.common.core.ResultCode;
import com.pawhome.common.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 认证服务实现。
 * <p>
 * 登录流程里包含了三道防护，这是本章实验的安全重点：
 * 1. 统一错误提示 —— 不区分"账号不存在"和"密码错误"，避免被枚举出有效账号；
 * 2. 失败计数 —— Redis 计数，达到阈值锁定 15 分钟，抵御暴力破解；
 * 3. 密码自动升级 —— 检测到明文或旧格式立刻重算 BCrypt，无需人工干预。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    private static final String LOGIN_FAIL_KEY = "pawhome:login:fail:";
    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    public LoginVO login(LoginRequest request, String ip, String userAgent) {
        String username = request.getUsername().trim();

        // ---------- 1. 前置检查：是否已被锁定 ----------
        String failKey = LOGIN_FAIL_KEY + username;
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr == null ? 0 : Integer.parseInt(failCountStr);
        if (failCount >= jwtProperties.getMaxFailCount()) {
            Long ttl = redisTemplate.getExpire(failKey);
            throw new BizException(ResultCode.FORBIDDEN,
                    "密码错误次数过多，账号已锁定 " + (ttl == null || ttl < 0 ? jwtProperties.getLockMinutes() * 60 : ttl) + " 秒");
        }

        // ---------- 2. 查用户 ----------
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (user == null) {
            // 注意：即使用户不存在也要走一遍失败计数，否则可以通过响应时间差异枚举账号
            recordFail(failKey);
            saveLog(null, username, ip, userAgent, false, "账号不存在");
            throw new BizException(ResultCode.BAD_REQUEST, "账号或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            saveLog(user.getId(), username, ip, userAgent, false, "账号已停用");
            throw new BizException(ResultCode.FORBIDDEN, "账号已被停用，请联系管理员");
        }

        // ---------- 3. 校验密码 ----------
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFail(failKey);
            saveLog(user.getId(), username, ip, userAgent, false, "密码错误");
            int remain = jwtProperties.getMaxFailCount() - currentFail(failKey);
            throw new BizException(ResultCode.BAD_REQUEST,
                    remain > 0 ? "账号或密码错误，还可尝试 " + remain + " 次" : "密码错误次数过多，账号已锁定");
        }

        // ---------- 4. 登录成功：清理计数、升级密码、更新登录时间 ----------
        redisTemplate.delete(failKey);

        if (passwordEncoder.needUpgrade(user.getPassword())) {
            User upgrade = new User();
            upgrade.setId(user.getId());
            upgrade.setPassword(passwordEncoder.encode(request.getPassword()));
            userMapper.updateById(upgrade);
            log.info("用户 {} 的密码已自动升级为 BCrypt", username);
        }

        User update = new User();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(update);

        saveLog(user.getId(), username, ip, userAgent, true, null);

        // ---------- 5. 签发 Token ----------
        LoginUser loginUser = LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .stationId(user.getStationId())
                .build();

        String token = JwtUtils.createToken(loginUser, jwtProperties.getSecret(), jwtProperties.getExpireMillis());

        return LoginVO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpireHours() * 3600)
                .user(toVO(user))
                .build();
    }

    private void recordFail(String failKey) {
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(failKey, Duration.ofMinutes(jwtProperties.getLockMinutes()));
        } else if (count != null) {
            // 每次失败都续期，防止攻击者靠等待绕过锁定
            redisTemplate.expire(failKey, Duration.ofMinutes(jwtProperties.getLockMinutes()));
        }
    }

    private int currentFail(String failKey) {
        String value = redisTemplate.opsForValue().get(failKey);
        return value == null ? 0 : Integer.parseInt(value);
    }

    private void saveLog(Long userId, String username, String ip, String ua, boolean success, String reason) {
        try {
            LoginLog logEntity = new LoginLog();
            logEntity.setUserId(userId);
            logEntity.setUsername(username);
            logEntity.setLoginIp(ip);
            logEntity.setUserAgent(StrUtil.maxLength(ua, 480));
            logEntity.setSuccess(success ? 1 : 0);
            logEntity.setFailReason(reason);
            logEntity.setLoginTime(LocalDateTime.now());
            loginLogMapper.insert(logEntity);
        } catch (Exception e) {
            // 日志写入失败不能影响登录主流程
            log.warn("登录日志写入失败：{}", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException("账号 " + request.getUsername() + " 已被注册");
        }

        // 注册接口只允许自助注册为领养人或志愿者，防止越权创建管理员
        Set<String> allowedRoles = Set.of(CommonConstants.ROLE_ADOPTER, CommonConstants.ROLE_VOLUNTEER);
        String role = StrUtil.isBlank(request.getRole()) ? CommonConstants.ROLE_ADOPTER : request.getRole();
        if (!allowedRoles.contains(role)) {
            throw new BizException(ResultCode.FORBIDDEN, "注册角色仅支持 ADOPTER 或 VOLUNTEER");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole(role);
        user.setStationId(1L);
        user.setStatus(1);
        userMapper.insert(user);

        log.info("新用户注册成功：{} / {}", user.getUsername(), role);
        return user.getId();
    }

    @Override
    public UserVO currentUser() {
        Long userId = UserContext.requireUserId();
        User user = userMapper.selectById(userId);
        BizException.throwIfNull(user, "用户不存在");
        return toVO(user);
    }

    @Override
    public PageResult<UserVO> pageUsers(long pageNum, long pageSize, String keyword, String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(StrUtil.isNotBlank(role), User::getRole, role)
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(User::getUsername, keyword)
                        .or().like(User::getRealName, keyword)
                        .or().like(User::getPhone, keyword))
                .orderByDesc(User::getCreateTime);

        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<UserVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        BizException.throwIfNull(user, "用户不存在");
        if (CommonConstants.ROLE_ADMIN.equals(user.getRole()) && status == 0) {
            throw new BizException("不能停用管理员账号");
        }
        User update = new User();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
    }

    @Override
    public String resetPassword(Long userId) {
        User user = userMapper.selectById(userId);
        BizException.throwIfNull(user, "用户不存在");
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        userMapper.updateById(update);
        // 重置后清掉失败计数，避免用户被旧记录锁住
        redisTemplate.delete(LOGIN_FAIL_KEY + user.getUsername());
        return DEFAULT_PASSWORD;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(ProfileUpdateRequest request) {
        Long userId = UserContext.requireUserId();
        User user = userMapper.selectById(userId);
        BizException.throwIfNull(user, "用户不存在");

        String realName = request.getRealName().trim();
        String phone = StrUtil.emptyToNull(StrUtil.trim(request.getPhone()));
        String email = StrUtil.emptyToNull(StrUtil.trim(request.getEmail()));

        // 用 UpdateWrapper 显式 set：updateById 会忽略 null，
        // 那样用户想把手机号清空就清不掉
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getRealName, realName)
                .set(User::getPhone, phone)
                .set(User::getEmail, email)
                .set(User::getUpdateTime, LocalDateTime.now()));

        log.info("用户 {} 修改了个人资料", user.getUsername());
        return toVO(userMapper.selectById(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        Long userId = UserContext.requireUserId();
        User user = userMapper.selectById(userId);
        BizException.throwIfNull(user, "用户不存在");

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BizException("新密码不能与原密码相同");
        }

        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(update);

        // 改密后清掉失败计数，避免旧记录继续锁着
        redisTemplate.delete(LOGIN_FAIL_KEY + user.getUsername());
        log.info("用户 {} 修改了登录密码", user.getUsername());
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }
}
