package com.etm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    /** SINGLE 单选 / MULTI 多选 */
    private String type;

    private String stem;

    /** JSON 数组字符串：[{"k":"A","v":"选项内容"}] */
    private String options;

    /** 正确答案，多选用逗号分隔 */
    private String answer;

    private Integer score;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
