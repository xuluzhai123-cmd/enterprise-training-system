package com.etm.vo;

import lombok.Data;

@Data
public class LoginVO {

    private String token;

    private Long userId;

    private String username;

    private String realName;

    private String role;

    private Long deptId;

    private String deptName;
}
