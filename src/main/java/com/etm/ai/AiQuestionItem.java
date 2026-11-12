package com.etm.ai;

import java.util.List;

/**
 * 一道「AI 生成」的题目。
 *
 * <p>和数据库实体 Question 的区别：
 * <ul>
 *   <li>options 是结构化数组，不是数据库里那串 JSON 字符串，读写都方便</li>
 *   <li>多一个 explain（解析），用来在预览时告诉老师「为什么选这个」</li>
 * </ul>
 * 入库前才把 options 序列化成字符串、把 explain 丢掉。
 *
 * <p>这是 Java 17 的 record，Jackson 也能直接反序列化（2.15 起原生支持）。
 */
public record AiQuestionItem(
        /** SINGLE / MULTI */
        String type,
        /** 题干 */
        String stem,
        /** 选项，例如 [{k:"A", v:"..."}, ...] */
        List<Opt> options,
        /** 正确答案，"A" 或 "A,C" */
        String answer,
        /** 分值 */
        Integer score,
        /** 解析（不存库，只在预览里看） */
        String explain
) {

    /** 一个选项：k 是字母，v 是文字 */
    public record Opt(String k, String v) {
    }
}
