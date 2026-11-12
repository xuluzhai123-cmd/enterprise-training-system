package com.etm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
/*为什么用 @ConfigurationProperties 而不是一堆 @Value？
配置有 8 个字段了，@Value 要写 8 遍、拼错没提示；
AiProperties 有类型安全 + IDE 自动补全，还能被 @Validated 校验。
这是“会用 Spring”和“入门 Spring”的分水岭之一。*/
public class AiProperties {
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private int timeoutSeconds = 30;   // 单次请求超时
    private int maxRetries = 2;        // 失败重试次数
    private double temperature = 0.3;
    private int maxTokens = 800;
    private boolean mockWhenNoKey = true;  // 没配 key 时返回假数据，保证能演示
}