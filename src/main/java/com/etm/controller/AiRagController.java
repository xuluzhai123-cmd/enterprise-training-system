package com.etm.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.etm.ai.RagService;
import com.etm.common.Result;
import com.etm.dto.RagAskRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 课程问答（轻量 RAG）。
 */
@RestController
@RequestMapping("/api/ai/rag")
@RequiredArgsConstructor
public class AiRagController {

    private final RagService ragService;

    /** 构建知识库：把章节正文切片入库，管理员和讲师可操作 */
    @PostMapping("/ingest")
    @SaCheckRole(value = {"ADMIN", "TEACHER"}, mode = SaMode.OR)
    public Result<Map<String, Object>> ingest(@RequestParam Long courseId) {
        return Result.ok(ragService.ingest(courseId));
    }

    /** 提问：学员、讲师、管理员都可以问 */
    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@RequestBody RagAskRequest request) {
        return Result.ok(ragService.ask(request));
    }

    /** 知识库状态：前端显示"已有 N 块" */
    @GetMapping("/status")
    public Result<Map<String, Object>> status(@RequestParam Long courseId) {
        return Result.ok(ragService.status(courseId));
    }
}
