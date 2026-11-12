package com.etm.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.etm.common.PageResult;
import com.etm.common.Result;
import com.etm.entity.Enroll;
import com.etm.service.EnrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnrollController {

    private final EnrollService enrollService;

    /** 审批列表（管理员 / 讲师） */
    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @GetMapping("/enroll/page")
    public Result<PageResult<Enroll>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Long classId,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(enrollService.page(current, size, classId, status));
    }

    /** 我的培训（学员） */
    @GetMapping("/enroll/my")
    public Result<List<Enroll>> mine() {
        return Result.ok(enrollService.mine());
    }

    /** 学员报名 */
    @SaCheckRole("STUDENT")
    @PostMapping("/enroll/{classId}")
    public Result<Void> apply(@PathVariable Long classId) {
        enrollService.apply(classId);
        return Result.ok();
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @PutMapping("/enroll/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        enrollService.approve(id);
        return Result.ok();
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @PutMapping("/enroll/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        enrollService.reject(id);
        return Result.ok();
    }

    @PutMapping("/enroll/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        enrollService.cancel(id);
        return Result.ok();
    }

    // ---------------- 学习 ----------------

    @GetMapping("/learn/{enrollId}")
    public Result<Map<String, Object>> learnDetail(@PathVariable Long enrollId) {
        return Result.ok(enrollService.learnDetail(enrollId));
    }

    @PutMapping("/learn/{enrollId}/progress")
    public Result<Void> updateProgress(@PathVariable Long enrollId, @RequestParam Integer progress) {
        enrollService.updateProgress(enrollId, progress);
        return Result.ok();
    }

    /** 勾选/取消章节完成，返回重算后的进度 */
    @PostMapping("/learn/{enrollId}/chapter/{chapterId}")
    public Result<Integer> toggleChapter(@PathVariable Long enrollId,
                                         @PathVariable Long chapterId,
                                         @RequestParam(defaultValue = "true") boolean done) {
        return Result.ok(enrollService.toggleChapter(enrollId, chapterId, done));
    }
}
