package com.etm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工 / 学员（一个账号三种角色）
 */
@Data
@TableName("sys_user")
public class SysUser {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 只允许写入，永不返回给前端 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String realName;

    private Long deptId;

    /** ADMIN / TEACHER / STUDENT */
    private String role;

    private String phone;

    /** 0 正常 1 禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    /** 非数据库字段：部门名称 */
    @TableField(exist = false)
    private String deptName;
}
