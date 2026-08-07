package com.myproject.website.modules.character.controller;

import com.myproject.website.common.ApiResponse;
import com.myproject.website.modules.character.dto.CharacterResponse;
import com.myproject.website.modules.character.dto.CreateCharacterRequest;
import com.myproject.website.modules.character.dto.UpdateCharacterRequest;
import com.myproject.website.modules.character.service.CharacterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping
    public ApiResponse<List<CharacterResponse>> list(
            @RequestParam(required = false) Long storyBaseId) {
        return ApiResponse.ok(characterService.listEnabled(storyBaseId));
    }

    @GetMapping("/{id}")
    public ApiResponse<CharacterResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(characterService.getById(id));
    }

    @PostMapping
    public ApiResponse<CharacterResponse> create(@Valid @RequestBody CreateCharacterRequest request) {
        return ApiResponse.ok(characterService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CharacterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCharacterRequest request) {
        return ApiResponse.ok(characterService.update(id, request));
    }
}
