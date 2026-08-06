package com.myproject.website.modules.story.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TheaterRoundRequest {

    @NotEmpty(message = "characterIds required")
    @Size(min = 2, max = 6, message = "theater needs 2-6 characters")
    private List<Long> characterIds;

    /** Optional player interruption line */
    private String playerLine;

    /** Number of AI speaking turns in this round */
    private Integer turns = 3;
}
