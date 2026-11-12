package com.etm.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etm.common.BizException;
import com.etm.dto.RagAskRequest;
import com.etm.entity.Course;
import com.etm.entity.CourseChapter;
import com.etm.entity.KnowledgeChunk;
import com.etm.mapper.CourseChapterMapper;
import com.etm.mapper.CourseMapper;
import com.etm.mapper.KnowledgeChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量 RAG（检索增强生成）：课程问答。
 *
 * <p>链路：章节正文 → 切片 → 建库；提问 → 分词 → BM25 打分取前 3 块
 * → 拼进 prompt → 让模型只依据这几块回答 → 返回答案 + 引用来源。</p>
 *
 * <p>为什么不用向量库：DeepSeek 没有 embeddings 接口，本地算向量要再引入一个
 * provider；课设数据量只有几十块切片，BM25 的效果足够，且零成本、可解释
 * （能说清"为什么是这块"）。检索层是独立的，将来换向量只改 {@link #search}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    /** 切片目标长度（字） */
    private static final int CHUNK_SIZE = 260;
    /** 相邻切片重叠长度，避免一句话被切断后两边都不完整 */
    private static final int CHUNK_OVERLAP = 40;
    /** 取前几块 */
    private static final int TOP_K = 3;
    /** BM25 调参：k1 控制词频饱和、b 控制长度归一化强度 */
    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private static final String SYSTEM_PROMPT = """
            你是一门企业培训课程的助教。回答必须遵守：
            1. 只依据【资料】回答；资料里没有的内容，直接说「课程资料里没有提到这一点」，不要编造；
            2. 用中文，200 字以内，条理清楚；
            3. 引用了哪一段，就在句末标注对应编号，如 [1]；
            4. 不要用「根据资料」这种开头，直接给结论。
            """;

    private final KnowledgeChunkMapper chunkMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseMapper courseMapper;
    private final AiClient aiClient;

    /**
     * 为某门课构建知识库：把每个章节的正文切片后入库。
     * 幂等：先删掉这门课已有的切片再重建，改完章节重新点一次即可。
     */
    @Transactional
    public Map<String, Object> ingest(Long courseId) {
        if (courseId == null) {
            throw new BizException(400, "请选择课程");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(400, "课程不存在");
        }

        chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getCourseId, courseId));

        List<CourseChapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, courseId)
                .orderByAsc(CourseChapter::getSort)
                .orderByAsc(CourseChapter::getId));

        int seq = 0;
        int chapterUsed = 0;
        for (CourseChapter chapter : chapters) {
            String text = chapter.getContent() == null ? "" : chapter.getContent().trim();
            if (text.length() < 20) {
                // 正文太短的章节跳过：切出来也是噪音
                continue;
            }
            chapterUsed++;
            for (String piece : split(text)) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setCourseId(courseId);
                chunk.setChapterId(chapter.getId());
                chunk.setTitle(chapter.getTitle());
                chunk.setContent(piece);
                chunk.setSeq(seq++);
                chunkMapper.insert(chunk);
            }
        }

        log.info("知识库构建完成：course={}，章节={}/{}，切片={}", course.getTitle(), chapterUsed, chapters.size(), seq);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseId", courseId);
        result.put("courseTitle", course.getTitle());
        result.put("chapterCount", chapters.size());
        result.put("chapterUsed", chapterUsed);
        result.put("chunkCount", seq);
        return result;
    }

    /** 课程问答 */
    public Map<String, Object> ask(RagAskRequest req) {
        if (req == null || req.getCourseId() == null) {
            throw new BizException(400, "请选择课程");
        }
        String question = req.getQuestion() == null ? "" : req.getQuestion().trim();
        if (question.length() < 2) {
            throw new BizException(400, "问题太短了，至少写两个字");
        }

        List<KnowledgeChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getCourseId, req.getCourseId()));
        if (chunks.isEmpty()) {
            throw new BizException(4001, "这门课还没有知识库，请先点「构建知识库」");
        }

        List<Hit> hits = search(chunks, question);

        Map<String, Object> result = new LinkedHashMap<>();
        if (hits.isEmpty()) {
            // 检索不到就不调模型：既省钱，也避免模型硬编
            result.put("answer", "课程资料里没有检索到和这个问题相关的内容，换个说法再试试。");
            result.put("sources", new ArrayList<>());
            result.put("tokens", 0);
            result.put("costMs", 0);
            result.put("model", "none");
            return result;
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            KnowledgeChunk c = hits.get(i).chunk;
            context.append("【资料").append(i + 1).append("】").append(c.getTitle()).append('\n')
                    .append(brief(c.getContent(), 220)).append("\n\n");
        }

        String user = "【资料】\n" + context + "【问题】\n" + question;
        AiResult ai = aiClient.chat(SYSTEM_PROMPT, user);
        if (ai.mock()) {
            throw new BizException(4002, "未配置 AI 密钥（AI_API_KEY），无法回答");
        }

        List<Map<String, Object>> sources = new ArrayList<>();
        for (int i = 0; i < hits.size(); i++) {
            Hit hit = hits.get(i);
            Map<String, Object> src = new LinkedHashMap<>();
            src.put("no", i + 1);
            src.put("title", hit.chunk.getTitle());
            src.put("chapterId", hit.chunk.getChapterId());
            src.put("snippet", brief(hit.chunk.getContent(), 90));
            src.put("score", Math.round(hit.score * 100) / 100.0);
            sources.add(src);
        }

        result.put("answer", ai.content().trim());
        result.put("sources", sources);
        result.put("tokens", ai.totalTokens());
        result.put("costMs", ai.costMs());
        result.put("model", ai.model());
        return result;
    }

    /** 知识库状态，前端用来显示"已有 N 块" */
    public Map<String, Object> status(Long courseId) {
        Long count = chunkMapper.selectCount(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getCourseId, courseId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseId", courseId);
        result.put("chunkCount", count == null ? 0 : count);
        return result;
    }

    // ------------------------------------------------------------------ 切片

    /** 按句号切句，再把句子拼到接近 CHUNK_SIZE 的一块，相邻块重叠一小段 */
    private List<String> split(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (char c : text.toCharArray()) {
            buf.append(c);
            if (c == '。' || c == '！' || c == '？' || c == '；' || c == '\n') {
                String s = buf.toString().trim();
                if (!s.isEmpty()) {
                    sentences.add(s);
                }
                buf.setLength(0);
            }
        }
        if (buf.toString().trim().length() > 0) {
            sentences.add(buf.toString().trim());
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String s : sentences) {
            if (cur.length() > 0 && cur.length() + s.length() > CHUNK_SIZE) {
                String prev = cur.toString();
                chunks.add(prev);
                cur.setLength(0);
                int from = Math.max(0, prev.length() - CHUNK_OVERLAP);
                cur.append(prev.substring(from));
            }
            cur.append(s);
        }
        if (cur.length() > 0) {
            chunks.add(cur.toString());
        }
        return chunks;
    }

    // ------------------------------------------------------------- BM25 检索

    private record Hit(KnowledgeChunk chunk, double score) {
    }

    private List<Hit> search(List<KnowledgeChunk> chunks, String question) {
        List<String> query = tokens(question);
        if (query.isEmpty()) {
            return new ArrayList<>();
        }

        int n = chunks.size();
        double avgLen = chunks.stream().mapToInt(c -> c.getContent().length()).average().orElse(1);

        List<Map<String, Integer>> termFreqs = new ArrayList<>();
        Map<String, Integer> docFreq = new HashMap<>();
        for (KnowledgeChunk chunk : chunks) {
            Map<String, Integer> tf = new HashMap<>();
            for (String token : tokens(chunk.getContent())) {
                tf.merge(token, 1, Integer::sum);
            }
            termFreqs.add(tf);
            for (String token : tf.keySet()) {
                docFreq.merge(token, 1, Integer::sum);
            }
        }

        Set<String> unique = new HashSet<>(query);
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Map<String, Integer> tf = termFreqs.get(i);
            double len = chunks.get(i).getContent().length();
            double score = 0;
            for (String token : unique) {
                int f = tf.getOrDefault(token, 0);
                if (f == 0) {
                    continue;
                }
                int df = docFreq.getOrDefault(token, 0);
                double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));
                score += idf * (f * (K1 + 1)) / (f + K1 * (1 - B + B * len / avgLen));
            }
            if (score > 0) {
                hits.add(new Hit(chunks.get(i), score));
            }
        }
        hits.sort(Comparator.comparingDouble((Hit h) -> h.score).reversed());
        return hits.size() > TOP_K ? new ArrayList<>(hits.subList(0, TOP_K)) : hits;
    }

    /**
     * 极简分词：英文/数字按整块，中文按二元组（"分布式事务" → 分布/布式/式事/事务）。
     * 不引分词库是因为没有词表也能检索，二元组对中文足够稳。
     */
    private List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        Matcher matcher = Pattern.compile("[A-Za-z0-9_]+|[\\u4e00-\\u9fa5]+").matcher(text);
        while (matcher.find()) {
            String group = matcher.group();
            if (group.charAt(0) < 128) {
                out.add(group.toLowerCase());
            }
            else if (group.length() == 1) {
                out.add(group);
            }
            else {
                for (int i = 0; i + 1 < group.length(); i++) {
                    out.add(group.substring(i, i + 2));
                }
            }
        }
        return out;
    }

    private String brief(String text, int max) {
        if (text == null) {
            return "";
        }
        String s = text.replaceAll("\\s+", " ").trim();
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
