package com.etm.common;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.etm.entity.SysUser;

/**
 * 当前登录用户工具
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static SysUser get() {
        SaSession session = StpUtil.getSession(false);
        if (session == null) {
            return null;
        }
        return (SysUser) session.get("user");
    }

    public static Long id() {
        SysUser user = get();
        return user == null ? null : user.getId();
    }
}
