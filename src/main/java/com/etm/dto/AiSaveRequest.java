package com.etm.dto;

import com.etm.ai.AiQuestionItem;
import lombok.Data;

import java.util.List;

/**
 * 把「AI 生成的题目」存进题库的请求。
 *
 * <p>为什么不让「生成」直接入库、非要分两步？
 * 因为模型会出错（题干重复、答案不合法），必须让人先看一眼、勾选要哪些。
 * 这也是 AI 功能落地的通用原则：<b>AI 产出，人来把关</b>。
 */
@Data
public class AiSaveRequest {

    /** 存到哪门课下面 */
    private Long courseId;

    /** 用户勾选后要保存的题目 */
    private List<AiQuestionItem> questions;
}
