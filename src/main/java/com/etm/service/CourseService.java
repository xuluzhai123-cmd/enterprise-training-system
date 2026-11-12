package com.etm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etm.common.BizException;
import com.etm.common.PageResult;
import com.etm.entity.Course;
import com.etm.entity.CourseChapter;
import com.etm.mapper.CourseChapterMapper;
import com.etm.mapper.CourseMapper;
import com.etm.vo.CourseDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;

    public PageResult<Course> page(long current, long size, String keyword, String category, Integer status) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Course::getTitle, keyword)
                    .or().like(Course::getTeacherName, keyword));
        }
        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(Course::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(Course::getStatus, status);
        }
        wrapper.orderByDesc(Course::getId);

        Page<Course> page = courseMapper.selectPage(new Page<>(current, size), wrapper);
        for (Course course : page.getRecords()) {
            course.setChapterCount(Math.toIntExact(chapterMapper.selectCount(
                    new LambdaQueryWrapper<CourseChapter>().eq(CourseChapter::getCourseId, course.getId()))));
        }
        return PageResult.of(page);
    }

    public List<Course> options() {
        return courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .orderByDesc(Course::getId));
    }

    public CourseDetailVO detail(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException("课程不存在");
        }
        CourseDetailVO vo = new CourseDetailVO();
        vo.setCourse(course);
        vo.setChapters(chapters(id));
        return vo;
    }

    public List<CourseChapter> chapters(Long courseId) {
        return chapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, courseId)
                .orderByAsc(CourseChapter::getSort)
                .orderByAsc(CourseChapter::getId));
    }

    @Transactional
    public void create(Course course) {
        course.setId(null);
        if (StrUtil.isBlank(course.getTitle())) {
            throw new BizException("课程名称不能为空");
        }
        if (course.getStatus() == null) {
            course.setStatus(0);
        }
        if (course.getHours() == null) {
            course.setHours(0);
        }
        courseMapper.insert(course);
    }

    @Transactional
    public void update(Course course) {
        if (course.getId() == null || courseMapper.selectById(course.getId()) == null) {
            throw new BizException("课程不存在");
        }
        courseMapper.updateById(course);
    }

    @Transactional
    public void delete(Long id) {
        courseMapper.deleteById(id);
        chapterMapper.delete(new LambdaQueryWrapper<CourseChapter>().eq(CourseChapter::getCourseId, id));
    }

    // ---------------- 章节 ----------------

    @Transactional
    public void saveChapter(CourseChapter chapter) {
        chapter.setId(null);
        if (chapter.getCourseId() == null) {
            throw new BizException("请先选择课程");
        }
        if (StrUtil.isBlank(chapter.getTitle())) {
            throw new BizException("章节标题不能为空");
        }
        if (chapter.getSort() == null) {
            Long max = chapterMapper.selectCount(new LambdaQueryWrapper<CourseChapter>()
                    .eq(CourseChapter::getCourseId, chapter.getCourseId()));
            chapter.setSort(max == null ? 1 : max.intValue() + 1);
        }
        if (StrUtil.isBlank(chapter.getType())) {
            chapter.setType("VIDEO");
        }
        if (chapter.getDuration() == null) {
            chapter.setDuration(0);
        }
        chapterMapper.insert(chapter);
    }

    @Transactional
    public void updateChapter(CourseChapter chapter) {
        if (chapter.getId() == null) {
            throw new BizException("章节 id 不能为空");
        }
        chapterMapper.updateById(chapter);
    }

    @Transactional
    public void deleteChapter(Long id) {
        chapterMapper.deleteById(id);
    }
}
