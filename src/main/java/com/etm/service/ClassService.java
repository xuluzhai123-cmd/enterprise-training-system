package com.etm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etm.common.BizException;
import com.etm.common.CurrentUser;
import com.etm.common.PageResult;
import com.etm.entity.Course;
import com.etm.entity.Enroll;
import com.etm.entity.SysUser;
import com.etm.entity.TrainClass;
import com.etm.mapper.CourseMapper;
import com.etm.mapper.EnrollMapper;
import com.etm.mapper.TrainClassMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassService {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_FINISHED = 2;

    private final TrainClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final EnrollMapper enrollMapper;

    public PageResult<TrainClass> page(long current, long size, String keyword, Integer status) {
        LambdaQueryWrapper<TrainClass> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(TrainClass::getName, keyword);
        }
        if (status != null) {
            wrapper.eq(TrainClass::getStatus, status);
        }
        wrapper.orderByDesc(TrainClass::getId);

        Page<TrainClass> page = classMapper.selectPage(new Page<>(current, size), wrapper);
        fillCourseName(page.getRecords());
        fillApplied(page.getRecords());
        return PageResult.of(page);
    }

    public List<TrainClass> options() {
        return classMapper.selectList(new LambdaQueryWrapper<TrainClass>()
                .orderByDesc(TrainClass::getId));
    }

    public TrainClass detail(Long id) {
        TrainClass clazz = classMapper.selectById(id);
        if (clazz == null) {
            throw new BizException("培训班不存在");
        }
        fillCourseName(List.of(clazz));
        return clazz;
    }

    @Transactional
    public void create(TrainClass clazz) {
        clazz.setId(null);
        if (StrUtil.isBlank(clazz.getName())) {
            throw new BizException("培训班名称不能为空");
        }
        if (clazz.getCourseId() == null) {
            throw new BizException("请选择培训课程");
        }
        if (clazz.getQuota() == null || clazz.getQuota() <= 0) {
            throw new BizException("名额必须大于 0");
        }
        clazz.setEnrolledCount(0);
        clazz.setVersion(0);
        if (clazz.getStatus() == null) {
            clazz.setStatus(STATUS_DRAFT);
        }
        classMapper.insert(clazz);
    }

    @Transactional
    public void update(TrainClass clazz) {
        if (clazz.getId() == null || classMapper.selectById(clazz.getId()) == null) {
            throw new BizException("培训班不存在");
        }
        // 名额不能改到比已报名人数还少
        TrainClass old = classMapper.selectById(clazz.getId());
        if (clazz.getQuota() != null && clazz.getQuota() < old.getEnrolledCount()) {
            throw new BizException("名额不能小于已通过人数 " + old.getEnrolledCount());
        }
        classMapper.updateById(clazz);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        TrainClass clazz = new TrainClass();
        clazz.setId(id);
        clazz.setStatus(status);
        classMapper.updateById(clazz);
    }

    @Transactional
    public void delete(Long id) {
        Long count = enrollMapper.selectCount(new LambdaQueryWrapper<Enroll>()
                .eq(Enroll::getClassId, id));
        if (count != null && count > 0) {
            throw new BizException("该培训班已有报名记录，不能删除");
        }
        classMapper.deleteById(id);
    }

    private void fillCourseName(List<TrainClass> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> courseIds = list.stream().map(TrainClass::getCourseId).collect(Collectors.toSet());
        Map<Long, Course> courseMap = courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a));
        for (TrainClass clazz : list) {
            Course course = courseMap.get(clazz.getCourseId());
            if (course != null) {
                clazz.setCourseName(course.getTitle());
            }
        }
    }

    /** 学员视角：标记是否已报名 */
    private void fillApplied(List<TrainClass> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        SysUser user = CurrentUser.get();
        if (user == null || !SysUser.ROLE_STUDENT.equals(user.getRole())) {
            return;
        }
        Set<Long> classIds = list.stream().map(TrainClass::getId).collect(Collectors.toSet());
        Set<Long> applied = enrollMapper.selectList(new LambdaQueryWrapper<Enroll>()
                        .eq(Enroll::getUserId, user.getId())
                        .in(Enroll::getClassId, classIds)
                        .ne(Enroll::getStatus, 3))
                .stream().map(Enroll::getClassId).collect(Collectors.toSet());
        for (TrainClass clazz : list) {
            boolean full = clazz.getEnrolledCount() != null && clazz.getQuota() != null
                    && clazz.getEnrolledCount() >= clazz.getQuota();
            clazz.setCanApply(STATUS_PUBLISHED == clazz.getStatus() && !full && !applied.contains(clazz.getId()));
        }
    }
}
