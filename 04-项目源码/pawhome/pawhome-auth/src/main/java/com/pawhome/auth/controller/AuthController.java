package com.pawhome.auth.controller;

import com.pawhome.auth.dto.ChangePasswordRequest;
import com.pawhome.auth.dto.LoginRequest;
import com.pawhome.auth.dto.LoginVO;
import com.pawhome.auth.dto.ProfileUpdateRequest;
import com.pawhome.auth.dto.RegisterRequest;
import com.pawhome.auth.dto.UserVO;
import com.pawhome.auth.service.AuthService;
import com.pawhome.common.core.PageResult;
import com.pawhome.common.core.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口。
 * <p>
 * 网关转发规则：/api/auth/** 去掉 /api 前缀后打到本服务的 /auth/**。
 * 例如前端请求 POST http://localhost:8080/api/auth/login
 * 实际落到本服务的 POST /auth/login。
 */
@Tag(name = "认证授权", description = "登录、注册、用户管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录", description = "校验账号密码，成功后返回 JWT 令牌")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String ip = getClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        return R.ok("登录成功", authService.login(request, ip, userAgent));
    }

    @Operation(summary = "注册", description = "自助注册，角色仅支持 ADOPTER / VOLUNTEER")
    @PostMapping("/register")
    public R<Long> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok("注册成功", authService.register(request));
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public R<UserVO> me() {
        return R.ok(authService.currentUser());
    }

    @Operation(summary = "退出登录", description = "JWT 无状态，服务端仅作记录，客户端需自行清除 token")
    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok("已退出登录", null);
    }

    @Operation(summary = "分页查询用户")
    @GetMapping("/users")
    public R<PageResult<UserVO>> pageUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字：账号/姓名/手机号") @RequestParam(required = false) String keyword,
            @Parameter(description = "角色筛选") @RequestParam(required = false) String role) {
        return R.ok(authService.pageUsers(pageNum, pageSize, keyword, role));
    }

    @Operation(summary = "启用/停用账号")
    @PutMapping("/users/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        authService.updateStatus(id, status);
        return R.ok(status == 1 ? "已启用" : "已停用", null);
    }

    @Operation(summary = "修改个人资料", description = "本人修改姓名 / 手机号 / 邮箱，账号与角色不可改")
    @PutMapping("/profile")
    public R<UserVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return R.ok("资料已更新", authService.updateProfile(request));
    }

    @Operation(summary = "修改自己的密码", description = "必须先校验原密码")
    @PutMapping("/profile/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return R.ok("密码已修改", null);
    }

    @Operation(summary = "重置密码", description = "重置为默认密码 123456")
    @PostMapping("/users/{id}/reset-password")
    public R<String> resetPassword(@PathVariable Long id) {
        return R.ok("密码已重置", authService.resetPassword(id));
    }

    /** 取真实客户端 IP，兼容 Nginx 反向代理场景 */
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
