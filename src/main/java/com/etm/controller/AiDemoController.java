package com.etm.controller;

import com.etm.ai.AiClient;
import com.etm.ai.AiResult;
import com.etm.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * D1：最小的模型调用 Demo。
 * 目的不是做功能，而是把「HTTP + JSON」这条链路走通。
 */
@RestController
@RequestMapping("/api/ai")
public class AiDemoController {

    private final AiClient aiClient;

    public AiDemoController(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @GetMapping("/ping")
    public Result<String> ping(@RequestParam(defaultValue = "用一句话介绍你自己") String q) {
        AiResult r = aiClient.chat("你是一个简洁的助手，回答不超过 50 个字。", q);
        return Result.ok(r.content() + "  [tokens=" + r.totalTokens() + ", " + r.costMs() + "ms]");
    }

    @GetMapping("/stats")
    public Result<String> stats() {
        return Result.ok(aiClient.stats());
    }
}