package com.etm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("train_class")
public class TrainClass {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long courseId;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 名额上限 */
    private Integer quota;

    /** 已通过人数 */
    private Integer enrolledCount;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 0草稿 1已发布 2已结束 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;

    /** 课程名称，非数据库字段 */
    @TableField(exist = false)
    private String courseName;

    /** 是否可报名，非数据库字段 */
    @TableField(exist = false)
    private Boolean canApply;
}
