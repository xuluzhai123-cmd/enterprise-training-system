package com.etm.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.etm.ai.AiQuestionService;
import com.etm.common.Result;
import com.etm.dto.AiQuestionRequest;
import com.etm.dto.AiSaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 出题的两个接口：先生成（给老师看），再保存（入题库）。
 *
 * <p>权限：只有管理员和讲师能碰题库，学员调会被 403 挡住。
 */
@RestController
@RequestMapping("/api/ai/question")
@RequiredArgsConstructor
public class AiQuestionController {

    private final AiQuestionService aiQuestionService;

    /** 生成题目（不落库，只返回给前端预览） */
    @PostMapping("/generate")
    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    public Result<Map<String, Object>> generate(@RequestBody AiQuestionRequest req) {
        return Result.ok(aiQuestionService.generate(req));
    }

    /** 把勾选的题目存进题库 */
    @PostMapping("/save")
    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    public Result<Integer> save(@RequestBody AiSaveRequest req) {
        return Result.ok(aiQuestionService.save(req));
    }
}
