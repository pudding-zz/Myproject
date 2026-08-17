package com.myproject.website.modules.ai;

import java.util.List;

/**
 * Single place to talk to LLM providers. Swap implementations to change models.
 */
public interface AiClient {

    String chat(List<AiMessage> messages);
}
