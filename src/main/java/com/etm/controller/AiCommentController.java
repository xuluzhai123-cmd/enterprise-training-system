package com.etm.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.etm.ai.AiCommentResult;
import com.etm.ai.AiCommentService;
import com.etm.common.Result;
import com.etm.dto.AiCommentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/comment")
@RequiredArgsConstructor
public class AiCommentController {

    private final AiCommentService aiCommentService;

    /** 给某一个学员生成学习评语（已存在则直接返回） */
    @PostMapping("/generate")
    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    public Result<AiCommentResult> generate(@RequestBody AiCommentRequest req) {
        return Result.ok(aiCommentService.generate(req));
    }

    /** 给一个班还没评语的学员批量生成，最多 20 人 */
    @PostMapping("/batch")
    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    public Result<Map<String, Object>> batch(@RequestParam Long classId,
                                             @RequestParam(defaultValue = "5") int limit) {
        return Result.ok(aiCommentService.batch(classId, limit));
    }
}
