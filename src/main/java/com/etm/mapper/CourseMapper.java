package com.etm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etm.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
