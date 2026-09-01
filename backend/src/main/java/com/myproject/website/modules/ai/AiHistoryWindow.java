package com.myproject.website.modules.ai;

import java.util.Collections;
import java.util.List;

/**
 * 将历史消息截断为最近若干条，控制 token / 延迟。
 */
public final class AiHistoryWindow {

    private AiHistoryWindow() {
    }

    public static <T> List<T> recent(List<T> all, int maxItems) {
        if (all == null || all.isEmpty() || maxItems <= 0) {
            return List.of();
        }
        if (all.size() <= maxItems) {
            return all;
        }
        return all.subList(all.size() - maxItems, all.size());
    }

    /** 保证截断后若第一条是 assistant，尽量从更早的 user 对齐；此处仅做尾部窗口。 */
    public static <T> List<T> recentOrEmpty(List<T> all, int maxItems) {
        List<T> window = recent(all, maxItems);
        return window.isEmpty() ? List.of() : Collections.unmodifiableList(List.copyOf(window));
    }
}
