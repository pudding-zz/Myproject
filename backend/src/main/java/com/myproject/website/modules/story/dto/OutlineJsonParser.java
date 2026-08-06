package com.myproject.website.modules.story.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myproject.website.common.BusinessException;
import com.myproject.website.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutlineJsonParser {

    private final ObjectMapper objectMapper;

    public ParsedOutline parse(String raw) {
        try {
            String json = extractJson(raw);
            JsonNode root = objectMapper.readTree(json);
            ParsedOutline outline = new ParsedOutline();
            outline.setTitle(text(root, "title"));
            outline.setAuthor(text(root, "author"));
            outline.setBackground(text(root, "background"));
            List<ParsedNode> nodes = new ArrayList<>();
            JsonNode arr = root.path("nodes");
            if (arr.isArray()) {
                int i = 1;
                for (JsonNode n : arr) {
                    ParsedNode node = new ParsedNode();
                    node.setSeqNo(n.path("seqNo").asInt(i));
                    node.setTimeLabel(text(n, "timeLabel"));
                    node.setPlace(text(n, "place"));
                    node.setOriginalPlot(text(n, "originalPlot"));
                    if (!StringUtils.hasText(node.getOriginalPlot())) {
                        continue;
                    }
                    nodes.add(node);
                    i++;
                }
            }
            if (nodes.isEmpty()) {
                throw new BusinessException(ErrorCode.AI_ERROR, "剧情底本草稿缺少原著节点");
            }
            outline.setNodes(nodes);
            return outline;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.AI_ERROR, "无法解析剧情底本草稿: " + ex.getMessage());
        }
    }

    private static String extractJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(ErrorCode.AI_ERROR, "empty outline");
        }
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    @lombok.Data
    public static class ParsedOutline {
        private String title;
        private String author;
        private String background;
        private List<ParsedNode> nodes;
    }

    @lombok.Data
    public static class ParsedNode {
        private Integer seqNo;
        private String timeLabel;
        private String place;
        private String originalPlot;
    }
}
