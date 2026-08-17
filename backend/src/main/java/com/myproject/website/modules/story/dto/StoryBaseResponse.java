package com.myproject.website.modules.story.dto;

import com.myproject.website.modules.story.entity.CanonNodeEntity;
import com.myproject.website.modules.story.entity.StoryBaseEntity;
import com.myproject.website.modules.story.entity.WorldStateEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StoryBaseResponse {

    private Long id;
    private String title;
    private String author;
    private String background;
    private String status;
    private String disclaimer;
    private List<CanonNodeResponse> nodes;
    private WorldStateResponse world;

    public static StoryBaseResponse from(
            StoryBaseEntity base,
            List<CanonNodeEntity> nodes,
            WorldStateEntity world) {
        return StoryBaseResponse.builder()
                .id(base.getId())
                .title(base.getTitle())
                .author(base.getAuthor())
                .background(base.getBackground())
                .status(base.getStatus())
                .disclaimer(base.getDisclaimer())
                .nodes(nodes.stream().map(CanonNodeResponse::from).toList())
                .world(world == null ? null : WorldStateResponse.from(world))
                .build();
    }

    @Data
    @Builder
    public static class CanonNodeResponse {
        private Long id;
        private Integer seqNo;
        private String timeLabel;
        private String place;
        private String originalPlot;
        private String status;
        private String changedPlot;

        public static CanonNodeResponse from(CanonNodeEntity n) {
            return CanonNodeResponse.builder()
                    .id(n.getId())
                    .seqNo(n.getSeqNo())
                    .timeLabel(n.getTimeLabel())
                    .place(n.getPlace())
                    .originalPlot(n.getOriginalPlot())
                    .status(n.getStatus())
                    .changedPlot(n.getChangedPlot())
                    .build();
        }
    }

    @Data
    @Builder
    public static class WorldStateResponse {
        private String currentTime;
        private String currentPlace;
        private String presentCharacters;
        private String summary;

        public static WorldStateResponse from(WorldStateEntity w) {
            return WorldStateResponse.builder()
                    .currentTime(w.getCurrentTime())
                    .currentPlace(w.getCurrentPlace())
                    .presentCharacters(w.getPresentCharacters())
                    .summary(w.getSummary())
                    .build();
        }
    }
}
