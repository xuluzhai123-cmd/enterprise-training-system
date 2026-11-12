package com.etm.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ExamSubmitDTO {

    private Long classId;

    /** 开始答题时间，格式 yyyy-MM-dd HH:mm:ss */
    private String startTime;

    /** 题号 -> 答案，多选用逗号分隔 */
    private Map<String, String> answers;
}
