package com.etm.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etm.common.BizException;
import com.etm.common.CurrentUser;
import com.etm.common.PageResult;
import com.etm.entity.SysDept;
import com.etm.entity.SysUser;
import com.etm.mapper.SysDeptMapper;
import com.etm.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    public static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    public PageResult<SysUser> page(long current, long size, String keyword, Long deptId, String role, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (deptId != null) {
            wrapper.eq(SysUser::getDeptId, deptId);
        }
        if (StrUtil.isNotBlank(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        wrapper.orderByAsc(SysUser::getId);
        wrapper.eq(status != null, SysUser::getStatus, status);
        Page<SysUser> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        fillDeptName(page.getRecords());
        return PageResult.of(page);
    }

    @Transactional
    public void create(SysUser user) {
        user.setId(null);
        if (StrUtil.isBlank(user.getUsername())) {
            throw new BizException("登录账号不能为空");
        }
        if (StrUtil.isBlank(user.getRealName())) {
            throw new BizException("姓名不能为空");
        }
        if (user.getDeptId() == null) {
            throw new BizException("请选择所属部门");
        }
        if (exists(user.getUsername(), null)) {
            throw new BizException("登录账号已存在");
        }
        String raw = StrUtil.isBlank(user.getPassword()) ? DEFAULT_PASSWORD : user.getPassword();
        user.setPassword(BCrypt.hashpw(raw));
        if (StrUtil.isBlank(user.getRole())) {
            user.setRole(SysUser.ROLE_STUDENT);
        }
        if (user.getStatus() == null) {
            user.setStatus(0);
        }
        userMapper.insert(user);
    }

    @Transactional
    public void update(SysUser user) {
        if (user.getId() == null) {
            throw new BizException("用户 id 不能为空");
        }
        SysUser old = userMapper.selectById(user.getId());
        if (old == null) {
            throw new BizException("用户不存在");
        }
        if (StrUtil.isNotBlank(user.getUsername()) && exists(user.getUsername(), user.getId())) {
            throw new BizException("登录账号已存在");
        }
        // 密码不在这里改，走重置接口
        user.setPassword(null);
        userMapper.updateById(user);
    }

    @Transactional
    public void delete(Long id) {
        if (id.equals(CurrentUser.id())) {
            throw new BizException("不能删除当前登录账号");
        }
        userMapper.deleteById(id);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (id.equals(CurrentUser.id())) {
            throw new BizException("不能禁用当前登录账号");
        }
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Transactional
    public void resetPassword(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(BCrypt.hashpw(DEFAULT_PASSWORD));
        userMapper.updateById(user);
    }

    /**
     * 每个部门有多少人。
     * 不写 XML：selectMaps + groupBy 让 MyBatis-Plus 直接拼 SQL，
     * 查出来的 Map key 就是 select 里的别名（deptId / cnt）。
     */
    public List<Map<String, Object>> countByDept() {
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        qw.select("dept_id AS deptId", "COUNT(*) AS cnt")
                .groupBy("dept_id")
                .orderByAsc("dept_id");
        List<Map<String, Object>> rows = userMapper.selectMaps(qw);

        // 上一步只拿到 dept_id，部门名在这里补齐：一次查全部部门，避免逐行再查（N+1）
        Map<Long, String> deptNames = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName, (a, b) -> a));
        for (Map<String, Object> row : rows) {
            Object deptId = row.get("deptId");
            if (deptId == null) {
                row.put("deptName", "未分配部门");
            } else {
                long id = ((Number) deptId).longValue();
                row.put("deptId", id);
                row.put("deptName", deptNames.getOrDefault(id, "已删除部门"));
            }
            // MySQL 的 COUNT(*) 返回的是 Long，统一转 int，前端拿来直接显示
            row.put("cnt", ((Number) row.get("cnt")).intValue());
        }
        return rows;
    }

    /** 学员下拉（报名、成绩筛选用） */
    public List<SysUser> options() {
        List<SysUser> list = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, SysUser.ROLE_STUDENT)
                .orderByAsc(SysUser::getId));
        fillDeptName(list);
        return list;
    }

    private boolean exists(String username, Long excludeId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(SysUser::getId, excludeId);
        }
        Long count = userMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private void fillDeptName(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Map<Long, SysDept> deptMap = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, Function.identity(), (a, b) -> a));
        for (SysUser user : users) {
            SysDept dept = deptMap.get(user.getDeptId());
            if (dept != null) {
                user.setDeptName(dept.getName());
            }
        }
    }
}
