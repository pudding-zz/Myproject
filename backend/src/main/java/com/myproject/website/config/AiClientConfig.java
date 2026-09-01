package com.myproject.website.config;

import com.myproject.website.modules.ai.AiClient;
import com.myproject.website.modules.ai.DeepSeekAiClient;
import com.myproject.website.modules.ai.MockAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class AiClientConfig {

    private final DeepSeekProperties deepSeekProperties;

    @Bean
    public AiClient aiClient(RestClient.Builder restClientBuilder) {
        if (StringUtils.hasText(deepSeekProperties.getApiKey())) {
            return new DeepSeekAiClient(deepSeekProperties, restClientBuilder);
        }
        // 未配置 Key 时，启用演示模式 Mock
        return new MockAiClient();
    }

    /**
     * 前端设置接口会用到，标识当前是哪种 AI 实现。
     */
    @Bean
    public String aiProvider() {
        return StringUtils.hasText(deepSeekProperties.getApiKey()) ? "deepseek" : "mock";
    }
}
