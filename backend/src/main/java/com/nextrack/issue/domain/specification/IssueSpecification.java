package com.nextrack.issue.domain.specification;

import com.nextrack.issue.domain.entity.Issue;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class IssueSpecification {

    private IssueSpecification() {}

    public static Specification<Issue> withFilters(UUID projectId, IssueFilterParams filters) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory Project Scoping
            predicates.add(builder.equal(root.get("projectId"), projectId));

            if (filters == null) {
                predicates.add(builder.isNull(root.get("archivedAt")));
                return builder.and(predicates.toArray(new Predicate[0]));
            }

            // 2. Archive Filter
            if (filters.includeArchived() == null || !filters.includeArchived()) {
                predicates.add(builder.isNull(root.get("archivedAt")));
            }

            // 3. Free-text search on Title or Description
            if (filters.search() != null && !filters.search().isBlank()) {
                String term = "%" + filters.search().trim().toLowerCase() + "%";
                Predicate titleMatch = builder.like(builder.lower(root.get("title")), term);
                Predicate descMatch = builder.like(builder.lower(root.get("description")), term);
                predicates.add(builder.or(titleMatch, descMatch));
            }

            // 4. Status Multi-Match
            if (filters.statuses() != null && !filters.statuses().isEmpty()) {
                predicates.add(root.get("status").in(filters.statuses()));
            }

            // 5. Priority Multi-Match
            if (filters.priorities() != null && !filters.priorities().isEmpty()) {
                predicates.add(root.get("priority").in(filters.priorities()));
            }

            // 6. Issue Type Multi-Match
            if (filters.types() != null && !filters.types().isEmpty()) {
                predicates.add(root.get("issueType").in(filters.types()));
            }

            // 7. Assignee Match
            if (Boolean.TRUE.equals(filters.isUnassigned())) {
                predicates.add(builder.isNull(root.get("assigneeId")));
            } else if (filters.assigneeId() != null) {
                predicates.add(builder.equal(root.get("assigneeId"), filters.assigneeId()));
            }

            // 8. Reporter Match
            if (filters.reporterId() != null) {
                predicates.add(builder.equal(root.get("reporterId"), filters.reporterId()));
            }

            // 9. Due Date Range
            if (filters.dueBefore() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("dueDate"), filters.dueBefore()));
            }
            if (filters.dueAfter() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("dueDate"), filters.dueAfter()));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
