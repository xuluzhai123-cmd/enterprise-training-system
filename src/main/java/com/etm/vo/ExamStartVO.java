package com.etm.vo;

import com.etm.entity.Question;
import lombok.Data;

import java.util.List;

@Data
public class ExamStartVO {

    private Long classId;

    private String className;

    private String courseTitle;

    /** 考试时长（分钟） */
    private Integer duration;

    private Integer totalScore;

    /** 题目列表，已去掉正确答案 */
    private List<Question> questions;
}
