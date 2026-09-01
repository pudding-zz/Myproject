package com.myproject.website.modules.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.myproject.website.common.BusinessException;
import com.myproject.website.common.ErrorCode;
import com.myproject.website.config.DeepSeekProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 由 {@link com.myproject.website.config.AiClientConfig} 按需创建，勿再加 {@code @Component}，
 * 避免与 Mock 双 Bean 冲突。
 */
@Slf4j
@RequiredArgsConstructor
public class DeepSeekAiClient implements AiClient {

    private final DeepSeekProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String chat(List<AiMessage> messages) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException(ErrorCode.AI_ERROR, "DEEPSEEK_API_KEY is not configured");
        }

        List<Map<String, String>> payloadMessages = messages.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", payloadMessages);
        body.put("stream", false);

        try {
            JsonNode response = restClientBuilder.build()
                    .post()
                    .uri(properties.getBaseUrl() + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.AI_ERROR, "empty response from DeepSeek");
            }

            JsonNode content = response.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || !StringUtils.hasText(content.asText())) {
                log.warn("Unexpected DeepSeek response: {}", response);
                throw new BusinessException(ErrorCode.AI_ERROR, "invalid response from DeepSeek");
            }
            return content.asText();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("DeepSeek call failed", ex);
            throw new BusinessException(ErrorCode.AI_ERROR, "DeepSeek call failed: " + ex.getMessage());
        }
    }
}
