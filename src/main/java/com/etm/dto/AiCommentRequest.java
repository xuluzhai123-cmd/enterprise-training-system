package com.etm.dto;

import lombok.Data;

/**
 * AI 考试评语请求。
 * <p>评语是按「某一次考试」生成的，所以优先用 examRecordId 定位；
 * 批量生成时用 classId，逐个考试记录补评语。</p>
 */
@Data
public class AiCommentRequest {

    /** 指定某一条考试记录（成绩列表里点某一行就是这个） */
    private Long examRecordId;

    /** 不传 examRecordId 时，用 classId + userId 取最近一次考试 */
    private Long classId;

    private Long userId;

    /** true = 重新生成（覆盖已有评语），默认 false 复用已有的 */
    private Boolean force;
}
