package com.etm.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.etm.common.PageResult;
import com.etm.common.Result;
import com.etm.entity.SysUser;
import com.etm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @SaCheckRole("ADMIN")
    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long deptId,
                                            @RequestParam(required = false) String role,
                                            @RequestParam(required = false) Integer status
                                            ) {
        return Result.ok(userService.page(current, size, keyword, deptId, role,status));
    }

    @SaCheckRole("ADMIN")
    @GetMapping("/options")
    public Result<List<SysUser>> options() {
        return Result.ok(userService.options());
    }

    /**
     * 每个部门有多少人：GET /api/user/count-by-dept
     * 返回 [{"deptId":1,"deptName":"总公司","cnt":1}, ...]
     * 只要求登录（看板类统计接口，讲师/学员也会看）
     */
    @GetMapping("/count-by-dept")
    public Result<List<Map<String, Object>>> countByDept() {
        return Result.ok(userService.countByDept());
    }

    @SaCheckRole("ADMIN")
    @PostMapping
    public Result<Void> create(@RequestBody SysUser user) {
        userService.create(user);
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) {
        userService.update(user);
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/{id}/reset")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.ok();
    }
}
