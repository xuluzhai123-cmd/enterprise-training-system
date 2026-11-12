package com.etm.vo;

import com.etm.entity.Course;
import com.etm.entity.CourseChapter;
import lombok.Data;

import java.util.List;

@Data
public class CourseDetailVO {

    private Course course;

    private List<CourseChapter> chapters;
}
