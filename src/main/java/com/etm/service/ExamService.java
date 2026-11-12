package com.etm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etm.common.BizException;
import com.etm.common.CurrentUser;
import com.etm.common.PageResult;
import com.etm.dto.ExamSubmitDTO;
import com.etm.entity.Course;
import com.etm.entity.Enroll;
import com.etm.entity.ExamRecord;
import com.etm.entity.Question;
import com.etm.entity.SysUser;
import com.etm.entity.TrainClass;
import com.etm.mapper.CourseMapper;
import com.etm.mapper.EnrollMapper;
import com.etm.mapper.ExamRecordMapper;
import com.etm.mapper.QuestionMapper;
import com.etm.mapper.SysUserMapper;
import com.etm.mapper.TrainClassMapper;
import com.etm.vo.ExamResultVO;
import com.etm.vo.ExamStartVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    /** 一次考试的题目数量 */
    public static final int QUESTION_COUNT = 10;

    /** 考试时长（分钟） */
    public static final int DURATION_MINUTES = 20;

    /** 及格线百分比 */
    public static final int PASS_PERCENT = 60;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExamRecordMapper examRecordMapper;
    private final QuestionMapper questionMapper;
    private final TrainClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final EnrollMapper enrollMapper;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    /** 开始考试：校验报名 → 随机抽题 → 抹掉答案 */
    public ExamStartVO start(Long classId) {
        Enroll enroll = enrollMapper.selectOne(new LambdaQueryWrapper<Enroll>()
                .eq(Enroll::getClassId, classId)
                .eq(Enroll::getUserId, CurrentUser.id()));
        if (enroll == null || enroll.getStatus() != EnrollService.STATUS_APPROVED) {
            throw new BizException("请先报名该培训班并通过审批，才能参加考试");
        }
        TrainClass clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BizException("培训班不存在");
        }
        Course course = courseMapper.selectById(clazz.getCourseId());

        List<Question> pool = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, clazz.getCourseId()));
        if (pool.isEmpty()) {
            throw new BizException("该课程还没有配置题目，请联系管理员");
        }
        Collections.shuffle(pool);
        List<Question> picked = new ArrayList<>(pool.subList(0, Math.min(QUESTION_COUNT, pool.size())));
        picked.sort((a, b) -> Long.compare(a.getId(), b.getId()));

        List<Question> safe = picked.stream().map(q -> {
            Question copy = new Question();
            copy.setId(q.getId());
            copy.setCourseId(q.getCourseId());
            copy.setType(q.getType());
            copy.setStem(q.getStem());
            copy.setOptions(q.getOptions());
            copy.setScore(q.getScore());
            return copy;
        }).collect(Collectors.toList());

        ExamStartVO vo = new ExamStartVO();
        vo.setClassId(classId);
        vo.setClassName(clazz.getName());
        vo.setCourseTitle(course == null ? null : course.getTitle());
        vo.setDuration(DURATION_MINUTES);
        vo.setTotalScore(safe.stream().mapToInt(Question::getScore).sum());
        vo.setQuestions(safe);
        return vo;
    }

    /** 交卷判分 */
    @Transactional
    public ExamResultVO submit(ExamSubmitDTO dto) {
        if (dto.getClassId() == null) {
            throw new BizException("缺少培训班参数");
        }
        if (dto.getAnswers() == null || dto.getAnswers().isEmpty()) {
            throw new BizException("还没有作答，不能交卷");
        }
        List<Long> ids = new ArrayList<>();
        for (String key : dto.getAnswers().keySet()) {
            try {
                ids.add(Long.valueOf(key));
            } catch (NumberFormatException ignored) {
                // 非法题号直接跳过
            }
        }
        if (ids.isEmpty()) {
            throw new BizException("作答数据不合法");
        }
        List<Question> questions = questionMapper.selectBatchIds(ids);

        int score = 0;
        int total = 0;
        int correct = 0;
        for (Question question : questions) {
            total += question.getScore() == null ? 0 : question.getScore();
            String given = dto.getAnswers().get(String.valueOf(question.getId()));
            if (match(question.getAnswer(), given)) {
                score += question.getScore() == null ? 0 : question.getScore();
                correct++;
            }
        }
        int passed = total > 0 && score * 100 >= total * PASS_PERCENT ? 1 : 0;

        ExamRecord record = new ExamRecord();
        record.setClassId(dto.getClassId());
        record.setUserId(CurrentUser.id());
        record.setScore(score);
        record.setPassed(passed);
        record.setStartTime(parseTime(dto.getStartTime()));
        record.setSubmitTime(LocalDateTime.now());
        try {
            record.setAnswerJson(objectMapper.writeValueAsString(dto.getAnswers()));
        } catch (JsonProcessingException e) {
            record.setAnswerJson("{}");
        }
        examRecordMapper.insert(record);

        // 成绩回写到报名记录，多次考试取最高分
        Enroll enroll = enrollMapper.selectOne(new LambdaQueryWrapper<Enroll>()
                .eq(Enroll::getClassId, dto.getClassId())
                .eq(Enroll::getUserId, CurrentUser.id()));
        if (enroll != null) {
            if (enroll.getScore() == null || score > enroll.getScore()) {
                enroll.setScore(score);
                enroll.setPassed(passed);
                enrollMapper.updateById(enroll);
            }
        }

        ExamResultVO vo = new ExamResultVO();
        vo.setScore(score);
        vo.setTotalScore(total);
        vo.setCorrectCount(correct);
        vo.setTotalCount(questions.size());
        vo.setPassed(passed);
        return vo;
    }

    public PageResult<ExamRecord> records(long current, long size, Long classId, boolean mine) {
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        if (classId != null) {
            wrapper.eq(ExamRecord::getClassId, classId);
        }
        if (mine) {
            wrapper.eq(ExamRecord::getUserId, CurrentUser.id());
        }
        wrapper.orderByDesc(ExamRecord::getId);
        Page<ExamRecord> page = examRecordMapper.selectPage(new Page<>(current, size), wrapper);
        fill(page.getRecords());
        return PageResult.of(page);
    }

    public void fill(List<ExamRecord> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> classIds = list.stream().map(ExamRecord::getClassId).collect(Collectors.toSet());
        Map<Long, TrainClass> classMap = classMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(TrainClass::getId, Function.identity(), (a, b) -> a));
        Set<Long> courseIds = classMap.values().stream()
                .map(TrainClass::getCourseId).collect(Collectors.toSet());
        Map<Long, Course> courseMap = courseIds.isEmpty() ? Map.of()
                : courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a));
        Set<Long> userIds = list.stream().map(ExamRecord::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));

        // 第几次考试：把这批记录涉及到的「同班同人」的历史记录一次性查出来，
        // 再按 id 统计"不大于本次"的条数，避免每行都查一次库。
        Map<String, List<Long>> history = new HashMap<>();
        List<ExamRecord> all = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .in(ExamRecord::getClassId, classIds)
                .in(ExamRecord::getUserId, userIds)
                .select(ExamRecord::getId, ExamRecord::getClassId, ExamRecord::getUserId));
        for (ExamRecord r : all) {
            history.computeIfAbsent(r.getClassId() + "-" + r.getUserId(), k -> new ArrayList<>())
                    .add(r.getId());
        }

        for (ExamRecord record : list) {
            TrainClass clazz = classMap.get(record.getClassId());
            if (clazz != null) {
                record.setClassName(clazz.getName());
                Course course = courseMap.get(clazz.getCourseId());
                if (course != null) {
                    record.setCourseTitle(course.getTitle());
                }
            }
            SysUser user = userMap.get(record.getUserId());
            if (user != null) {
                record.setRealName(user.getRealName());
            }
            List<Long> ids = history.get(record.getClassId() + "-" + record.getUserId());
            if (ids != null) {
                long no = ids.stream().filter(id -> id <= record.getId()).count();
                record.setAttemptNo((int) no);
                record.setAttemptCount(ids.size());
            }
        }
    }

    /** 答案比对：忽略大小写、空格，多选顺序无关 */
    private boolean match(String standard, String given) {
        if (standard == null || given == null) {
            return false;
        }
        return normalize(standard).equals(normalize(given));
    }

    private String normalize(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value, TIME_FORMAT);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
