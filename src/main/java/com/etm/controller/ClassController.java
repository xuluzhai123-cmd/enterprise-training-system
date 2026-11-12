package com.etm.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.etm.common.PageResult;
import com.etm.common.Result;
import com.etm.entity.TrainClass;
import com.etm.service.ClassService;
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

@RestController
@RequestMapping("/api/class")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    /** 所有登录用户都能看培训班列表（学员要浏览报名） */
    @GetMapping("/page")
    public Result<PageResult<TrainClass>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(classService.page(current, size, keyword, status));
    }

    @GetMapping("/options")
    public Result<List<TrainClass>> options() {
        return Result.ok(classService.options());
    }

    @GetMapping("/{id}")
    public Result<TrainClass> detail(@PathVariable Long id) {
        return Result.ok(classService.detail(id));
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @PostMapping
    public Result<Void> create(@RequestBody TrainClass clazz) {
        classService.create(clazz);
        return Result.ok();
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @PutMapping
    public Result<Void> update(@RequestBody TrainClass clazz) {
        classService.update(clazz);
        return Result.ok();
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        classService.updateStatus(id, status);
        return Result.ok();
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        classService.delete(id);
        return Result.ok();
    }
}
