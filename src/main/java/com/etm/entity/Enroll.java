package com.etm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("enroll")
public class Enroll {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;

    private Long userId;

    /** 0待审批 1已通过 2已驳回 3已取消 */
    private Integer status;

    /** 学习进度 0-100 */
    private Integer progress;

    private Integer score;

    /** 0否 1是 */
    private Integer passed;

    /** AI 生成的学习评语 */
    private String aiComment;

    /** 评语生成时间 */
    private LocalDateTime commentTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ---------- 以下为非数据库字段，列表展示用 ----------

    @TableField(exist = false)
    private String className;

    @TableField(exist = false)
    private String courseTitle;

    @TableField(exist = false)
    private String realName;

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private Long courseId;
}
