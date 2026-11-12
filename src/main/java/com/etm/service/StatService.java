package com.etm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etm.entity.Course;
import com.etm.entity.Enroll;
import com.etm.entity.ExamRecord;
import com.etm.entity.SysUser;
import com.etm.entity.TrainClass;
import com.etm.mapper.CourseMapper;
import com.etm.mapper.EnrollMapper;
import com.etm.mapper.ExamRecordMapper;
import com.etm.mapper.SysUserMapper;
import com.etm.mapper.TrainClassMapper;
import com.etm.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatService {

    private final SysUserMapper userMapper;
    private final CourseMapper courseMapper;
    private final TrainClassMapper classMapper;
    private final EnrollMapper enrollMapper;
    private final ExamRecordMapper examRecordMapper;

    public DashboardVO dashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setUserCount(userMapper.selectCount(null));
        vo.setCourseCount(courseMapper.selectCount(null));
        vo.setClassCount(classMapper.selectCount(new LambdaQueryWrapper<TrainClass>()
                .eq(TrainClass::getStatus, ClassService.STATUS_PUBLISHED)));
        vo.setEnrollCount(enrollMapper.selectCount(new LambdaQueryWrapper<Enroll>()
                .eq(Enroll::getStatus, EnrollService.STATUS_APPROVED)));
        vo.setPendingCount(enrollMapper.selectCount(new LambdaQueryWrapper<Enroll>()
                .eq(Enroll::getStatus, EnrollService.STATUS_PENDING)));

        List<ExamRecord> records = examRecordMapper.selectList(null);
        vo.setExamCount((long) records.size());
        long passed = records.stream().filter(r -> r.getPassed() != null && r.getPassed() == 1).count();
        vo.setPassRate(records.isEmpty() ? 0 : (int) Math.round(passed * 100.0 / records.size()));

        vo.setCategoryDist(categoryDist());
        vo.setClassRank(classRank());
        return vo;
    }

    /** 课程按分类分组统计 */
    private List<Map<String, Object>> categoryDist() {
        List<Course> courses = courseMapper.selectList(null);
        Map<String, Integer> counter = new LinkedHashMap<>();
        for (Course course : courses) {
            String key = (course.getCategory() == null || course.getCategory().isBlank())
                    ? "未分类" : course.getCategory();
            counter.merge(key, 1, Integer::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        counter.forEach((name, value) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("value", value);
            result.add(item);
        });
        return result;
    }

    /** 报名人数前 5 的培训班 */
    private List<Map<String, Object>> classRank() {
        List<TrainClass> classes = classMapper.selectList(new LambdaQueryWrapper<TrainClass>()
                .orderByDesc(TrainClass::getEnrolledCount)
                .last("limit 5"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (TrainClass clazz : classes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", clazz.getName());
            item.put("value", clazz.getEnrolledCount() == null ? 0 : clazz.getEnrolledCount());
            result.add(item);
        }
        return result;
    }
}
