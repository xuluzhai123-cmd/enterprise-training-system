package com.etm.ai;

import com.etm.common.BizException;
import com.etm.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final AiProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    // HttpClient 线程安全，必须复用（内部有连接池）；每次 new 会耗尽端口
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // 累计统计（AtomicLong 保证并发安全）
    private final AtomicLong calls = new AtomicLong();
    private final AtomicLong fails = new AtomicLong();
    private final AtomicLong tokens = new AtomicLong();

    /** 不可重试：参数错、鉴权错，重试一百次也一样 */
    private static class FatalException extends RuntimeException {
        FatalException(String m) { super(m); }
    }

    /** 可重试：429 限流、5xx 服务端抖动、超时 */
    private static class RetryableException extends RuntimeException {
        RetryableException(String m) { super(m); }
    }

    public AiResult chat(String systemPrompt, String userPrompt) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            if (props.isMockWhenNoKey()) {
                return new AiResult("[mock] 未配置 AI_API_KEY。你问的是：" + userPrompt, 0, 0, 0, 0, props.getModel(), true);
            }
            throw new BizException(4002, "AI 密钥未配置");
        }

        int maxRetries = Math.max(0, props.getMaxRetries());
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return doCall(systemPrompt, userPrompt);
            } catch (FatalException e) {
                log.warn("AI 调用失败（不可重试）：{}", e.getMessage());
                throw new BizException(4003, e.getMessage());
            } catch (RetryableException e) {
                log.warn("AI 调用失败（第 {}/{} 次）：{}", attempt + 1, maxRetries + 1, e.getMessage());
                if (attempt == maxRetries) {
                    break;
                }
                sleep(500L << attempt);   // 500ms、1000ms 退避
            }
        }
        fails.incrementAndGet();
        throw new BizException(4004, "AI 服务繁忙，请稍后再试");
    }

    private AiResult doCall(String systemPrompt, String userPrompt) {
        long start = System.currentTimeMillis();
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", props.getModel());
            body.put("temperature", props.getTemperature());
            body.put("max_tokens", props.getMaxTokens());
            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();

            if (code == 429 || code >= 500) {
                throw new RetryableException("HTTP " + code);
            }
            if (code != 200) {
                // 401 鉴权错、400 参数错、402 余额不足 —— 重试没意义
                throw new FatalException("模型返回 " + code + "：" + brief(resp.body()));
            }

            JsonNode root = mapper.readTree(resp.body());
            JsonNode usage = root.path("usage");
            long promptTokens = usage.path("prompt_tokens").asLong();
            long completionTokens = usage.path("completion_tokens").asLong();
            long totalTokens = usage.path("total_tokens").asLong();
            String content = root.path("choices").path(0).path("message").path("content").asText();

            calls.incrementAndGet();
            tokens.addAndGet(totalTokens);

            long cost = System.currentTimeMillis() - start;
            log.info("AI 调用成功 model={} tokens={} cost={}ms", props.getModel(), totalTokens, cost);
            return new AiResult(content, promptTokens, completionTokens, totalTokens, cost, props.getModel(), false);

        } catch (FatalException | RetryableException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new RetryableException("请求超时");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RetryableException("线程被中断");
        } catch (Exception e) {
            throw new RetryableException(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private String brief(String s) {
        return s == null ? "" : (s.length() > 200 ? s.substring(0, 200) + "..." : s);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 给前端/监控用的统计 */
    public String stats() {
        return "calls=" + calls.get() + ", fails=" + fails.get() + ", tokens=" + tokens.get();
    }
}