package com.nextrack.issue.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Comment body cannot be blank")
        @Size(max = 10000, message = "Comment body cannot exceed 10000 characters")
        String body
) {}
