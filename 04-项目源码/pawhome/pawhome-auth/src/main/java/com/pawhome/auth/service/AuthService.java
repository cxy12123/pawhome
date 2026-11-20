package com.pawhome.auth.service;

import com.pawhome.auth.dto.ChangePasswordRequest;
import com.pawhome.auth.dto.LoginRequest;
import com.pawhome.auth.dto.LoginVO;
import com.pawhome.auth.dto.ProfileUpdateRequest;
import com.pawhome.auth.dto.RegisterRequest;
import com.pawhome.auth.dto.UserVO;
import com.pawhome.common.core.PageResult;

public interface AuthService {

    /** 登录，返回 JWT */
    LoginVO login(LoginRequest request, String ip, String userAgent);

    /** 注册新用户 */
    Long register(RegisterRequest request);

    /** 获取当前登录用户信息 */
    UserVO currentUser();

    /** 分页查询用户 */
    PageResult<UserVO> pageUsers(long pageNum, long pageSize, String keyword, String role);

    /** 启用/停用账号 */
    void updateStatus(Long userId, Integer status);

    /** 重置密码为默认值 */
    String resetPassword(Long userId);

    /** 修改当前登录用户的资料（姓名 / 手机 / 邮箱） */
    UserVO updateProfile(ProfileUpdateRequest request);

    /** 修改当前登录用户的密码，需校验原密码 */
    void changePassword(ChangePasswordRequest request);
}
