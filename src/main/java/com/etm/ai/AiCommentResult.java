package com.etm.ai;

/**
 * 一次评语生成的结果。
 *
 * @param content 评语正文
 * @param cached  是否直接复用了数据库里已有的评语（没有调用模型）
 * @param tokens  本次消耗的 token 数
 * @param costMs  本次调用耗时（毫秒）
 */
public record AiCommentResult(String content, boolean cached, long tokens, long costMs) {
}
