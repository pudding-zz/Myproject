package com.myproject.website.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** 喂给模型的最近消息条数（user+assistant 合计） */
    private int historyMaxMessages = 24;
}
