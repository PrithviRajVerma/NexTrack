package com.nextrack.issue.domain.workflow;

import com.nextrack.issue.domain.entity.IssueStatus;
import com.nextrack.shared.exception.WorkflowTransitionDeniedException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Fixed Finite State Machine (FSM) enforcing issue lifecycle transitions (ADR-07).
 * Permitted transitions:
 * - BACKLOG <-> TO_DO
 * - TO_DO <-> IN_PROGRESS
 * - IN_PROGRESS <-> IN_REVIEW
 * - IN_REVIEW <-> DONE
 * - DONE -> IN_PROGRESS (defect reopen)
 * - BACKLOG -> ARCHIVED / DONE -> ARCHIVED
 */
public final class IssueStatusTransitionRule {

    private static final Map<IssueStatus, Set<IssueStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<IssueStatus, Set<IssueStatus>> map = new EnumMap<>(IssueStatus.class);

        map.put(IssueStatus.BACKLOG, Set.of(IssueStatus.TO_DO, IssueStatus.ARCHIVED));
        map.put(IssueStatus.TO_DO, Set.of(IssueStatus.BACKLOG, IssueStatus.IN_PROGRESS));
        map.put(IssueStatus.IN_PROGRESS, Set.of(IssueStatus.TO_DO, IssueStatus.IN_REVIEW));
        map.put(IssueStatus.IN_REVIEW, Set.of(IssueStatus.IN_PROGRESS, IssueStatus.DONE));
        map.put(IssueStatus.DONE, Set.of(IssueStatus.IN_REVIEW, IssueStatus.IN_PROGRESS, IssueStatus.ARCHIVED));
        map.put(IssueStatus.ARCHIVED, Collections.emptySet());

        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private IssueStatusTransitionRule() {
        // Utility class
    }

    public static boolean isTransitionAllowed(IssueStatus currentStatus, IssueStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        if (currentStatus == targetStatus) {
            return false;
        }
        Set<IssueStatus> permitted = ALLOWED_TRANSITIONS.get(currentStatus);
        return permitted != null && permitted.contains(targetStatus);
    }

    public static void validateTransition(IssueStatus currentStatus, IssueStatus targetStatus) {
        if (!isTransitionAllowed(currentStatus, targetStatus)) {
            throw new WorkflowTransitionDeniedException(
                    "Transition from " + currentStatus + " to " + targetStatus + " is not permitted by the workflow state machine"
            );
        }
    }

    public static Set<IssueStatus> getPermittedTransitions(IssueStatus currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
    }
}
