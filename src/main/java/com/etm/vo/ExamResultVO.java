package com.etm.vo;

import lombok.Data;

@Data
public class ExamResultVO {

    private Integer score;

    private Integer totalScore;

    private Integer correctCount;

    private Integer totalCount;

    private Integer passed;
}
