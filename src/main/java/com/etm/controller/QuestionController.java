package com.etm.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.etm.common.PageResult;
import com.etm.common.Result;
import com.etm.entity.Question;
import com.etm.service.QuestionService;
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

@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @GetMapping("/page")
    public Result<PageResult<Question>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) Long courseId,
                                             @RequestParam(required = false) String type) {
        return Result.ok(questionService.page(current, size, courseId, type));
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @PostMapping
    public Result<Void> create(@RequestBody Question question) {
        questionService.create(question);
        return Result.ok();
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @PutMapping
    public Result<Void> update(@RequestBody Question question) {
        questionService.update(question);
        return Result.ok();
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.ok();
    }
}
