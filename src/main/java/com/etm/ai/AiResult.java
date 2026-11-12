package com.etm.ai;

/**
 * 一次模型调用的结果。
 * 用 record 而不是普通类：只读 DTO，自动生成构造器、getter、equals、toString。
 */
public record AiResult(
        String content,        // 模型回答
        long promptTokens,     // 输入消耗
        long completionTokens, // 输出消耗
        long totalTokens,      // 合计（计费依据）
        long costMs,           // 耗时
        String model,
        boolean mock           // 是否是假数据
) {
}