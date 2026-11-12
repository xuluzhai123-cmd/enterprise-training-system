package com.etm.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点
 */
@Data
public class DeptTreeVO {

    private Long id;

    private String name;

    private Long parentId;

    private Integer sort;

    private Integer status;

    private List<DeptTreeVO> children = new ArrayList<>();
}
