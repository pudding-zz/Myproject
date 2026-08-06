package com.myproject.website.modules.story.controller;

import com.myproject.website.common.ApiResponse;
import com.myproject.website.config.StoryProperties;
import com.myproject.website.modules.story.dto.DivergenceResponse;
import com.myproject.website.modules.story.dto.FromTitleRequest;
import com.myproject.website.modules.story.dto.StoryBaseResponse;
import com.myproject.website.modules.story.dto.TheaterRoundRequest;
import com.myproject.website.modules.story.dto.TheaterRoundResponse;
import com.myproject.website.modules.story.dto.UpsertStoryBaseRequest;
import com.myproject.website.modules.story.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/story-bases")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final StoryProperties storyProperties;

    @GetMapping("/settings")
    public ApiResponse<Map<String, Object>> settings() {
        return ApiResponse.ok(Map.of(
                "outlineFromTitleEnabled", storyProperties.isOutlineFromTitleEnabled()));
    }

    @GetMapping
    public ApiResponse<List<StoryBaseResponse>> list() {
        return ApiResponse.ok(storyService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<StoryBaseResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(storyService.get(id));
    }

    @PostMapping("/from-title")
    public ApiResponse<StoryBaseResponse> fromTitle(@Valid @RequestBody FromTitleRequest request) {
        return ApiResponse.ok(storyService.createFromTitle(request));
    }

    @PostMapping
    public ApiResponse<StoryBaseResponse> create(@Valid @RequestBody UpsertStoryBaseRequest request) {
        return ApiResponse.ok(storyService.createFromPaste(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<StoryBaseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpsertStoryBaseRequest request) {
        return ApiResponse.ok(storyService.update(id, request));
    }

    @GetMapping("/{id}/divergences")
    public ApiResponse<List<DivergenceResponse>> divergences(@PathVariable Long id) {
        return ApiResponse.ok(storyService.listDivergences(id));
    }

    @PostMapping("/{id}/theater/round")
    public ApiResponse<TheaterRoundResponse> theater(
            @PathVariable Long id,
            @Valid @RequestBody TheaterRoundRequest request) {
        return ApiResponse.ok(storyService.theaterRound(id, request));
    }
}
