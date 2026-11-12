package com.etm.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.etm.entity.SysUser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限数据源：角色直接从登录时写入 Session 的用户对象里取
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>();
        SaSession session = StpUtil.getSessionByLoginId(loginId, false);
        if (session == null) {
            return roles;
        }
        SysUser user = (SysUser) session.get("user");
        if (user != null && user.getRole() != null) {
            roles.add(user.getRole());
        }
        return roles;
    }
}
