package com.nextrack.issue.api.dto.response;

import com.nextrack.issue.domain.entity.IssueStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BoardViewResponse(
        UUID projectId,
        String projectKey,
        Map<IssueStatus, List<BoardCardResponse>> columns,
        int totalIssues
) {}
