package com.myproject.website.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "ok"),
    BAD_REQUEST(400, "bad request"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    INTERNAL_ERROR(500, "internal error"),
    AI_ERROR(502, "ai service error");

    private final int code;
    private final String message;
}
