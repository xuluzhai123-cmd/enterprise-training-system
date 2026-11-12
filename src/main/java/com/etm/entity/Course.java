package com.etm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程名称 */
    private String title;

    /** 分类：新员工/技术/管理/合规 */
    private String category;

    /** 讲师姓名 */
    private String teacherName;

    /** 课时（小时） */
    private Integer hours;

    private String coverUrl;

    private String intro;

    /** 0下架 1上架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;

    /** 章节数量，非数据库字段 */
    @TableField(exist = false)
    private Integer chapterCount;
}
