package com.etm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etm.common.BizException;
import com.etm.common.PageResult;
import com.etm.entity.Question;
import com.etm.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;

    public PageResult<Question> page(long current, long size, Long courseId, String type) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) {
            wrapper.eq(Question::getCourseId, courseId);
        }
        if (StrUtil.isNotBlank(type)) {
            wrapper.eq(Question::getType, type);
        }
        wrapper.orderByDesc(Question::getId);
        Page<Question> page = questionMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page);
    }

    public List<Question> listByCourse(Long courseId) {
        return questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .orderByAsc(Question::getId));
    }

    @Transactional
    public void create(Question question) {
        question.setId(null);
        validate(question);
        questionMapper.insert(question);
    }

    @Transactional
    public void update(Question question) {
        if (question.getId() == null || questionMapper.selectById(question.getId()) == null) {
            throw new BizException("题目不存在");
        }
        validate(question);
        questionMapper.updateById(question);
    }

    @Transactional
    public void delete(Long id) {
        questionMapper.deleteById(id);
    }

    private void validate(Question question) {
        if (question.getCourseId() == null) {
            throw new BizException("请选择所属课程");
        }
        if (StrUtil.isBlank(question.getStem())) {
            throw new BizException("题干不能为空");
        }
        if (StrUtil.isBlank(question.getOptions())) {
            throw new BizException("选项不能为空");
        }
        if (StrUtil.isBlank(question.getAnswer())) {
            throw new BizException("正确答案不能为空");
        }
        if (StrUtil.isBlank(question.getType())) {
            question.setType("SINGLE");
        }
        if (question.getScore() == null || question.getScore() <= 0) {
            question.setScore(10);
        }
    }
}
