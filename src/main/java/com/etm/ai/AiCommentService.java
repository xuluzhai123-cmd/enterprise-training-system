package com.etm.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etm.common.BizException;
import com.etm.dto.AiCommentRequest;
import com.etm.entity.Course;
import com.etm.entity.CourseChapter;
import com.etm.entity.Enroll;
import com.etm.entity.ExamRecord;
import com.etm.entity.SysUser;
import com.etm.entity.TrainClass;
import com.etm.mapper.CourseChapterMapper;
import com.etm.mapper.CourseMapper;
import com.etm.mapper.EnrollMapper;
import com.etm.mapper.ExamRecordMapper;
import com.etm.mapper.SysUserMapper;
import com.etm.mapper.TrainClassMapper;
import com.etm.service.EnrollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 考试评语：针对「某一条考试记录」写评语，而不是给学员写整期总评。
 * <p>这样第一次考砸、第二次考好的学员，两条记录会得到两段不同的评语；
 * 评语存在 exam_record.ai_comment 上，一行对一行，不会互相覆盖。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCommentService {

    private static final String SYSTEM_PROMPT = """
            你是一名企业培训讲师，要为学员的一次考试写评语。
            要求：
            1. 只输出评语正文，80~140 字，一段话，不要分点、不要标题、不要加"评语："前缀；
            2. 只针对"这一次考试"的表现来写，不要写成整个培训期的总结；
            3. 先给出与本次成绩相符的判断，再给一条具体、可执行的建议；
            4. 本次通过：肯定发挥，并指出还能再提高的方向；本次未通过：直言问题所在，给出复习重点，语气鼓励但不敷衍；
            5. 考试次数怎么提：只有"一次就通过"的学员不要提次数；
               其余情况（补考过，或唯一一次考试没通过）都要在正文里写明"第 N 次考试"；
            6. 只能使用用户给出的数据，不要编造分数、章节或经历。
            """;

    private final AiClient aiClient;
    private final ExamRecordMapper examRecordMapper;
    private final EnrollMapper enrollMapper;
    private final TrainClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final SysUserMapper userMapper;

    /** 生成（或复用）某一条考试记录的评语 */
    public AiCommentResult generate(AiCommentRequest req) {
        ExamRecord record = resolve(req);
        boolean force = Boolean.TRUE.equals(req.getForce());
        if (!force && record.getAiComment() != null && !record.getAiComment().isBlank()) {
            return new AiCommentResult(record.getAiComment(), true, 0, 0);
        }

        Enroll enroll = enrollMapper.selectOne(new LambdaQueryWrapper<Enroll>()
                .eq(Enroll::getClassId, record.getClassId())
                .eq(Enroll::getUserId, record.getUserId())
                .last("limit 1"));
        if (enroll == null || enroll.getStatus() == null || enroll.getStatus() != EnrollService.STATUS_APPROVED) {
            throw new BizException(400, "只有报名通过的学员才能生成考试评语");
        }

        TrainClass clazz = classMapper.selectById(record.getClassId());
        Course course = clazz == null ? null : courseMapper.selectById(clazz.getCourseId());
        SysUser user = userMapper.selectById(record.getUserId());

        long totalChapter = clazz == null ? 0 : chapterMapper.selectCount(
                new LambdaQueryWrapper<CourseChapter>().eq(CourseChapter::getCourseId, clazz.getCourseId()));
        int progress = enroll.getProgress() == null ? 0 : enroll.getProgress();
        int doneChapter = totalChapter == 0 ? 0 : (int) Math.round(totalChapter * progress / 100.0);

        int attemptNo = attemptNo(record);
        int attemptCount = attemptCount(record);
        Integer prevScore = prevScore(record);
        // "一次通过"= 只考了一次并且过了；其余情况（补考过、或唯一一次没过）都要在评语里点明是第几次
        boolean onePass = attemptCount == 1 && record.getPassed() != null && record.getPassed() == 1;

        String prompt = buildPrompt(user, clazz, course, progress, doneChapter,
                (int) totalChapter, record, attemptNo, prevScore, onePass);
        AiResult result = aiClient.chat(SYSTEM_PROMPT, prompt);
        if (result.mock()) {
            throw new BizException(4002, "未配置 AI 密钥（AI_API_KEY），无法生成评语");
        }

        String content = clean(result.content());
        if (onePass && content != null) {
            // 一次通过的不标注次数：模型偶尔会自作聪明，这里兜一道底
            content = content.replaceAll("第\\s*1\\s*次考试", "本次考试")
                    .replaceAll("第一次考试", "本次考试")
                    .replaceAll("初次考试", "本次考试");
        }
        if (content == null || content.length() < 15) {
            throw new BizException(4005, "模型返回的评语太短，请重试");
        }
        if (content.length() > 300) {
            content = content.substring(0, 300) + "…";
        }

        ExamRecord patch = new ExamRecord();
        patch.setId(record.getId());
        patch.setAiComment(content);
        patch.setCommentTime(LocalDateTime.now());
        examRecordMapper.updateById(patch);

        log.info("AI 考试评语 recordId={} 学员={} 第{}次 {}分 tokens={} 耗时={}ms",
                record.getId(), user == null ? "-" : user.getRealName(), attemptNo,
                record.getScore(), result.totalTokens(), result.costMs());
        return new AiCommentResult(content, false, result.totalTokens(), result.costMs());
    }

    /** 给一个班所有还没有评语的考试记录批量生成（最多 limit 条） */
    public Map<String, Object> batch(Long classId, int limit) {
        if (classId == null) {
            throw new BizException(400, "请先选择培训班");
        }
        int cap = Math.max(1, Math.min(limit, 20));
        List<ExamRecord> list = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getClassId, classId)
                .isNull(ExamRecord::getAiComment)
                .orderByAsc(ExamRecord::getId)
                .last("limit " + cap));

        int generated = 0;
        int failed = 0;
        for (ExamRecord record : list) {
            try {
                AiCommentRequest req = new AiCommentRequest();
                req.setExamRecordId(record.getId());
                generate(req);
                generated++;
            } catch (BizException ex) {
                if (ex.getCode() == 4002) {
                    throw ex;
                }
                failed++;
                log.warn("批量评语失败 recordId={} 原因={}", record.getId(), ex.getMessage());
            } catch (Exception ex) {
                failed++;
                log.warn("批量评语异常 recordId={}", record.getId(), ex);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("generated", generated);
        data.put("failed", failed);
        data.put("total", list.size());
        return data;
    }

    /** 定位考试记录：优先按 id，其次按 classId + userId 取最近一次 */
    private ExamRecord resolve(AiCommentRequest req) {
        if (req == null) {
            throw new BizException(400, "请求不能为空");
        }
        if (req.getExamRecordId() != null) {
            ExamRecord record = examRecordMapper.selectById(req.getExamRecordId());
            if (record == null) {
                throw new BizException(400, "找不到这条考试记录");
            }
            return record;
        }
        if (req.getClassId() == null || req.getUserId() == null) {
            throw new BizException(400, "缺少 examRecordId，或 classId + userId");
        }
        ExamRecord record = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getClassId, req.getClassId())
                .eq(ExamRecord::getUserId, req.getUserId())
                .orderByDesc(ExamRecord::getId)
                .last("limit 1"));
        if (record == null) {
            throw new BizException(400, "该学员在这个班还没有考试记录");
        }
        return record;
    }

    /** 第几次考试：同班同人、id 不大于本次的考试记录条数 */
    private int attemptNo(ExamRecord record) {
        Long count = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getClassId, record.getClassId())
                .eq(ExamRecord::getUserId, record.getUserId())
                .le(ExamRecord::getId, record.getId()));
        return count == null ? 1 : count.intValue();
    }

    /** 这个学员在这个班一共考了几次 */
    private int attemptCount(ExamRecord record) {
        Long count = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getClassId, record.getClassId())
                .eq(ExamRecord::getUserId, record.getUserId()));
        return count == null ? 1 : count.intValue();
    }

    /** 上一次的成绩（没有则为 null），用于在评语里体现进步 */
    private Integer prevScore(ExamRecord record) {
        ExamRecord prev = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getClassId, record.getClassId())
                .eq(ExamRecord::getUserId, record.getUserId())
                .lt(ExamRecord::getId, record.getId())
                .orderByDesc(ExamRecord::getId)
                .last("limit 1"));
        return prev == null ? null : prev.getScore();
    }

    private String buildPrompt(SysUser user, TrainClass clazz, Course course,
                               int progress, int doneChapter, int totalChapter,
                               ExamRecord record, int attemptNo, Integer prevScore, boolean onePass) {
        StringBuilder sb = new StringBuilder();
        sb.append("学员姓名：").append(user == null ? "某学员" : user.getRealName()).append('\n');
        sb.append("培训班：").append(clazz == null ? "-" : clazz.getName()).append('\n');
        sb.append("课程：").append(course == null ? "-" : course.getTitle()).append('\n');
        sb.append("学习进度：").append(progress).append("%（已完成 ").append(doneChapter)
                .append('/').append(totalChapter).append(" 个章节）\n");
        int score = record.getScore() == null ? 0 : record.getScore();
        boolean passed = record.getPassed() != null && record.getPassed() == 1;
        sb.append("本次考试：").append(score).append(" 分（满分 100），结果：")
                .append(passed ? "通过" : "未通过").append('\n');
        if (!onePass) {
            sb.append("考试次数：第 ").append(attemptNo).append(" 次");
            if (prevScore != null) {
                sb.append("（上一次 ").append(prevScore).append(" 分）");
            }
            sb.append('\n');
        }
        sb.append("请针对这一次考试写一段评语。");
        return sb.toString();
    }

    /** 去掉模型常见的多余包裹：代码围栏、标题、前缀符号 */
    private String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        s = s.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "");
        s = s.replaceAll("^(评语|学习评语|考试评语|评语正文)\\s*[:：]\\s*", "");
        s = s.replaceAll("^[*#\\s]+", "");
        s = s.replace("\r\n", "\n").replaceAll("\n{2,}", "\n").trim();
        return s;
    }
}
