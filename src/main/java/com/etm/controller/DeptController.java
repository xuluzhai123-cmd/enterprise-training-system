package com.etm.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.etm.common.Result;
import com.etm.entity.SysDept;
import com.etm.service.DeptService;
import com.etm.vo.DeptTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    /** 部门树：所有登录用户可读（下拉框要用） */
    @GetMapping("/tree")
    public Result<List<DeptTreeVO>> tree() {
        return Result.ok(deptService.tree());
    }

    /** 扁平列表 */
    @GetMapping("/list")
    public Result<List<SysDept>> list() {
        return Result.ok(deptService.list());
    }

    @SaCheckRole("ADMIN")
    @PostMapping
    public Result<Void> create(@RequestBody SysDept dept) {
        deptService.create(dept);
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @PutMapping
    public Result<Void> update(@RequestBody SysDept dept) {
        deptService.update(dept);
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok();
    }
}
