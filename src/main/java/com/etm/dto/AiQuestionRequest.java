package com.etm.dto;

import lombok.Data;

/**
 * AI 出题请求。
 * 前端在「题库管理 → AI 生成」弹窗里填的东西，最终都变成这个对象。
 */
@Data
public class AiQuestionRequest {

    /** 给哪门课程出题（决定题目内容的方向） */
    private Long courseId;

    /** 出几道，1~10，后端会再钳一次 */
    private Integer count = 5;

    /** 难度：简单 / 中等 / 困难 */
    private String difficulty = "中等";

    /** 题型：SINGLE / MULTI / MIXED（混合） */
    private String type = "MIXED";
}
