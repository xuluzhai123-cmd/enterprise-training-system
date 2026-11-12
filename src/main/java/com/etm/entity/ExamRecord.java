package com.etm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_record")
public class ExamRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;

    private Long userId;

    private Integer score;

    /** 0 未通过 1 通过 */
    private Integer passed;

    private String answerJson;

    private LocalDateTime startTime;

    private LocalDateTime submitTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** AI 针对「这一次考试」生成的评语 */
    private String aiComment;

    private LocalDateTime commentTime;

    // ---------- 非数据库字段 ----------

    @TableField(exist = false)
    private String realName;

    @TableField(exist = false)
    private String className;

    @TableField(exist = false)
    private String courseTitle;

    /** 这是该学员在这个班的第几次考试（从 1 开始），列表展示用 */
    @TableField(exist = false)
    private Integer attemptNo;

    /** 该学员在这个班一共考了几次，用来判断要不要显示"第 N 次"标签 */
    @TableField(exist = false)
    private Integer attemptCount;
}
