package com.nextrack.project.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(min = 2, max = 128, message = "Project name must be between 2 and 128 characters")
        String name,

        @NotBlank(message = "Project key is required")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9]{1,9}$", message = "Project key must start with a letter and contain up to 10 alphanumeric characters")
        String projectKey,

        String description
) {}
