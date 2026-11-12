package com.etm.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashboardVO {

    /** 员工总数 */
    private Long userCount;

    /** 课程总数 */
    private Long courseCount;

    /** 已发布培训班数 */
    private Long classCount;

    /** 已通过报名数 */
    private Long enrollCount;

    /** 待审批报名数 */
    private Long pendingCount;

    /** 考试记录数 */
    private Long examCount;

    /** 考试通过率（百分比，四舍五入） */
    private Integer passRate;

    /** 课程分类分布 [{name,value}] */
    private List<Map<String, Object>> categoryDist;

    /** 培训班报名人数排行 [{name,value}] */
    private List<Map<String, Object>> classRank;
}
