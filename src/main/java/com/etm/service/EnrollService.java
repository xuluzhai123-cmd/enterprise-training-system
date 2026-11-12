package com.etm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etm.common.BizException;
import com.etm.common.CurrentUser;
import com.etm.common.PageResult;
import com.etm.entity.Course;
import com.etm.entity.CourseChapter;
import com.etm.entity.Enroll;
import com.etm.entity.LearnRecord;
import com.etm.entity.SysDept;
import com.etm.entity.SysUser;
import com.etm.entity.TrainClass;
import com.etm.mapper.CourseChapterMapper;
import com.etm.mapper.CourseMapper;
import com.etm.mapper.EnrollMapper;
import com.etm.mapper.LearnRecordMapper;
import com.etm.mapper.SysDeptMapper;
import com.etm.mapper.SysUserMapper;
import com.etm.mapper.TrainClassMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollService {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;
    public static final int STATUS_CANCELED = 3;

    private final EnrollMapper enrollMapper;
    private final TrainClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final LearnRecordMapper learnRecordMapper;

    public PageResult<Enroll> page(long current, long size, Long classId, Integer status) {
        LambdaQueryWrapper<Enroll> wrapper = new LambdaQueryWrapper<>();
        if (classId != null) {
            wrapper.eq(Enroll::getClassId, classId);
        }
        if (status != null) {
            wrapper.eq(Enroll::getStatus, status);
        }
        wrapper.orderByDesc(Enroll::getId);

        Page<Enroll> page = enrollMapper.selectPage(new Page<>(current, size), wrapper);
        fill(page.getRecords());
        return PageResult.of(page);
    }

    public List<Enroll> mine() {
        List<Enroll> list = enrollMapper.selectList(new LambdaQueryWrapper<Enroll>()
                .eq(Enroll::getUserId, CurrentUser.id())
                .orderByDesc(Enroll::getId));
        fill(list);
        return list;
    }

    /** 学习页数据：报名信息 + 课程章节 */
    public Map<String, Object> learnDetail(Long enrollId) {
        Enroll enroll = getOwned(enrollId);
        if (enroll.getStatus() != STATUS_APPROVED) {
            throw new BizException("报名尚未通过审批，暂时不能学习");
        }
        TrainClass clazz = classMapper.selectById(enroll.getClassId());
        List<CourseChapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, clazz.getCourseId())
                .orderByAsc(CourseChapter::getSort)
                .orderByAsc(CourseChapter::getId));
        Map<String, Object> result = new HashMap<>();
        result.put("enroll", enroll);
        result.put("className", clazz.getName());
        result.put("courseId", clazz.getCourseId());
        result.put("chapters", chapters);
        result.put("doneChapterIds", doneChapterIds(enrollId));
        return result;
    }

    /** 该报名已完成的章节 id 列表 */
    private List<Long> doneChapterIds(Long enrollId) {
        return learnRecordMapper.selectList(new LambdaQueryWrapper<LearnRecord>()
                        .eq(LearnRecord::getEnrollId, enrollId))
                .stream()
                .map(LearnRecord::getChapterId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void apply(Long classId) {
        Long userId = CurrentUser.id();
        TrainClass clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BizException("培训班不存在");
        }
        if (clazz.getStatus() != ClassService.STATUS_PUBLISHED) {
            throw new BizException("该培训班未开放报名");
        }
        if (clazz.getEnrolledCount() >= clazz.getQuota()) {
            throw new BizException("名额已满");
        }
        Enroll exist = enrollMapper.selectOne(new LambdaQueryWrapper<Enroll>()
                .eq(Enroll::getClassId, classId)
                .eq(Enroll::getUserId, userId));
        if (exist != null && exist.getStatus() != STATUS_CANCELED) {
            throw new BizException("你已报名该培训班，请勿重复报名");
        }
        if (exist != null) {
            exist.setStatus(STATUS_PENDING);
            exist.setProgress(0);
            exist.setScore(null);
            exist.setPassed(null);
            enrollMapper.updateById(exist);
            return;
        }
        Enroll enroll = new Enroll();
        enroll.setClassId(classId);
        enroll.setUserId(userId);
        enroll.setStatus(STATUS_PENDING);
        enroll.setProgress(0);
        enrollMapper.insert(enroll);
    }

    @Transactional
    public void approve(Long id) {
        Enroll enroll = enrollMapper.selectById(id);
        if (enroll == null) {
            throw new BizException("报名记录不存在");
        }
        if (enroll.getStatus() != STATUS_PENDING) {
            throw new BizException("该报名不是待审批状态");
        }
        TrainClass clazz = classMapper.selectById(enroll.getClassId());
        if (clazz.getEnrolledCount() >= clazz.getQuota()) {
            throw new BizException("名额已满，无法通过");
        }
        clazz.setEnrolledCount(clazz.getEnrolledCount() + 1);
        // @Version 乐观锁：并发审批时只会有一个成功
        if (classMapper.updateById(clazz) == 0) {
            throw new BizException("名额正在被别人修改，请刷新后重试");
        }
        enroll.setStatus(STATUS_APPROVED);
        enrollMapper.updateById(enroll);
    }

    @Transactional
    public void reject(Long id) {
        Enroll enroll = enrollMapper.selectById(id);
        if (enroll == null) {
            throw new BizException("报名记录不存在");
        }
        if (enroll.getStatus() != STATUS_PENDING) {
            throw new BizException("该报名不是待审批状态");
        }
        enroll.setStatus(STATUS_REJECTED);
        enrollMapper.updateById(enroll);
    }

    @Transactional
    public void cancel(Long id) {
        Enroll enroll = getOwned(id);
        if (enroll.getStatus() == STATUS_CANCELED) {
            throw new BizException("该报名已取消");
        }
        if (enroll.getStatus() == STATUS_APPROVED) {
            TrainClass clazz = classMapper.selectById(enroll.getClassId());
            if (clazz.getEnrolledCount() > 0) {
                clazz.setEnrolledCount(clazz.getEnrolledCount() - 1);
                classMapper.updateById(clazz);
            }
        }
        enroll.setStatus(STATUS_CANCELED);
        enrollMapper.updateById(enroll);
    }

    @Transactional
    public void updateProgress(Long id, Integer progress) {
        Enroll enroll = getOwned(id);
        if (enroll.getStatus() != STATUS_APPROVED) {
            throw new BizException("报名尚未通过审批");
        }
        int value = progress == null ? 0 : Math.max(0, Math.min(100, progress));
        enroll.setProgress(value);
        enrollMapper.updateById(enroll);
    }

    /**
     * 勾选 / 取消某个章节的学习记录，并重算学习进度
     * 进度由后端按「已完成章节数 / 总章节数」计算，不再信任前端传值
     */
    @Transactional
    public int toggleChapter(Long enrollId, Long chapterId, boolean done) {
        Enroll enroll = getOwned(enrollId);
        if (enroll.getStatus() != STATUS_APPROVED) {
            throw new BizException("报名尚未通过审批，暂时不能学习");
        }
        TrainClass clazz = classMapper.selectById(enroll.getClassId());
        CourseChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null || clazz == null || !chapter.getCourseId().equals(clazz.getCourseId())) {
            throw new BizException("该章节不属于当前培训班课程");
        }

        LambdaQueryWrapper<LearnRecord> wrapper = new LambdaQueryWrapper<LearnRecord>()
                .eq(LearnRecord::getEnrollId, enrollId)
                .eq(LearnRecord::getChapterId, chapterId);

        if (done) {
            // 已存在就不重复插入，防止连点产生重复记录
            if (learnRecordMapper.selectOne(wrapper) == null) {
                LearnRecord record = new LearnRecord();
                record.setEnrollId(enrollId);
                record.setChapterId(chapterId);
                record.setUserId(CurrentUser.id());
                learnRecordMapper.insert(record);
            }
        } else {
            learnRecordMapper.delete(wrapper);
        }

        long total = chapterMapper.selectCount(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, clazz.getCourseId()));
        long finished = learnRecordMapper.selectCount(new LambdaQueryWrapper<LearnRecord>()
                .eq(LearnRecord::getEnrollId, enrollId));
        int value = total == 0 ? 0 : (int) (finished * 100 / total);

        enroll.setProgress(value);
        enrollMapper.updateById(enroll);
        return value;
    }

    private Enroll getOwned(Long id) {
        Enroll enroll = enrollMapper.selectById(id);
        if (enroll == null) {
            throw new BizException("报名记录不存在");
        }
        if (!enroll.getUserId().equals(CurrentUser.id())) {
            throw new BizException(403, "只能操作自己的报名记录");
        }
        return enroll;
    }

    /** 批量补全培训班名 / 课程名 / 学员姓名 / 部门名 */
    public void fill(List<Enroll> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> classIds = list.stream().map(Enroll::getClassId).collect(Collectors.toSet());
        Map<Long, TrainClass> classMap = classMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(TrainClass::getId, Function.identity(), (a, b) -> a));

        Set<Long> courseIds = classMap.values().stream()
                .map(TrainClass::getCourseId).collect(Collectors.toSet());
        Map<Long, Course> courseMap = courseIds.isEmpty() ? Map.of()
                : courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a));

        Set<Long> userIds = list.stream().map(Enroll::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));

        Set<Long> deptIds = userMap.values().stream()
                .map(SysUser::getDeptId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysDept> deptMap = deptIds.isEmpty() ? Map.of()
                : deptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, Function.identity(), (a, b) -> a));

        for (Enroll enroll : list) {
            TrainClass clazz = classMap.get(enroll.getClassId());
            if (clazz != null) {
                enroll.setClassName(clazz.getName());
                enroll.setCourseId(clazz.getCourseId());
                Course course = courseMap.get(clazz.getCourseId());
                if (course != null) {
                    enroll.setCourseTitle(course.getTitle());
                }
            }
            SysUser user = userMap.get(enroll.getUserId());
            if (user != null) {
                enroll.setRealName(user.getRealName());
                SysDept dept = deptMap.get(user.getDeptId());
                if (dept != null) {
                    enroll.setDeptName(dept.getName());
                }
            }
        }
    }
}
