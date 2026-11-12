package com.etm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 章节学习记录：一个报名对同一章节只允许一条记录
 */
@Data
@TableName("learn_record")
public class LearnRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报名记录 id */
    private Long enrollId;

    /** 章节 id */
    private Long chapterId;

    /** 学员 id（冗余，方便按人统计） */
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
