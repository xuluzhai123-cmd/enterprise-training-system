package com.etm.ai;

import com.etm.common.BizException;
import com.etm.dto.AiQuestionRequest;
import com.etm.dto.AiSaveRequest;
import com.etm.entity.Course;
import com.etm.entity.Question;
import com.etm.mapper.CourseMapper;
import com.etm.mapper.QuestionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 自动出题：给课程生成题目 → 老师预览勾选 → 批量入库。
 *
 * <p>核心难点不是调用模型，而是「<b>模型不完全可靠</b>」：
 * 它可能返回 markdown 代码块、可能多给字段、可能答案字母不在选项里。
 * 所以这里的流程是：让模型输出 JSON → 去掉包裹 → 逐题校验 → 不合格的丢掉并计数。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuestionService {

    private final AiClient aiClient;
    private final CourseMapper courseMapper;
    private final QuestionMapper questionMapper;
    private final ObjectMapper mapper;

    /** system 提示词：只约束"你是谁、输出什么格式"，不写业务细节 */
    private static final String SYSTEM_PROMPT = """
            你是一名企业培训命题专家，为内部培训课程出题。
            你只能输出 JSON 数组，禁止输出任何解释、前后缀或 markdown 代码块。
            """;

    // ==================== 生成 ====================

    public Map<String, Object> generate(AiQuestionRequest req) {
        if (req.getCourseId() == null) {
            throw new BizException(400, "请先选择课程");
        }
        Course course = courseMapper.selectById(req.getCourseId());
        if (course == null) {
            throw new BizException(400, "课程不存在");
        }

        int count = clamp(req.getCount() == null ? 5 : req.getCount(), 1, 10);
        String userPrompt = buildUserPrompt(course, count, req.getDifficulty(), req.getType());

        AiResult result = aiClient.chat(SYSTEM_PROMPT, userPrompt);
        if (result.mock()) {
            throw new BizException(4002, "未配置 AI 密钥（AI_API_KEY），无法生成题目");
        }

        List<AiQuestionItem> items = new ArrayList<>();
        int dropped = 0;
        for (JsonNode node : readArray(result.content())) {
            AiQuestionItem item = toItem(node);
            if (item == null) {
                dropped++;
            } else {
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            throw new BizException(4005, "模型没有返回有效题目，请重试或换个课程");
        }

        log.info("AI 出题完成 courseId={} 生成={} 丢弃={} tokens={}",
                course.getId(), items.size(), dropped, result.totalTokens());

        Map<String, Object> data = new HashMap<>();
        data.put("questions", items);
        data.put("dropped", dropped);
        data.put("tokens", result.totalTokens());
        data.put("costMs", result.costMs());
        data.put("model", result.model());
        return data;
    }

    /** 拼 user 提示词：把"课程名 + 数量 + 难度 + 题型 + 输出格式"说清楚 */
    private String buildUserPrompt(Course course, int count, String difficulty, String type) {
        String typeDesc = switch (type == null ? "MIXED" : type.toUpperCase()) {
            case "SINGLE" -> "全部是单选题";
            case "MULTI" -> "全部是多选题";
            default -> "单选题和多选题混合，多选至少 2 道";
        };
        return """
                课程名称：%s
                课程简介：%s
                请生成 %d 道%s的练习题，%s。

                严格按下面的 JSON 数组输出，不要输出任何其他内容：
                [
                  {
                    "type": "SINGLE",
                    "stem": "题干（不超过 60 字）",
                    "options": [
                      {"k": "A", "v": "选项内容"},
                      {"k": "B", "v": "选项内容"},
                      {"k": "C", "v": "选项内容"},
                      {"k": "D", "v": "选项内容"}
                    ],
                    "answer": "A",
                    "score": 5,
                    "explain": "为什么选这个答案（不超过 50 字）"
                  }
                ]

                硬性要求：
                1. 每题必须 4 个选项，字母从 A 开始连续排列；
                2. 多选题的 type 写 "MULTI"，answer 用逗号分隔，例如 "A,C"；
                3. answer 里的字母必须都在 options 里出现过；
                4. 不要出现"以上都对""以上都不对"这种选项；
                5. 只输出 JSON 数组本身。
                """.formatted(
                course.getTitle(),
                course.getIntro() == null ? "无" : brief(course.getIntro(), 120),
                count,
                difficulty == null ? "中等" : difficulty,
                typeDesc
        );
    }

    // ==================== 解析与校验 ====================

    /** 把模型返回的正文变成 JSON 数组节点；容忍 ```json 包裹和前后废话 */
    private JsonNode readArray(String content) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```")) {
            int first = text.indexOf('\n');
            int last = text.lastIndexOf("```");
            if (first > 0 && last > first) {
                text = text.substring(first + 1, last).trim();
            }
        }
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw new BizException(4005, "模型返回的内容不是 JSON 数组：" + brief(text, 120));
        }
        try {
            return mapper.readTree(text.substring(start, end + 1));
        } catch (Exception e) {
            throw new BizException(4005, "模型返回的 JSON 解析失败：" + brief(text, 120));
        }
    }

    /** 单个题目的校验与转换，不合格返回 null（由调用方计数） */
    private AiQuestionItem toItem(JsonNode node) {
        String stem = node.path("stem").asText("").trim();
        if (stem.isEmpty()) {
            return null;
        }
        JsonNode optsNode = node.path("options");
        if (!optsNode.isArray() || optsNode.size() < 2) {
            return null;
        }
        List<AiQuestionItem.Opt> options = new ArrayList<>();
        for (int i = 0; i < optsNode.size() && i < 6; i++) {
            String v = optsNode.get(i).path("v").asText("").trim();
            if (v.isEmpty()) {
                return null;
            }
            options.add(new AiQuestionItem.Opt(String.valueOf((char) ('A' + i)), v));
        }

        // 答案归一化：大写、去空格、排序，确保 "c,a" 和 "A,C" 等价
        String rawAnswer = node.path("answer").asText("").toUpperCase().replace(" ", "");
        List<String> letters = new ArrayList<>();
        for (String s : rawAnswer.split(",")) {
            if (!s.isBlank()) {
                letters.add(s.trim());
            }
        }
        letters.sort(String::compareTo);
        boolean allValid = !letters.isEmpty() && letters.stream()
                .allMatch(l -> options.stream().anyMatch(o -> o.k().equals(l)));
        if (!allValid) {
            return null;
        }
        String answer = String.join(",", letters);
        String type = "MULTI".equalsIgnoreCase(node.path("type").asText("SINGLE"))
                || letters.size() > 1 ? "MULTI" : "SINGLE";
        if (letters.size() > 1) {
            type = "MULTI";
        }
        int score = clamp(node.path("score").asInt(5), 1, 20);

        return new AiQuestionItem(type, brief(stem, 200), options, answer, score,
                brief(node.path("explain").asText(""), 100));
    }

    // ==================== 入库 ====================

    @Transactional
    public int save(AiSaveRequest req) {
        if (req.getCourseId() == null) {
            throw new BizException(400, "缺少课程");
        }
        if (courseMapper.selectById(req.getCourseId()) == null) {
            throw new BizException(400, "课程不存在");
        }
        if (req.getQuestions() == null || req.getQuestions().isEmpty()) {
            throw new BizException(400, "请至少选择一道题目");
        }

        int saved = 0;
        for (AiQuestionItem item : req.getQuestions()) {
            Question q = new Question();
            q.setCourseId(req.getCourseId());
            q.setType(item.type() == null ? "SINGLE" : item.type());
            q.setStem(item.stem());
            q.setOptions(writeOptions(item.options()));
            q.setAnswer(item.answer());
            q.setScore(item.score() == null ? 5 : item.score());
            questionMapper.insert(q);
            saved++;
        }
        log.info("AI 题目入库 courseId={} 条数={}", req.getCourseId(), saved);
        return saved;
    }

    /** options 从"对象数组"变回数据库里那串 JSON，和手工新增题目的格式保持一致 */
    private String writeOptions(List<AiQuestionItem.Opt> options) {
        try {
            return mapper.writeValueAsString(options == null ? List.of() : options);
        } catch (Exception e) {
            throw new BizException(4005, "选项序列化失败");
        }
    }

    // ==================== 小工具 ====================

    private String brief(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace("\r", " ").replace("\n", " ").trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
