package com.myproject.website.modules.story.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpsertStoryBaseRequest {

    @NotBlank(message = "title is required")
    private String title;

    private String author;

    private String background;

    /**
     * If true, mark as CONFIRMED and init/refresh world state from first node.
     */
    private boolean confirm;

    @Valid
    @NotEmpty(message = "nodes required")
    private List<CanonNodeInput> nodes = new ArrayList<>();

    @Data
    public static class CanonNodeInput {
        private Long id;

        private Integer seqNo;

        private String timeLabel;

        private String place;

        @NotBlank(message = "originalPlot is required")
        private String originalPlot;

        private String status;
    }
}
