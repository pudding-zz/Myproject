package com.myproject.website.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "story")
public class StoryProperties {

    /**
     * When false, title-based outline extraction is disabled (for future public launch).
     */
    private boolean outlineFromTitleEnabled = true;
}
