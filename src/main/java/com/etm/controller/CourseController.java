package com.etm.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.etm.common.PageResult;
import com.etm.common.Result;
import com.etm.entity.Course;
import com.etm.entity.CourseChapter;
import com.etm.service.CourseService;
import com.etm.vo.CourseDetailVO;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/course/page")
    public Result<PageResult<Course>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(courseService.page(current, size, keyword, category, status));
    }

    @GetMapping("/course/options")
    public Result<List<Course>> options() {
        return Result.ok(courseService.options());
    }

    @GetMapping("/course/{id}")
    public Result<CourseDetailVO> detail(@PathVariable Long id) {
        return Result.ok(courseService.detail(id));
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @PostMapping("/course")
    public Result<Void> create(@RequestBody Course course) {
        courseService.create(course);
        return Result.ok();
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @PutMapping("/course")
    public Result<Void> update(@RequestBody Course course) {
        courseService.update(course);
        return Result.ok();
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @DeleteMapping("/course/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.ok();
    }

    // ---------------- 章节 ----------------

    @GetMapping("/chapter/list")
    public Result<List<CourseChapter>> chapters(@RequestParam Long courseId) {
        return Result.ok(courseService.chapters(courseId));
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @PostMapping("/chapter")
    public Result<Void> saveChapter(@RequestBody CourseChapter chapter) {
        courseService.saveChapter(chapter);
        return Result.ok();
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @PutMapping("/chapter")
    public Result<Void> updateChapter(@RequestBody CourseChapter chapter) {
        courseService.updateChapter(chapter);
        return Result.ok();
    }

    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    @DeleteMapping("/chapter/{id}")
    public Result<Void> deleteChapter(@PathVariable Long id) {
        courseService.deleteChapter(id);
        return Result.ok();
    }
}
