package com.etm.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etm.common.BizException;
import com.etm.common.CurrentUser;
import com.etm.dto.LoginDTO;
import com.etm.entity.SysDept;
import com.etm.entity.SysUser;
import com.etm.mapper.SysDeptMapper;
import com.etm.mapper.SysUserMapper;
import com.etm.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));

        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BizException("账号已被禁用，请联系管理员");
        }

        StpUtil.login(user.getId());
        StpUtil.getSession().set("user", user);
        return buildVO(user);
    }

    public LoginVO current() {
        SysUser user = CurrentUser.get();
        if (user == null) {
            throw new BizException(401, "请先登录");
        }
        return buildVO(user);
    }

    public void logout() {
        StpUtil.logout();
    }

    private LoginVO buildVO(SysUser user) {
        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setDeptId(user.getDeptId());
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }
        return vo;
    }
}
